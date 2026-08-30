package java.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import jdk.internal.io.Fs;

// KajiLibrary's java.io.File -- an abstract path name. The path-manipulation half is fully modelled
// (naming, parents, absoluteness, normalization, URI/URL conversion, ordering); the file-system half
// is honestly inert, because KajiJDK has no file system: existence/permission/metadata queries
// answer "no" (false / 0 / null), the mutators fail (false / no-op), and the temp-file / space
// queries report nothing. This is what lets File be the parameter type of Runtime.exec(...) and be
// passed around by programs that only manipulate paths.
//
public class File implements Serializable, Comparable<File> {

    /** The system-dependent name-separator character. */
    public static final char separatorChar = System.getProperty("file.separator").charAt(0);

    /** The system-dependent name-separator, as a string. */
    public static final String separator = String.valueOf(separatorChar);

    /** The system-dependent path-separator character. */
    public static final char pathSeparatorChar = System.getProperty("path.separator").charAt(0);

    /** The system-dependent path-separator, as a string. */
    public static final String pathSeparator = String.valueOf(pathSeparatorChar);

    // The normalized abstract path name.
    private final String path;

    public File(String pathname) {
        this.path = normalize(pathname);
    }

    public File(String parent, String child) {
        if (child == null) {
            throw new NullPointerException("child cannot be null");
        }
        if (parent == null || parent.length() == 0) {
            this.path = normalize(child);
        } else {
            this.path = normalize(parent + separatorChar + child);
        }
    }

    public File(File parent, String child) {
        this(parent == null ? null : parent.path, child);
    }

    public File(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri cannot be null");
        }
        String p = uri.getPath();
        if (p == null) {
            throw new IllegalArgumentException("URI has no path: " + uri);
        }
        this.path = normalize(p);
    }

    // ---- path manipulation ----

    /** The name of the file or directory this path denotes (its last segment). */
    public String getName() {
        int i = this.path.lastIndexOf(separatorChar);
        return (i < 0) ? this.path : this.path.substring(i + 1);
    }

    /** The parent path, or null if this path names no parent. */
    public String getParent() {
        int i = this.path.lastIndexOf(separatorChar);
        if (i < 0) {
            return null;
        }
        if (i == 0) {
            return separator; // parent of "/x" is "/"
        }
        return this.path.substring(0, i);
    }

    /** The parent as a {@code File}, or null. */
    public File getParentFile() {
        String p = this.getParent();
        return (p == null) ? null : new File(p);
    }

    /** This path in string form. */
    public String getPath() {
        return this.path;
    }

    /** Whether this path is absolute. */
    public boolean isAbsolute() {
        if (this.path.length() == 0) {
            return false;
        }
        if (separatorChar == '\\') {
            // Windows: a drive-letter root ("C:\") or a UNC path ("\\host").
            if (this.path.length() >= 3 && this.path.charAt(1) == ':'
                    && this.path.charAt(2) == separatorChar) {
                return true;
            }
            return this.path.length() >= 2 && this.path.charAt(0) == separatorChar
                    && this.path.charAt(1) == separatorChar;
        }
        // POSIX: a leading '/'.
        return this.path.charAt(0) == separatorChar;
    }

    /** This path made absolute (relative paths are resolved against the working directory). */
    public String getAbsolutePath() {
        if (this.isAbsolute()) {
            return this.path;
        }
        String cwd = System.getProperty("user.dir");
        if (cwd == null || cwd.length() == 0) {
            cwd = separator;
        }
        return normalize(cwd + separatorChar + this.path);
    }

    /** This path made absolute, as a {@code File}. */
    public File getAbsoluteFile() {
        return new File(this.getAbsolutePath());
    }

    /**
     * The canonical path. KajiJDK has no file system to resolve symlinks or {@code .}/{@code ..}
     * against, so this is the (normalized) absolute path.
     */
    public String getCanonicalPath() throws IOException {
        return this.getAbsolutePath();
    }

    /** The canonical path, as a {@code File}. */
    public File getCanonicalFile() throws IOException {
        return new File(this.getCanonicalPath());
    }

    // ---- URI / URL ----

    /** A {@code file:} URI for this abstract path. */
    public URI toURI() {
        String p = this.getAbsolutePath();
        StringBuilder sb = new StringBuilder("file:");
        if (p.length() == 0 || p.charAt(0) != separatorChar) {
            sb.append('/');
        }
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            sb.append(c == separatorChar ? '/' : c);
            i = i + 1;
        }
        return URI.create(sb.toString());
    }

    /**
     * @deprecated use {@link #toURI()} then {@link URI#toString()} with a URL when one is needed.
     */
    @Deprecated
    public URL toURL() throws MalformedURLException {
        return new URL(this.toURI().toString());
    }

    // ---- el estado en el disco ----
    //
    // Los seis salen de **una sola** llamada a `Fs.stat`, que devuelve las banderas juntas. Es a
    // proposito: preguntarlas por separado tocaria el disco una vez por cada una y --peor-- podria
    // dar respuestas de momentos distintos si algo cambia en el medio. Aca cada metodo hace su
    // propia consulta igual, porque una `File` no cachea nada: el archivo puede aparecer o
    // desaparecer entre dos llamadas, y una respuesta guardada seria una mentira con fecha.

    public boolean canRead() {
        return (Fs.stat(this.path) & Fs.SE_LEE) != 0;
    }

    public boolean canWrite() {
        return (Fs.stat(this.path) & Fs.SE_ESCRIBE) != 0;
    }

    /**
     * Si se puede ejecutar.
     *
     * <p>Se responde con "se puede leer", que en Windows es lo mismo para cualquier archivo y en
     * POSIX no. Es la unica de las tres que esta biblioteca no distingue, y se documenta en vez de
     * devolver `false` --que seria mentir sobre todo ejecutable-- o `true` --que lo seria sobre
     * todo lo demas--.
     */
    public boolean canExecute() {
        return (Fs.stat(this.path) & Fs.SE_LEE) != 0;
    }

    public boolean exists() {
        return (Fs.stat(this.path) & Fs.EXISTE) != 0;
    }

    public boolean isDirectory() {
        return (Fs.stat(this.path) & Fs.ES_DIRECTORIO) != 0;
    }

    public boolean isFile() {
        return (Fs.stat(this.path) & Fs.ES_ARCHIVO) != 0;
    }

    public boolean isHidden() {
        return this.getName().startsWith(".");
    }

    /**
     * Cuando se modifico por ultima vez.
     *
     * <p>Devuelve 0 -- "no se sabe" --, que es lo que el contrato dice para un archivo del que no se
     * puede obtener la fecha. El nativo no la expone todavia; agregarla es una linea de Rust, y
     * hasta entonces esto no inventa una fecha.
     */
    public long lastModified() {
        return 0L;
    }

    public long length() {
        return Fs.size(this.path);
    }

    // ---- mutacion ----

    /**
     * Crea el archivo si no existe. `true` si lo creo esta llamada.
     *
     * <p>La comprobacion y la creacion **no** son atomicas aca, a diferencia del JDK: entre el
     * `exists()` y el `writeAllBytes` otro proceso puede crear el archivo, y entonces esto devuelve
     * `true` habiendolo pisado con vacio. Se dice de frente porque el javadoc del JDK promete
     * atomicidad y este no la tiene.
     */
    public boolean createNewFile() throws IOException {
        if (this.exists()) {
            return false;
        }
        return Fs.writeAllBytes(this.path, new byte[0], false);
    }

    public boolean delete() {
        return Fs.delete(this.path);
    }

    public void deleteOnExit() {
    }

    public String[] list() {
        return null;
    }

    public String[] list(FilenameFilter filter) {
        return null;
    }

    public File[] listFiles() {
        return null;
    }

    public File[] listFiles(FilenameFilter filter) {
        return null;
    }

    public File[] listFiles(FileFilter filter) {
        return null;
    }

    public boolean mkdir() {
        return Fs.mkdir(this.path, false);
    }

    /** Idem, creando tambien los directorios padres que falten. */
    public boolean mkdirs() {
        return Fs.mkdir(this.path, true);
    }

    public boolean renameTo(File dest) {
        return false;
    }

    public boolean setLastModified(long time) {
        return false;
    }

    public boolean setReadOnly() {
        return false;
    }

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    public boolean setWritable(boolean writable) {
        return false;
    }

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    public boolean setReadable(boolean readable) {
        return false;
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    public boolean setExecutable(boolean executable) {
        return false;
    }

    // ---- roots / space / temp (inert) ----

    /** The file-system roots. KajiJDK exposes none. */
    public static File[] listRoots() {
        return new File[0];
    }

    public long getTotalSpace() {
        return 0L;
    }

    public long getFreeSpace() {
        return 0L;
    }

    public long getUsableSpace() {
        return 0L;
    }

    /** @throws IOException always -- KajiJDK has no file system to create a temp file in. */
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        throw new IOException("KajiJDK has no file system");
    }

    /** @throws IOException always -- KajiJDK has no file system to create a temp file in. */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null);
    }

    // ---- identity / ordering ----

    public int compareTo(File other) {
        return this.path.compareTo(other.path);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof File)) {
            return false;
        }
        return this.path.equals(((File) obj).path);
    }

    public int hashCode() {
        return this.path.hashCode() ^ 1234321;
    }

    public String toString() {
        return this.path;
    }

    /** A {@link Path} for this abstract path name. */
    public Path toPath() {
        return Path.of(this.path);
    }

    // Collapse mixed/duplicate separators to the platform separator and drop a trailing one.
    private static String normalize(String p) {
        if (p == null) {
            throw new NullPointerException("path cannot be null");
        }
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (c == '/' || c == '\\') {
                c = separatorChar;
            }
            if (!(c == separatorChar && prev == separatorChar)) {
                sb.append(c);
                prev = c;
            }
            i = i + 1;
        }
        int len = sb.length();
        if (len > 1 && sb.charAt(len - 1) == separatorChar) {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }
}
