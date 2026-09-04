// `java.util.zip` de punta a punta: escribir un archivo comprimido, volver a leerlo con `ZipFile`
// --que hasta esta tanda tiraba `UnsupportedOperationException`-- y con `ZipInputStream`.
//
// **Se comprueba contra `java` real corriendo lo mismo**, y por eso la prueba **crea** el archivo en
// vez de leer uno del repositorio: los dos lados tienen que ver exactamente los mismos bytes.
//
// Lo que mas se cuida es lo que distingue a `ZipFile` de `ZipInputStream`: acceso **aleatorio**.
// Buscar una entrada por nombre sin haber leido las anteriores, y abrirla, es toda la razon de que
// la clase exista -- y es lo que necesita saltar al desplazamiento que dice el directorio central.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static final String RUTA = "_kaji_ziptest.zip";

    // Tres entradas de tamanios muy distintos: una corta, una que comprime mucho --texto repetido--
    // y una que no comprime nada --bytes que no se repiten--. Las tres juntas ejercitan el camino
    // comprimido y el almacenado.
    static final String[] NOMBRES = { "hola.txt", "repetido.txt", "ruido.bin" };

    static byte[] contenido(int i) {
        if (i == 0) {
            return "hola mundo".getBytes(StandardCharsets.UTF_8);
        }
        if (i == 1) {
            StringBuilder sb = new StringBuilder();
            int k = 0;
            while (k < 400) {
                sb.append("abcabcabc");
                k = k + 1;
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }
        byte[] out = new byte[512];
        int k = 0;
        while (k < out.length) {
            // Una secuencia que no se repite en el bloque: no hay nada que comprimir.
            out[k] = (byte) (k * 37 + 11);
            k = k + 1;
        }
        return out;
    }

    static void escribir() throws Exception {
        File f = new File(RUTA);
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream fos = new FileOutputStream(f);
        ZipOutputStream zos = new ZipOutputStream(fos);
        int i = 0;
        while (i < NOMBRES.length) {
            zos.putNextEntry(new ZipEntry(NOMBRES[i]));
            byte[] datos = contenido(i);
            zos.write(datos, 0, datos.length);
            zos.closeEntry();
            i = i + 1;
        }
        zos.close();
        ok(f.exists());
        ok(f.length() > 0L);
    }

    static void leerConZipFile() throws Exception {
        ZipFile zf = new ZipFile(RUTA);
        ok(zf.size() == NOMBRES.length);
        ok(RUTA.equals(zf.getName()));

        // **Acceso aleatorio**: se pide la ultima primero, sin haber leido las anteriores.
        ZipEntry ultima = zf.getEntry(NOMBRES[2]);
        ok(ultima != null);
        ok(NOMBRES[2].equals(ultima.getName()));
        ok(ultima.getSize() == contenido(2).length);
        ok(leerTodo(zf.getInputStream(ultima)).length == contenido(2).length);
        ok(iguales(leerTodo(zf.getInputStream(ultima)), contenido(2)));

        // Y despues la primera, para comprobar que el orden no importa.
        ZipEntry primera = zf.getEntry(NOMBRES[0]);
        ok(primera != null);
        ok(iguales(leerTodo(zf.getInputStream(primera)), contenido(0)));

        // La entrada grande. **No se comprueba la razon de compresion**, y eso es a proposito: el
        // `Deflater` de esta biblioteca emite solo bloques `STORED` --lo dice su propio comentario de
        // cabecera, con el motivo-- asi que el tamanio "comprimido" es el original mas unos pocos
        // bytes de cabecera. Es DEFLATE valido y cualquier inflater del mundo lo lee, pero no
        // comprime, y una prueba que exigiera lo contrario estaria midiendo algo que la biblioteca
        // no promete todavia.
        //
        // Lo que si es contrato, y es lo que se comprueba: los tamanios que el directorio guarda son
        // los que hay, y el contenido vuelve **exacto**.
        ZipEntry media = zf.getEntry(NOMBRES[1]);
        ok(media.getSize() == contenido(1).length);
        ok(media.getCompressedSize() > 0L);
        ok(iguales(leerTodo(zf.getInputStream(media)), contenido(1)));

        // El CRC guardado tiene que coincidir con el del contenido.
        CRC32 crc = new CRC32();
        crc.update(contenido(1), 0, contenido(1).length);
        ok(media.getCrc() == crc.getValue());

        // Una que no esta: `null`, no una excepcion.
        ok(zf.getEntry("no_existe.txt") == null);

        // La enumeracion recorre las tres, en orden de escritura.
        // `entries()` devuelve `Enumeration<? extends ZipEntry>` en el JDK: el comodin esta para
        // que una subclase pueda devolver sus propias entradas. Se recibe con el comodin, que es lo
        // que el contrato dice.
        Enumeration<? extends ZipEntry> e = zf.entries();
        int n = 0;
        while (e.hasMoreElements()) {
            // Ligado a una local: encadenar sobre el retorno de un intermedio de tipo comodin se
            // pierde en silencio (#108).
            ZipEntry actual = e.nextElement();
            ok(NOMBRES[n].equals(actual.getName()));
            n = n + 1;
        }
        ok(n == NOMBRES.length);

        // Y el flujo, que es la forma moderna de lo mismo.
        ok(zf.stream().count() == (long) NOMBRES.length);

        zf.close();

        // Despues de cerrar, usarlo falla. No es ceremonia: es lo que hace que el codigo escrito
        // contra esta clase se comporte igual el dia que haya un descriptor de verdad.
        boolean tiro = false;
        try {
            zf.size();
        } catch (IllegalStateException ex) {
            tiro = true;
        }
        ok(tiro);
    }

    // El mismo archivo por la otra puerta: secuencial. Las dos tienen que ver lo mismo.
    static void leerConZipInputStream() throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(RUTA);
        ZipInputStream zis = new ZipInputStream(fis);
        int i = 0;
        ZipEntry e = zis.getNextEntry();
        while (e != null) {
            ok(NOMBRES[i].equals(e.getName()));
            ok(iguales(leerTodo(zis), contenido(i)));
            zis.closeEntry();
            e = zis.getNextEntry();
            i = i + 1;
        }
        ok(i == NOMBRES.length);
        zis.close();
    }

    static void limpiar() {
        File f = new File(RUTA);
        if (f.exists()) {
            ok(f.delete());
        }
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

    public static int run() throws Exception {
        escribir();
        leerConZipFile();
        leerConZipInputStream();
        limpiar();
        return primerFallo;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
