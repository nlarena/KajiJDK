package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * El {@link FileStore} de un volumen del sistema de archivos por omision.
 *
 * <p>Existe desde que `jdk.internal.io.Fs` sabe preguntar por el espacio de un volumen. Antes no:
 * los ocho miembros que piden datos del volumen habrian tenido que devolver `""` y `0`, y un cero en
 * `getUsableSpace()` no es "no se" sino "no entra nada" -- una respuesta concreta y falsa, del tipo
 * que hace que un programa decida no escribir. Por eso esta clase no estaba y `getFileStore`
 * levantaba.
 *
 * <h2>Que se sabe y que no</h2>
 *
 * <p>Los tres espacios son reales, y son **tres** y no dos: lo utilizable es lo que este usuario
 * puede escribir y lo sin asignar es lo que le queda al volumen. Con una cuota puesta difieren.
 *
 * <p>{@link #type} devuelve `"unknown"`. **No es un relleno**: el nativo pregunta por espacio, no por
 * el tipo del sistema de archivos, y devolver `"ntfs"` porque estamos en Windows seria adivinar --
 * un volumen montado por red o una unidad FAT contestarian lo mismo y estarian mal. La cadena
 * `"unknown"` es la que el propio JDK usa cuando no lo puede determinar, asi que no inventa un
 * formato nuevo.
 *
 * <p>{@link #isReadOnly} devuelve `false`. Es una **cota inferior honesta**: no hay con que
 * preguntarlo, y decir `true` haria que un programa se niegue a escribir donde si puede. Un intento
 * de escritura sobre un volumen de solo lectura falla igual, con su `IOException`, que es donde la
 * verdad aparece de todas formas.
 */
final class KajiFileStore extends FileStore {

    private final String ruta;
    private final String nombre;

    KajiFileStore(String ruta, String nombre) {
        this.ruta = ruta;
        this.nombre = nombre;
    }

    public String name() {
        return this.nombre;
    }

    /**
     * El nombre del volumen que contiene a esa ruta absoluta.
     *
     * <p>En Windows es la letra con sus dos puntos (`C:`); en cualquier otro sistema, la raiz (`/`).
     * No es el nombre que el usuario le puso al volumen --eso no se puede preguntar-- sino el que lo
     * identifica, que es lo que `name()` promete: "su forma depende del sistema; puede no ser
     * unico".
     */
    static String nombreDeVolumen(String ruta) {
        if (ruta.length() >= 2 && ruta.charAt(1) == ':') {
            return ruta.substring(0, 2);
        }
        return "/";
    }

    /** Siempre `"unknown"`. Ver la nota de la clase sobre por que no se adivina. */
    public String type() {
        return "unknown";
    }

    /** Siempre `false`. Ver la nota de la clase. */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * El tamano total del volumen.
     *
     * @throws IOException si no se pudo averiguar
     */
    public long getTotalSpace() throws IOException {
        return KajiFileStore.exigir(jdk.internal.io.Fs.diskTotal(this.ruta), "total");
    }

    /**
     * Lo que este usuario puede escribir.
     *
     * @throws IOException si no se pudo averiguar
     */
    public long getUsableSpace() throws IOException {
        return KajiFileStore.exigir(jdk.internal.io.Fs.diskUsable(this.ruta), "utilizable");
    }

    /**
     * Los bytes sin asignar del volumen.
     *
     * @throws IOException si no se pudo averiguar
     */
    public long getUnallocatedSpace() throws IOException {
        return KajiFileStore.exigir(jdk.internal.io.Fs.diskUnallocated(this.ruta), "sin asignar");
    }

    // El -1 del nativo significa "no se pudo", no un tamano. Traducirlo a la excepcion que la firma
    // declara es lo unico que deja al llamador distinguir las dos cosas.
    private static long exigir(long v, String cual) throws IOException {
        if (v < 0L) {
            throw new IOException("no se pudo leer el espacio " + cual + " del volumen");
        }
        return v;
    }

    /**
     * Solo {@link BasicFileAttributeView}.
     *
     * <p>Es la unica vista que este sistema de archivos implementa, y contestar por las otras seria
     * prometer atributos que despues no se pueden leer.
     */
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return type == BasicFileAttributeView.class;
    }

    /** Solo `"basic"`. Ver la otra forma. */
    public boolean supportsFileAttributeView(String name) {
        return "basic".equals(name);
    }

    /**
     * Siempre `null`.
     *
     * <p>Una vista de atributos **del volumen** --no de un archivo-- es lo que este metodo devuelve,
     * y no hay ninguna: `Fs` sabe del espacio y nada mas. `null` es lo que el contrato define para
     * "no soportada", asi que decirlo asi no pierde nada.
     */
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        return null;
    }

    /**
     * Los tres espacios, por su nombre.
     *
     * <p>Los nombres son los que el JDK define (`totalSpace`, `usableSpace`, `unallocatedSpace`), y
     * cualquier otro es `UnsupportedOperationException` -- no `null`. La diferencia importa: `null`
     * seria "ese atributo vale nada" y lo que pasa es que ese atributo no existe.
     *
     * <p>La excepcion es `UnsupportedOperationException` y no `IllegalArgumentException` porque es
     * lo que contesta el JDK: se comprobo corriendo el mismo caso con `java` de verdad, que tira
     * `UnsupportedOperationException: 'x' not recognized`. La expectativa equivocada era la mia.
     *
     * @throws IOException si no se pudo averiguar
     * @throws UnsupportedOperationException si el atributo no es uno de los tres
     */
    public Object getAttribute(String attribute) throws IOException {
        if ("totalSpace".equals(attribute)) {
            return Long.valueOf(this.getTotalSpace());
        }
        if ("usableSpace".equals(attribute)) {
            return Long.valueOf(this.getUsableSpace());
        }
        if ("unallocatedSpace".equals(attribute)) {
            return Long.valueOf(this.getUnallocatedSpace());
        }
        throw new UnsupportedOperationException("'" + attribute + "' not recognized");
    }

    public String toString() {
        return this.nombre;
    }
}
