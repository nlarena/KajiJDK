// Lo que se agrego y lo que se corrigio en `java.nio.file` en esta pasada: `Files.isHidden`,
// `SecureDirectoryStream.newByteChannel`, `FileSystem.isReadOnly()` y los dos `readAttributes` del
// proveedor, que antes fallaban aunque los de `Files` funcionaran.
//
// **Solo se comprueba lo que las dos VMs contestan igual.** La prueba se corre con `run-headless` y
// con el `java` real, asi que no entra nada donde KajiJDK diverge a proposito. En particular
// **no** se comprueba aca la regla del punto de `isHidden` --KajiJDK dice que `.x` esta oculto y un
// JDK de Windows dice que no, porque mira el bit de DOS-- que es una diferencia de definicion
// documentada en `Files.isHidden` y vive en `NioOcultoKajiTest`.
//
// Tampoco `isHidden` sobre una ruta que no existe: KajiJDK contesta por el nombre y no mira el
// disco, y el JDK de Windows lee los atributos y tira `NoSuchFileException`. Todas las llamadas de
// aca son sobre archivos que la prueba misma creo.
//
// `Sds`, abajo, no se ejecuta: existe para que el **compilador** verifique que los siete metodos de
// `SecureDirectoryStream` son los del JDK. Si nuestra declaracion de `newByteChannel` no coincidiera
// con la real, esta clase no compilaria con el `javac` de `jdk-25`.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioOcultoTest {

    static final String BASE = "_kaji_oculto_";

    static int cuenta = 0;
    static int primerFallo = -1;

    static void ok(boolean c) {
        if (!c && primerFallo < 0) {
            primerFallo = cuenta;
        }
        cuenta = cuenta + 1;
    }

    // Un archivo con nombre corriente --sin punto inicial-- no esta oculto en ninguna de las dos
    // VMs, y ese es el unico caso donde las dos definiciones coinciden.
    static void oculto() throws IOException {
        Path f = Path.of(BASE + "visible.txt");
        Files.write(f, "hola".getBytes(StandardCharsets.UTF_8));

        ok(!Files.isHidden(f));
        ok(!FileSystems.getDefault().provider().isHidden(f));

        Files.delete(f);
    }

    // El sistema de archivos por omision **no** es de solo lectura, y la prueba de que la respuesta
    // no es una declaracion suelta es que escribir funciona en la misma corrida.
    static void escribible() throws IOException {
        FileSystem fs = FileSystems.getDefault();
        ok(!fs.isReadOnly());
        ok(fs.isOpen());

        Path f = Path.of(BASE + "escrito.txt");
        Files.write(f, "abc".getBytes(StandardCharsets.UTF_8));
        ok(Files.exists(f));
        ok(Files.size(f) == 3L);
        Files.delete(f);
        ok(!Files.exists(f));
    }

    // Los dos `readAttributes` del proveedor tienen que contestar lo mismo que los de `Files`. Antes
    // de esta pasada tiraban `UnsupportedOperationException` aunque los de `Files` funcionaran.
    //
    // Solo se miran los atributos que las dos VMs pueden dar iguales: tamaño y forma. Las tres
    // marcas de tiempo no entran --KajiJDK devuelve la epoca, que es lo que la spec manda cuando el
    // sistema de archivos no las soporta, y el JDK devuelve la fecha real-- ni `fileKey`.
    static void atributosDelProveedor() throws IOException {
        FileSystemProvider prov = FileSystems.getDefault().provider();

        Path f = Path.of(BASE + "attr.bin");
        Files.write(f, new byte[] {1, 2, 3, 4, 5});

        BasicFileAttributes a = prov.readAttributes(f, BasicFileAttributes.class);
        ok(a.isRegularFile());
        ok(!a.isDirectory());
        ok(!a.isOther());
        ok(a.size() == 5L);

        // Y coincide con el de `Files`, que es la costura que estaba rota.
        BasicFileAttributes b = Files.readAttributes(f, BasicFileAttributes.class);
        ok(b.size() == a.size());
        ok(b.isRegularFile() == a.isRegularFile());

        Map<String, Object> m = prov.readAttributes(f, "size,isRegularFile");
        ok(m.size() == 2);
        ok(Long.valueOf(5L).equals(m.get("size")));
        ok(Boolean.TRUE.equals(m.get("isRegularFile")));

        // La misma pregunta con `NOFOLLOW_LINKS` no cambia nada: no hay enlaces en el medio.
        BasicFileAttributes c = prov.readAttributes(f, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        ok(c.size() == 5L);

        Files.delete(f);
    }

    // `isSameFile` del proveedor sobre dos escrituras de la misma ruta. Las dos VMs contestan
    // `true`, por caminos distintos: KajiJDK las normaliza, el JDK abre los dos archivos y compara
    // la identidad. Por eso el archivo tiene que existir de verdad.
    static void mismoArchivo() throws IOException {
        FileSystemProvider prov = FileSystems.getDefault().provider();

        Path d = Path.of(BASE + "dir");
        Files.createDirectory(d);
        Path f = d.resolve("x.txt");
        Files.write(f, "x".getBytes(StandardCharsets.UTF_8));

        ok(prov.isSameFile(f, f));
        ok(prov.isSameFile(Path.of(BASE + "dir", ".", "x.txt"), f));
        ok(prov.isSameFile(Path.of(BASE + "dir", "sub", "..", "x.txt"), f));

        Files.delete(f);
        Files.delete(d);
    }

    // No se instancia por lo que hace --todo tira-- sino para que el compilador exija los siete
    // metodos de la interfaz. Ver la nota de la cabecera.
    static final class Sds implements SecureDirectoryStream<Path> {

        public Iterator<Path> iterator() {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
        }

        public SecureDirectoryStream<Path> newDirectoryStream(Path path, LinkOption... options)
                throws IOException {
            throw new UnsupportedOperationException();
        }

        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                FileAttribute<?>... attrs) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void deleteFile(Path path) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void deleteDirectory(Path path) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void move(Path srcpath, SecureDirectoryStream<Path> targetdir, Path targetpath)
                throws IOException {
            throw new UnsupportedOperationException();
        }

        public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
            throw new UnsupportedOperationException();
        }

        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
                LinkOption... options) {
            throw new UnsupportedOperationException();
        }
    }

    static void superficieSegura() throws IOException {
        Sds s = new Sds();
        ok(s instanceof SecureDirectoryStream);
        ok(s instanceof java.nio.file.DirectoryStream);
        s.close();
    }

    public static int run() throws Exception {
        oculto();
        escribible();
        atributosDelProveedor();
        mismoArchivo();
        superficieSegura();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
