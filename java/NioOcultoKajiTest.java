// Las diferencias **a proposito** de `java.nio.file` en KajiJDK, clavadas para que no se muevan sin
// que alguien se entere.
//
// **Esta prueba da -1 solo con `run-headless`.** Con el `java` real da el indice de la primera
// diferencia, y eso no es una falla: cada comprobacion de aca es un lugar donde KajiJDK contesta
// distinto que el JDK y el porque esta escrito en el codigo de la biblioteca. Lo comparable con el
// JDK vive en `NioOcultoTest`. Correrla igual con el `java` real sirve para lo otro: que el
// bytecode que emite nuestro `javac` cargue y ejecute en la JVM de verdad.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.spi.FileSystemProvider;

public class NioOcultoKajiTest {

    static int cuenta = 0;
    static int primerFallo = -1;

    static void ok(boolean c) {
        if (!c && primerFallo < 0) {
            primerFallo = cuenta;
        }
        cuenta = cuenta + 1;
    }

    // La regla de oculto de este proveedor: el nombre empieza con un punto. La spec deja la
    // definicion al proveedor, y esta es la misma que ya usa `java.io.File.isHidden()`.
    static void reglaDelPunto() throws IOException {
        ok(Files.isHidden(Path.of("._kaji_no_existe")));
        ok(Files.isHidden(Path.of("_kaji_dir", "._kaji_no_existe")));
        ok(!Files.isHidden(Path.of("_kaji_no_existe.txt")));
        ok(!Files.isHidden(Path.of("_kaji_dir", "_kaji_no_existe.txt")));

        // Un nombre con puntos que no van al principio no cuenta.
        ok(!Files.isHidden(Path.of("_kaji_no_existe.tar.gz")));

        // Ninguna de las cinco rutas de arriba existe --y se comprueba, porque de eso se trata: se
        // contesta por el nombre y no se toca el disco. El JDK de Windows tiraria
        // `NoSuchFileException` en las cinco.
        ok(!Files.exists(Path.of("._kaji_no_existe")));
        ok(!Files.exists(Path.of("_kaji_no_existe.txt")));

        // Una ruta sin nombre de archivo no esta oculta.
        ok(!Files.isHidden(Path.of("/")));

        // Y el proveedor contesta lo mismo que `Files`.
        FileSystemProvider prov = FileSystems.getDefault().provider();
        ok(prov.isHidden(Path.of(".oculto")));
        ok(!prov.isHidden(Path.of("visible")));
    }

    // `isSameFile` del proveedor contesta el subconjunto que se puede --dos escrituras de la misma
    // ruta-- y **falla** en el resto, en vez de adivinar.
    //
    // La tercera comprobacion es la que importa: con `user.dir` valiendo `null` en esta VM,
    // `toAbsolutePath()` inventa `\` como directorio actual, y comparar una ruta relativa contra una
    // absoluta por esa via daba `true` para `a.txt` y `\a.txt`, que son archivos distintos. Ahora
    // solo se comparan dos relativas o dos absolutas, y el caso mezclado falla.
    static void mismoArchivo() throws IOException {
        FileSystemProvider prov = FileSystems.getDefault().provider();

        ok(prov.isSameFile(Path.of("x", "y.txt"), Path.of("x", "y.txt")));
        ok(prov.isSameFile(Path.of("x", ".", "y.txt"), Path.of("x", "y.txt")));

        ok(tiraUoe(prov, Path.of("a.txt"), Path.of("/a.txt")));
        ok(tiraUoe(prov, Path.of("a.txt"), Path.of("b.txt")));
    }

    static boolean tiraUoe(FileSystemProvider prov, Path a, Path b) throws IOException {
        try {
            prov.isSameFile(a, b);
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    // El sistema de archivos por omision: se escribe, no tiene vistas de atributos, y no sabe de
    // usuarios.
    //
    // **`getRootDirectories()` ya no esta en esta lista**, y vale la pena decir por que salio: era
    // una diferencia a proposito --devolvia vacio-- mientras la VM no supiera enumerar las unidades
    // del sistema. Ahora las enumera, asi que dejo de ser una diferencia y paso a ser lo mismo que
    // hace el JDK. Lo que se comprueba aca es eso: que enumere.
    static void sistemaDeArchivos() {
        FileSystem fs = FileSystems.getDefault();

        ok(!fs.isReadOnly());
        ok(fs.isOpen());
        ok(fs.getRootDirectories().iterator().hasNext());

        // Vacio y no `{"basic"}`: los atributos se **leen**, pero la vista tambien escribiria.
        ok(fs.supportedFileAttributeViews().isEmpty());
        ok(Files.getFileAttributeView(Path.of("x"), BasicFileAttributeView.class) == null);

        // `getFileStores()` tampoco esta ya en la lista de diferencias, por lo mismo que
        // `getRootDirectories()`: devolvia vacio mientras la VM no supiera medir un volumen, y ahora
        // los mide. Lo que se comprueba es que devuelva alguno.
        ok(fs.getFileStores().iterator().hasNext());

        boolean tiro = false;
        try {
            fs.getUserPrincipalLookupService();
        } catch (UnsupportedOperationException e) {
            tiro = true;
        }
        ok(tiro);

        tiro = false;
        try {
            fs.newWatchService();
        } catch (UnsupportedOperationException e) {
            tiro = true;
        } catch (IOException e) {
            tiro = false;
        }
        ok(tiro);
    }

    // Enumerar un directorio es la ausencia mas grande del paquete: no hay nativo. El proveedor lo
    // dice en voz alta en vez de devolver un stream vacio.
    static void sinEnumerar() {
        FileSystemProvider prov = FileSystems.getDefault().provider();
        boolean tiro = false;
        try {
            prov.newDirectoryStream(Path.of("."), null);
        } catch (UnsupportedOperationException e) {
            tiro = true;
        } catch (IOException e) {
            tiro = false;
        }
        ok(tiro);

        tiro = false;
        try {
            prov.getFileStore(Path.of("."));
        } catch (UnsupportedOperationException e) {
            tiro = true;
        } catch (IOException e) {
            tiro = false;
        }
        ok(tiro);
    }

    public static int run() throws Exception {
        reglaDelPunto();
        mismoArchivo();
        sistemaDeArchivos();
        sinEnumerar();
        return primerFallo;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
