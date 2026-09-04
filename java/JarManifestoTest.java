// `java.util.jar` de punta a punta, con el manifiesto en el centro.
//
// La prueba esta escrita para correr **igual** con nuestra VM y con el `java` real, y por eso no lee
// ningun archivo del repositorio: escribe lo que va a leer. Las dos implementaciones tienen que ver
// los mismos bytes y contestar lo mismo.
//
// Lo que mas se cuida es el plegado a 72 bytes, que es la unica regla del formato que se puede
// cumplir "casi bien" y producir archivos que otras herramientas no leen. Se comprueba con **bytes
// exactos** y no con propiedades: la linea que sale mide 72, la continuacion arranca con un espacio,
// y un caracter multibyte **si** se parte en el corte -- eso ultimo es lo que hace el JDK, se
// verifico contra el JDK 25, y es correcto porque el lector une bytes antes de decodificar.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarManifestoTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static final String RUTA = "_kaji_jartest.jar";

    static String repetir(String s, int veces) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < veces) {
            sb.append(s);
            i = i + 1;
        }
        return sb.toString();
    }

    static boolean iguales(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int i = 0;
        while (i < a.length) {
            if (a[i] != b[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    static byte[] escribir(Manifest m) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        m.write(bo);
        return bo.toByteArray();
    }

    // ---- el plegado a 72 bytes -------------------------------------------------------------------

    static void plegadoAscii() throws Exception {
        String valor = repetir("abcdefghij", 20);          // 200 bytes
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("Long-Ascii", valor);

        // "Long-Ascii: " son 12 bytes, asi que la primera linea se lleva 60 del valor; cada
        // continuacion se lleva 71 y el espacio la deja en 72.
        String esperado = "Manifest-Version: 1.0\r\n"
                + "Long-Ascii: " + valor.substring(0, 60) + "\r\n"
                + " " + valor.substring(60, 131) + "\r\n"
                + " " + valor.substring(131, 200) + "\r\n"
                + "\r\n";
        ok(iguales(escribir(m), esperado.getBytes(StandardCharsets.UTF_8)));

        Manifest r = new Manifest(new ByteArrayInputStream(escribir(m)));
        ok(valor.equals(r.getMainAttributes().getValue("Long-Ascii")));
        // Y el nombre no distingue mayusculas ni al leer ni al buscar.
        ok(valor.equals(r.getMainAttributes().getValue("LONG-ascii")));
    }

    static void plegadoUtf8() throws Exception {
        // 60 enies = 120 bytes. "Long-Utf8: " son 11, asi que la primera linea se lleva 61 bytes de
        // valor: 30 enies enteras **mas el primer byte de la 31**. El corte cae en el medio de un
        // caracter, y eso es lo correcto.
        String valor = repetir("\u00f1", 60);
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("Long-Utf8", valor);
        byte[] b = escribir(m);

        ok(largoDeLineaMaximo(b) == 72);
        ok(continuacionesEmpiezanConEspacio(b));
        // La primera linea del atributo termina en 0xc3 --media enie-- y la siguiente arranca con el
        // espacio de continuacion y el 0xb1 que la completa.
        int corte = finDeLinea(b, indiceDe(b, "Long-Utf8: "));
        ok((b[corte - 1] & 0xff) == 0xc3);
        ok((b[corte + 2] & 0xff) == 0x20);
        ok((b[corte + 3] & 0xff) == 0xb1);

        // Y a pesar de eso la vuelta es exacta: el lector une bytes, no textos.
        Manifest r = new Manifest(new ByteArrayInputStream(b));
        ok(valor.equals(r.getMainAttributes().getValue("Long-Utf8")));
    }

    static int indiceDe(byte[] b, String s) {
        byte[] a = s.getBytes(StandardCharsets.UTF_8);
        int i = 0;
        while (i + a.length <= b.length) {
            int j = 0;
            while (j < a.length && b[i + j] == a[j]) {
                j = j + 1;
            }
            if (j == a.length) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    // El indice del '\r' que cierra la linea que empieza en `desde`.
    static int finDeLinea(byte[] b, int desde) {
        int i = desde;
        while (i < b.length && b[i] != '\r') {
            i = i + 1;
        }
        return i;
    }

    static int largoDeLineaMaximo(byte[] b) {
        int max = 0;
        int inicio = 0;
        int i = 0;
        while (i < b.length) {
            if (b[i] == '\n') {
                int fin = i;
                if (fin > inicio && b[fin - 1] == '\r') {
                    fin = fin - 1;
                }
                if (fin - inicio > max) {
                    max = fin - inicio;
                }
                inicio = i + 1;
            }
            i = i + 1;
        }
        return max;
    }

    static boolean continuacionesEmpiezanConEspacio(byte[] b) {
        // Toda linea que no tiene ':' antes de terminar y no esta vacia tiene que ser continuacion.
        int inicio = 0;
        int i = 0;
        while (i < b.length) {
            if (b[i] == '\n') {
                int fin = i;
                if (fin > inicio && b[fin - 1] == '\r') {
                    fin = fin - 1;
                }
                if (fin > inicio) {
                    boolean tieneDosPuntos = false;
                    int k = inicio;
                    while (k < fin) {
                        if (b[k] == ':') {
                            tieneDosPuntos = true;
                        }
                        k = k + 1;
                    }
                    if (!tieneDosPuntos && b[inicio] != ' ') {
                        return false;
                    }
                }
                inicio = i + 1;
            }
            i = i + 1;
        }
        return true;
    }

    // ---- secciones -------------------------------------------------------------------------------

    static void secciones() throws Exception {
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("Main-Class", "com.ejemplo.Principal");
        Attributes uno = new Attributes();
        uno.putValue("Sealed", "true");
        m.getEntries().put("com/ejemplo/", uno);

        byte[] b = escribir(m);
        Manifest r = new Manifest(new ByteArrayInputStream(b));
        ok("com.ejemplo.Principal".equals(r.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS)));
        ok(r.getEntries().size() == 1);
        ok(r.getAttributes("com/ejemplo/") != null);
        ok("true".equals(r.getAttributes("com/ejemplo/").getValue("SEALED")));
        ok(r.getAttributes("no/esta/") == null);
        ok(r.equals(m));

        // Escribir lo leido tiene que dar los mismos bytes.
        ok(iguales(b, escribir(r)));

        // La copia es independiente.
        Manifest c = new Manifest(m);
        c.getMainAttributes().putValue("Main-Class", "otra.Cosa");
        ok("com.ejemplo.Principal".equals(m.getMainAttributes().getValue("Main-Class")));

        // Y `clear` deja las dos partes vacias.
        c.clear();
        ok(c.getMainAttributes().isEmpty() && c.getEntries().isEmpty());
    }

    static boolean noLee(String texto) {
        try {
            new Manifest(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    static void malFormados() throws Exception {
        // Falta el espacio detras de los dos puntos.
        ok(noLee("Manifest-Version: 1.0\r\nA:uno\r\n"));
        // No hay dos puntos.
        ok(noLee("Manifest-Version: 1.0\r\nabc\r\n"));
        // Una continuacion sin nada adelante.
        ok(noLee(" oops\r\n"));
        // Una seccion que no arranca con `Name`.
        ok(noLee("Manifest-Version: 1.0\r\n\r\nSealed: true\r\n"));
        // Un nombre de cabecera con un caracter que no va.
        ok(noLee("Manifest-Version: 1.0\r\nA B: x\r\n"));

        // Y lo que si tiene que leer: los tres finales de linea, y una seccion con `Name`.
        try {
            Manifest m = new Manifest(new ByteArrayInputStream(
                    "Manifest-Version: 1.0\nA: uno\n".getBytes(StandardCharsets.UTF_8)));
            ok("uno".equals(m.getMainAttributes().getValue("A")));
            Manifest m2 = new Manifest(new ByteArrayInputStream(
                    "Manifest-Version: 1.0\rA: dos\r".getBytes(StandardCharsets.UTF_8)));
            ok("dos".equals(m2.getMainAttributes().getValue("A")));
            Manifest m3 = new Manifest(new ByteArrayInputStream(
                    "Manifest-Version: 1.0\r\n\r\nName: a/b\r\nX: 1\r\n".getBytes(StandardCharsets.UTF_8)));
            ok("1".equals(m3.getAttributes("a/b").getValue("X")));
            // Un valor vacio es un valor, no la ausencia del atributo.
            Manifest m4 = new Manifest(new ByteArrayInputStream(
                    "Manifest-Version: 1.0\r\nVacio: \r\n".getBytes(StandardCharsets.UTF_8)));
            ok("".equals(m4.getMainAttributes().getValue("Vacio")));
        } catch (IOException e) {
            ok(false);
        }
    }

    // ---- Attributes y Attributes.Name -----------------------------------------------------------

    static void atributos() throws Exception {
        Attributes a = new Attributes();
        a.putValue("Class-Path", "lib/a.jar");
        ok(a.size() == 1);
        ok("lib/a.jar".equals(a.getValue(Attributes.Name.CLASS_PATH)));
        ok("lib/a.jar".equals(a.getValue("class-path")));
        // La clave es un `Name`, no un `String`: buscar con texto pelado no encuentra nada.
        ok(a.get("Class-Path") == null);
        ok("lib/a.jar".equals(a.get(new Attributes.Name("CLASS-PATH"))));
        ok(a.containsKey(new Attributes.Name("class-path")));
        ok(a.getValue("No-Esta") == null);

        boolean tiro = false;
        try {
            a.put("Class-Path", "x");
        } catch (ClassCastException e) {
            tiro = true;
        }
        ok(tiro);

        // Los nombres no distinguen mayusculas, y el hash tiene que acompaniar.
        Attributes.Name n1 = new Attributes.Name("Foo-Bar");
        Attributes.Name n2 = new Attributes.Name("foo-bar");
        ok(n1.equals(n2));
        ok(n1.hashCode() == n2.hashCode());
        ok("Foo-Bar".equals(n1.toString()));

        ok(nombreInvalido("a b"));
        ok(nombreInvalido(""));
        ok(nombreInvalido("a.b"));
        ok(nombreInvalido("a:b"));
        ok(nombreInvalido(repetir("x", 71)));
        ok(!nombreInvalido(repetir("x", 70)));
        ok(!nombreInvalido("a_b-9Z"));
        boolean npe = false;
        try {
            new Attributes.Name(null);
        } catch (NullPointerException e) {
            npe = true;
        }
        ok(npe);

        // Copia y clon.
        Attributes b = new Attributes(a);
        ok(b.equals(a));
        b.putValue("Otro", "1");
        ok(!b.equals(a));
        ok(((Attributes) a.clone()).equals(a));
    }

    static boolean nombreInvalido(String s) {
        try {
            new Attributes.Name(s);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // ---- un JAR de verdad ------------------------------------------------------------------------

    static final String[] NOMBRES = { "a/uno.txt", "b/dos.txt" };

    static byte[] contenido(int i) {
        if (i == 0) {
            return "contenido uno".getBytes(StandardCharsets.UTF_8);
        }
        return repetir("dos ", 100).getBytes(StandardCharsets.UTF_8);
    }

    static Manifest manifiestoDePrueba() {
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("Created-By", "KajiJDK");
        m.getMainAttributes().putValue("Main-Class", "com.ejemplo.Principal");
        Attributes por = new Attributes();
        por.putValue("Sealed", "true");
        m.getEntries().put("a/uno.txt", por);
        return m;
    }

    static void escribirJar(String ruta) throws Exception {
        File f = new File(ruta);
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream fos = new FileOutputStream(f);
        JarOutputStream jos = new JarOutputStream(fos, manifiestoDePrueba());
        int i = 0;
        while (i < NOMBRES.length) {
            jos.putNextEntry(new ZipEntry(NOMBRES[i]));
            byte[] d = contenido(i);
            jos.write(d, 0, d.length);
            jos.closeEntry();
            i = i + 1;
        }
        jos.close();
    }

    static byte[] leerTodo(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        int n = in.read(buf, 0, buf.length);
        while (n > 0) {
            out.write(buf, 0, n);
            n = in.read(buf, 0, buf.length);
        }
        return out.toByteArray();
    }

    static void conJarFile() throws Exception {
        JarFile jf = new JarFile(RUTA);
        Manifest m = jf.getManifest();
        ok(m != null);
        ok("com.ejemplo.Principal".equals(m.getMainAttributes().getValue("Main-Class")));
        ok("KajiJDK".equals(m.getMainAttributes().getValue("Created-By")));

        JarEntry e = jf.getJarEntry("a/uno.txt");
        ok(e != null);
        ok("a/uno.txt".equals(e.getName()));
        ok("a/uno.txt".equals(e.getRealName()));
        // Los atributos de la entrada salen del manifiesto.
        ok(e.getAttributes() != null);
        ok("true".equals(e.getAttributes().getValue("Sealed")));
        // Sin verificacion de firmas no hay ni certificados ni firmantes. En el JDK real este JAR
        // tampoco esta firmado, asi que las dos implementaciones contestan lo mismo.
        ok(e.getCertificates() == null);
        ok(e.getCodeSigners() == null);
        ok(iguales(leerTodo(jf.getInputStream(e)), contenido(0)));

        JarEntry e2 = jf.getJarEntry("b/dos.txt");
        ok(iguales(leerTodo(jf.getInputStream(e2)), contenido(1)));
        ok(e2.getAttributes() == null);
        ok(jf.getJarEntry("no/esta") == null);

        // `getEntry` devuelve lo mismo que `getJarEntry`, con otro tipo estatico.
        ZipEntry z = jf.getEntry("b/dos.txt");
        ok(z instanceof JarEntry);

        // La enumeracion trae el manifiesto mas las dos entradas, y los objetos son de JAR.
        int n = 0;
        boolean todasDeJar = true;
        Enumeration<? extends ZipEntry> en = jf.entries();
        while (en.hasMoreElements()) {
            ZipEntry actual = en.nextElement();
            if (!(actual instanceof JarEntry)) {
                todasDeJar = false;
            }
            n = n + 1;
        }
        ok(n == 3);
        ok(todasDeJar);
        ok(jf.stream().count() == 3L);

        // Un JAR normal no es multi-release, y por eso su version es la base.
        ok(!jf.isMultiRelease());
        ok(jf.getVersion().equals(JarFile.baseVersion()));
        ok(JarFile.baseVersion().feature() == 8);
        ok(JarFile.runtimeVersion().feature() >= 8);
        ok(jf.versionedStream().count() == 3L);
        ok("META-INF/MANIFEST.MF".equals(JarFile.MANIFEST_NAME));
        jf.close();
    }

    static void conJarInputStream() throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(RUTA);
        JarInputStream jis = new JarInputStream(fis);
        // El manifiesto ya se consumio en el constructor.
        ok(jis.getManifest() != null);
        ok("com.ejemplo.Principal".equals(
                jis.getManifest().getMainAttributes().getValue("Main-Class")));

        int i = 0;
        JarEntry e = jis.getNextJarEntry();
        while (e != null) {
            ok(NOMBRES[i].equals(e.getName()));
            ok(iguales(leerTodo(jis), contenido(i)));
            e = jis.getNextJarEntry();
            i = i + 1;
        }
        ok(i == NOMBRES.length);
        jis.close();
    }

    // Un JAR sin manifiesto: `getManifest()` da `null` y la primera entrada no se pierde.
    static void sinManifiesto() throws Exception {
        String ruta = "_kaji_jartest_sinman.jar";
        File f = new File(ruta);
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream fos = new FileOutputStream(f);
        JarOutputStream jos = new JarOutputStream(fos);
        jos.putNextEntry(new ZipEntry("solo.txt"));
        byte[] d = "sin manifiesto".getBytes(StandardCharsets.UTF_8);
        jos.write(d, 0, d.length);
        jos.closeEntry();
        jos.close();

        JarFile jf = new JarFile(ruta);
        ok(jf.getManifest() == null);
        ok(jf.getJarEntry("solo.txt") != null);
        ok(jf.getJarEntry("solo.txt").getAttributes() == null);
        jf.close();

        java.io.FileInputStream fis = new java.io.FileInputStream(ruta);
        JarInputStream jis = new JarInputStream(fis);
        ok(jis.getManifest() == null);
        JarEntry e = jis.getNextJarEntry();
        ok(e != null);
        ok("solo.txt".equals(e.getName()));
        ok(iguales(leerTodo(jis), d));
        ok(jis.getNextJarEntry() == null);
        jis.close();
        f.delete();
    }

    // Multi-release: el mismo nombre resuelto a la version mas alta que no pase de la pedida.
    static void multiRelease() throws Exception {
        String ruta = "_kaji_jartest_mr.jar";
        File f = new File(ruta);
        if (f.exists()) {
            f.delete();
        }
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true");
        FileOutputStream fos = new FileOutputStream(f);
        JarOutputStream jos = new JarOutputStream(fos, m);
        escribirEntrada(jos, "p/C.txt", "base");
        escribirEntrada(jos, "META-INF/versions/9/p/C.txt", "nueve");
        escribirEntrada(jos, "META-INF/versions/11/p/C.txt", "once");
        escribirEntrada(jos, "META-INF/versions/99/p/C.txt", "noventa y nueve");
        jos.close();

        // Abierto en la base: se ve la entrada base.
        JarFile base = new JarFile(f);
        ok(base.isMultiRelease());
        ok(base.getVersion().equals(JarFile.baseVersion()));
        ok(texto(base, "p/C.txt").equals("base"));
        base.close();

        // Abierto a la 10: gana la 9, porque la 11 se pasa.
        JarFile diez = new JarFile(f, true, ZipFile.OPEN_READ, Runtime.Version.parse("10"));
        ok(diez.isMultiRelease());
        ok(diez.getVersion().feature() == 10);
        JarEntry e = diez.getJarEntry("p/C.txt");
        ok("p/C.txt".equals(e.getName()));
        ok("META-INF/versions/9/p/C.txt".equals(e.getRealName()));
        ok(texto(diez, "p/C.txt").equals("nueve"));
        // La vista resuelta tiene un solo `p/C.txt` --mas el manifiesto--, no cuatro.
        ok(diez.versionedStream().count() == 2L);
        // La cruda las tiene todas.
        ok(diez.stream().count() == 5L);
        diez.close();

        // Abierto a la 12: gana la 11.
        JarFile doce = new JarFile(f, true, ZipFile.OPEN_READ, Runtime.Version.parse("12"));
        ok(texto(doce, "p/C.txt").equals("once"));
        doce.close();

        f.delete();
    }

    static void escribirEntrada(JarOutputStream jos, String nombre, String texto) throws Exception {
        jos.putNextEntry(new ZipEntry(nombre));
        byte[] d = texto.getBytes(StandardCharsets.UTF_8);
        jos.write(d, 0, d.length);
        jos.closeEntry();
    }

    static String texto(JarFile jf, String nombre) throws Exception {
        JarEntry e = jf.getJarEntry(nombre);
        if (e == null) {
            return null;
        }
        return new String(leerTodo(jf.getInputStream(e)), StandardCharsets.UTF_8);
    }

    static void limpiar() {
        File f = new File(RUTA);
        if (f.exists()) {
            ok(f.delete());
        }
    }

    public static int run() throws Exception {
        plegadoAscii();
        plegadoUtf8();
        secciones();
        malFormados();
        atributos();
        escribirJar(RUTA);
        conJarFile();
        conJarInputStream();
        sinManifiesto();
        multiRelease();
        limpiar();
        return primerFallo;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
