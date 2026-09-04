import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * `Files.getFileStore` y `FileSystem.getFileStores`.
 *
 * <p>Lo que se comprueba es lo que se puede afirmar de cualquier volumen sin saber cual es: que los
 * tres espacios son numeros no negativos, que el total no es menor que lo que queda, que dos
 * llamadas sobre el mismo volumen dan el mismo nombre, y que un archivo que no existe falla en vez
 * de contestar por el volumen de su directorio.
 *
 * <p>**No se compara contra un tamano concreto**, y no por comodidad: el espacio libre de un disco
 * cambia entre dos llamadas. Una prueba que afirmara un numero fallaria sola cuando alguien copie un
 * archivo mientras corre.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `java.nio.file`.
 */
public class FileStoreTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    public static int run() throws IOException {
        failures = 0;

        Path aca = Paths.get(".").toAbsolutePath();
        FileStore fs = Files.getFileStore(aca);
        ok("hay un volumen", fs != null);
        ok("tiene nombre", fs.name() != null);
        ok("tiene tipo", fs.type() != null);
        ok("toString no es nulo", fs.toString() != null);

        long total = fs.getTotalSpace();
        long usable = fs.getUsableSpace();
        long libre = fs.getUnallocatedSpace();
        ok("el total es positivo", total > 0L);
        ok("lo utilizable no es negativo", usable >= 0L);
        ok("lo sin asignar no es negativo", libre >= 0L);
        ok("lo utilizable no pasa del total", usable <= total);
        ok("lo sin asignar no pasa del total", libre <= total);
        // Con cuota, lo utilizable es menor; sin cuota son iguales. Nunca al reves.
        ok("lo utilizable no pasa de lo sin asignar", usable <= libre);

        // Los mismos tres numeros, por nombre de atributo.
        ok("getAttribute da el total", ((Long) fs.getAttribute("totalSpace")).longValue() > 0L);
        ok("getAttribute da lo utilizable",
                ((Long) fs.getAttribute("usableSpace")).longValue() >= 0L);
        ok("getAttribute da lo sin asignar",
                ((Long) fs.getAttribute("unallocatedSpace")).longValue() >= 0L);

        // El JDK tira `UnsupportedOperationException`, no `IllegalArgumentException`. Esta prueba
        // decia lo segundo y `java` de verdad la corrigio.
        boolean tiroAtributo = false;
        try {
            fs.getAttribute("noExisteEsteAtributo");
        } catch (UnsupportedOperationException e) {
            tiroAtributo = true;
        }
        ok("un atributo que no existe tira", tiroAtributo);

        // Dos archivos del mismo volumen dan el mismo nombre de volumen.
        FileStore otro = Files.getFileStore(aca.getParent() == null ? aca : aca.getParent());
        ok("el mismo volumen tiene el mismo nombre", fs.name().equals(otro.name()));

        // La vista de atributos basica esta; una del volumen, no.
        ok("soporta la vista basic", fs.supportsFileAttributeView("basic"));
        ok("no soporta una vista inventada", !fs.supportsFileAttributeView("noExiste"));

        // Un archivo que no existe: falla, y no contesta por el volumen de su directorio.
        boolean tiroFalta = false;
        try {
            Files.getFileStore(aca.resolve("no-existe-este-archivo-de-prueba.tmp"));
        } catch (NoSuchFileException e) {
            tiroFalta = true;
        } catch (IOException e) {
            tiroFalta = true;
        }
        ok("un archivo que no existe falla", tiroFalta);

        boolean tiroNulo = false;
        try {
            Files.getFileStore(null);
        } catch (NullPointerException e) {
            tiroNulo = true;
        }
        ok("una ruta nula tira", tiroNulo);

        // La enumeracion del sistema de archivos: al menos un volumen, y todos contestan.
        int n = 0;
        for (FileStore v : FileSystems.getDefault().getFileStores()) {
            // Solo que no sea nulo: el JDK devuelve la **etiqueta** del volumen, que puede estar
            // vacia si nadie se la puso. Esta prueba exigia largo mayor que cero y `java` de verdad
            // la corrigio -- el contrato de `name()` no promete mas que "su forma depende de la
            // implementacion".
            ok("cada volumen enumerado tiene nombre", v.name() != null);
            n = n + 1;
        }
        ok("hay al menos un volumen enumerado", n > 0);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("FileStoreTest " + FileStoreTest.run());
    }
}
