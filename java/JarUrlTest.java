import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * `java.net.JarURLConnection`: partir una URL `jar:` y leer lo que nombra.
 *
 * <p>La clase es abstracta y lo unico que le falta a una subclase es **conseguir el archivo**, asi
 * que la prueba escribe un `.jar` de verdad en disco y una subclase de cuatro lineas que lo abre.
 * Con eso se ejercitan los ocho miembros que no dependen del transporte, que son los que esta clase
 * implementa.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `java.net`.
 */
public class JarUrlTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /** La subclase minima: la que sabe abrir un `jar:file:`. */
    static final class DeArchivo extends JarURLConnection {

        private final File archivo;

        DeArchivo(URL url, File archivo) throws MalformedURLException {
            super(url);
            this.archivo = archivo;
        }

        public JarFile getJarFile() throws IOException {
            return new JarFile(this.archivo);
        }

        public void connect() throws IOException {
            // Abrir el archivo es todo lo que hay que conectar acá.
            this.getJarFile();
        }
    }

    /** Escribe un `.jar` con manifiesto y dos entradas. */
    static File armarJar() throws IOException {
        File f = new File("scratchpad/zzjar/prueba.jar");
        f.getParentFile().mkdirs();
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putValue("Creado-Por", "JarUrlTest");
        JarOutputStream out = new JarOutputStream(new FileOutputStream(f), m);
        out.putNextEntry(new JarEntry("hola.txt"));
        out.write("hola".getBytes("UTF-8"));
        out.closeEntry();
        out.putNextEntry(new JarEntry("dir/con espacio.txt"));
        out.write("con espacio".getBytes("UTF-8"));
        out.closeEntry();
        out.close();
        return f;
    }

    static URL urlDeJar(File f, String entrada) throws IOException {
        return new URL("jar:" + f.toURI().toURL().toString() + "!/" + entrada);
    }

    public static int run() throws Exception {
        failures = 0;
        File jar = JarUrlTest.armarJar();

        // ---- partir la URL
        DeArchivo c = new DeArchivo(JarUrlTest.urlDeJar(jar, "hola.txt"), jar);
        ok("la entrada se lee", "hola.txt".equals(c.getEntryName()));
        ok("la URL del jar no lleva la entrada",
                c.getJarFileURL().toString().endsWith("prueba.jar"));
        ok("y no tiene el !/", c.getJarFileURL().toString().indexOf("!/") < 0);

        // ---- el archivo entero: la entrada es null, no ""
        DeArchivo entero = new DeArchivo(JarUrlTest.urlDeJar(jar, ""), jar);
        ok("sin entrada, getEntryName es null", entero.getEntryName() == null);
        ok("sin entrada, getJarEntry es null", entero.getJarEntry() == null);
        ok("sin entrada, getAttributes es null", entero.getAttributes() == null);
        ok("sin entrada, getCertificates es null", entero.getCertificates() == null);

        // ---- el nombre viene percent-encoded y hay que decodificarlo
        DeArchivo conEspacio =
                new DeArchivo(JarUrlTest.urlDeJar(jar, "dir/con%20espacio.txt"), jar);
        ok("el %20 se decodifica", "dir/con espacio.txt".equals(conEspacio.getEntryName()));
        ok("y asi la entrada se encuentra", conEspacio.getJarEntry() != null);

        // ---- el manifiesto y sus atributos principales
        Manifest m = c.getManifest();
        ok("hay manifiesto", m != null);
        ok("los atributos principales llegan",
                "JarUrlTest".equals(c.getMainAttributes().getValue("Creado-Por")));

        // ---- la entrada
        JarEntry e = c.getJarEntry();
        ok("la entrada se encuentra", e != null && "hola.txt".equals(e.getName()));
        // Sin firmar y sin seccion propia en el manifiesto, los atributos de la entrada son null.
        ok("una entrada sin seccion propia no tiene atributos", c.getAttributes() == null);
        ok("y sin leerla no hay certificados", c.getCertificates() == null);

        // ---- una URL sin el separador no es una URL jar
        boolean tiro = false;
        try {
            new DeArchivo(new URL("jar:file:/x/a.jar"), jar);
        } catch (MalformedURLException ex) {
            tiro = true;
        }
        ok("una URL jar sin !/ es malformada", tiro);

        // ---- una entrada que no existe da null, no una excepcion
        //
        // Escribi esta comprobacion al reves --esperando que tirara-- y `java` de verdad me corrigio:
        // esta clase no comprueba que la entrada exista. El que lo hace es el manejador de protocolo
        // concreto, al conectar.
        ok("una entrada que no existe da null",
                new DeArchivo(JarUrlTest.urlDeJar(jar, "no-esta.txt"), jar).getJarEntry() == null);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("JarUrlTest " + JarUrlTest.run());
    }
}
