package java.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import jdk.internal.io.Fs;

// KajiLibrary's java.io.File -- un nombre de ruta abstracto.
//
// La mitad que manipula rutas esta entera (nombres, padres, absolutizacion, normalizacion,
// conversion a URI/URL, orden). La otra mitad --la que toca el disco-- se apoya en
// `jdk.internal.io.Fs`, y hoy contesta de verdad: existencia y permisos (`stat`), tamaño (`size`),
// fecha (`mtime`/`setMtime`), listado (`list`), camino canonico (`canonical`), creacion y borrado.
//
// **Lo que sigue inerte, y por que.** Cuatro grupos de metodos devuelven `false`, `0` o vacio
// porque el nativo que los contestaria no existe. No inventan una respuesta: dicen que no pudieron,
// que es lo que el contrato permite decir.
//
//   - `renameTo` -- renombrar necesita un nativo propio. Simularlo con copiar-y-borrar no seria un
//     renombre: no es atomico, no funciona sobre directorios, y pierde los metadatos.
//   - `setReadOnly` / `setWritable` / `setReadable` / `setExecutable` -- cambiar permisos. Devolver
//     `true` sin haberlos cambiado convertiria un "no pude" en un "listo".
//   - `getTotalSpace` / `getFreeSpace` / `getUsableSpace` -- `0L` es lo que el contrato manda
//     devolver cuando la particion no se puede consultar.
//   - `listRoots` -- un arreglo vacio, que el contrato admite explicitamente.
//
// `isHidden` mira el punto inicial del nombre, que es la regla de Unix. En Windows lo oculto es un
// atributo del archivo y no una convencion de nombre, asi que ahi la respuesta puede diferir de la
// del JDK; se deja porque es la unica regla que se puede aplicar sin nativo, y equivocarse hacia
// "no esta oculto" no rompe nada que dependa de esto.
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
     * El camino **canonico**: absoluto, sin `.` ni `..`, con los enlaces resueltos y --en Windows--
     * con las mayusculas que el disco tiene de verdad.
     *
     * <p>Se lo pide al sistema (`Fs.canonical`) y no se calcula sobre la cadena, porque es la unica
     * forma de que dos rutas distintas que nombran el mismo archivo den el mismo resultado: en
     * Windows `C:\A.TXT` y `c:\a.txt` son el mismo archivo, y ninguna manipulacion de texto lo sabe.
     *
     * <p><strong>Un archivo que no existe igual tiene camino canonico.</strong> El contrato lo pide
     * --canonicalizar es una operacion sobre el nombre-- y el nativo no puede darlo, porque
     * canonicalizar lo que no esta no tiene respuesta del sistema. Asi que para esos se canonicaliza
     * el ancestro mas cercano que **si** exista y se le vuelven a colgar los nombres que faltaban.
     * El resultado tiene las mayusculas reales hasta donde el disco pudo decirlas y las escritas de
     * ahi en adelante, que es exactamente lo que hace el JDK.
     *
     * @throws IOException si ni siquiera se pudo armar la ruta absoluta
     */
    public String getCanonicalPath() throws IOException {
        String abs = normalizarPuntos(this.getAbsolutePath());
        String directo = despojarVerbatim(Fs.canonical(abs));
        if (directo != null) {
            return directo;
        }
        // No existe: se sube hasta el primer ancestro que si, y se reconstruye desde ahi.
        StringBuilder cola = new StringBuilder();
        String actual = abs;
        while (true) {
            int corte = actual.lastIndexOf(separatorChar);
            if (corte < 0) {
                return abs;                       // sin padre que consultar: la absoluta y listo
            }
            String nombre = actual.substring(corte + 1);
            actual = corte == 0 ? separator : actual.substring(0, corte);
            if (nombre.length() != 0) {
                cola.insert(0, nombre);
                cola.insert(0, separatorChar);
            }
            String base = despojarVerbatim(Fs.canonical(actual));
            if (base != null) {
                // Una raiz ya termina en separador (`C:\`); pegarle otro daria `C:\\x`.
                if (base.length() > 0 && base.charAt(base.length() - 1) == separatorChar) {
                    return base + cola.substring(1);
                }
                return base + cola;
            }
            if (actual.equals(separator) || actual.length() == 0) {
                return abs;
            }
        }
    }

    /** El camino canonico, como {@code File}. */
    public File getCanonicalFile() throws IOException {
        return new File(this.getCanonicalPath());
    }

    /**
     * Le saca a una ruta de Windows el prefijo "verbatim" (`\\?\`) con el que vuelve del sistema.
     *
     * <p>El nativo devuelve la forma extendida porque es la que usa el sistema por dentro; el JDK
     * devuelve `C:\x` y no `\\?\C:\x`, y quien compare el resultado de `getCanonicalPath()` con una
     * ruta escrita a mano espera la corta. `\\?\UNC\servidor\share` vuelve a ser
     * `\\servidor\share`, que es su forma normal.
     */
    private static String despojarVerbatim(String p) {
        if (p == null) {
            return null;
        }
        if (p.startsWith("\\\\?\\UNC\\")) {
            return "\\\\" + p.substring(8);
        }
        if (p.startsWith("\\\\?\\")) {
            return p.substring(4);
        }
        return p;
    }

    /**
     * Resuelve `.` y `..` sobre el texto de una ruta ya absoluta.
     *
     * <p>Es lo que se le pasa al nativo. Hacerlo antes importa porque `..` se resuelve **sobre los
     * nombres** y no sobre los enlaces: si el nativo falla --el archivo no existe-- la ruta que
     * queda para el camino de respaldo ya esta limpia.
     */
    private static String normalizarPuntos(String p) {
        // Se parte a mano y no con `split`: `split` es una expresion regular, y meter
        // `java.util.regex` en `java.io.File` --que es de las primeras clases que se cargan-- por
        // separar en dos caracteres seria pagar media biblioteca por un `indexOf`.
        String[] partes = new String[contarPartes(p)];
        int cuantas = 0;
        int desde = 0;
        for (int k = 0; k <= p.length(); k++) {
            if (k == p.length() || p.charAt(k) == '\\' || p.charAt(k) == '/') {
                partes[cuantas] = p.substring(desde, k);
                cuantas = cuantas + 1;
                desde = k + 1;
            }
        }
        String[] pila = new String[partes.length];
        int n = 0;
        for (int i = 0; i < partes.length; i++) {
            String s = partes[i];
            if (s.equals(".") || (s.length() == 0 && i != 0)) {
                continue;
            }
            // Un `..` al principio de una ruta absoluta no tiene a donde subir: se descarta, que es
            // lo que hace el sistema con `C:\..`.
            if (s.equals("..")) {
                if (n > 1) {
                    n = n - 1;
                }
                continue;
            }
            pila[n] = s;
            n = n + 1;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(separatorChar);
            }
            sb.append(pila[i]);
        }
        String r = sb.toString();
        return r.length() == 0 ? separator : r;
    }

    /** Cuantos tramos deja partir por separadores. Se cuenta antes para dimensionar el arreglo. */
    private static int contarPartes(String p) {
        int n = 1;
        for (int i = 0; i < p.length(); i++) {
            if (p.charAt(i) == '\\' || p.charAt(i) == '/') {
                n = n + 1;
            }
        }
        return n;
    }

    // ---- URI / URL ----

    /** A {@code file:} URI for this abstract path. */
    public URI toURI() {
        // La ruta se le pasa EN CRUDO al constructor por partes de `URI`, y es el quien la escapa.
        //
        // Armar el texto a mano y pasarlo por `URI.create` --que es lo que hacia antes-- no puede
        // andar: el constructor de un solo `String` espera un URI **ya escapado**, asi que un nombre
        // de archivo con un espacio producia `file:/a b/c`, que no es un URI valido, y `getRawPath()`
        // devolvia el espacio sin codificar. El constructor por partes existe justamente para esto.
        try {
            return new URI("file", null, barrear(this.getAbsolutePath(), this.isDirectory()), null);
        } catch (URISyntaxException imposible) {
            // Una ruta absoluta ya escapada siempre es un URI valido; si no lo fuera seria un
            // defecto de esta clase y no algo que el que llama pueda manejar.
            throw new Error(imposible);
        }
    }

    // La ruta con las barras del URI: separador de este sistema a '/', barra inicial si falta, y
    // barra final para un directorio -- eso ultimo es lo que hace que `resolve` contra el URI de un
    // directorio agregue al directorio en vez de reemplazar su ultimo segmento.
    private static String barrear(String p, boolean esDirectorio) {
        StringBuilder sb = new StringBuilder();
        if (p.length() == 0 || p.charAt(0) != separatorChar) {
            sb.append('/');
        }
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            sb.append(c == separatorChar ? '/' : c);
            i = i + 1;
        }
        if (esDirectorio && (sb.length() == 0 || sb.charAt(sb.length() - 1) != '/')) {
            sb.append('/');
        }
        return sb.toString();
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
     * Cuando se modifico por ultima vez, en milisegundos desde la epoca; `0L` si no se pudo saber.
     *
     * <p>El cero del contrato es ambiguo a proposito y hay que conocerlo: significa "no existe o
     * fallo la consulta", **y tambien** es la fecha valida del 1 de enero de 1970. Por eso el nativo
     * no usa cero como centinela sino `Long.MIN_VALUE`, y la traduccion a cero se hace aca, en el
     * unico lugar donde el contrato la obliga. Quien necesite distinguir los dos casos tiene
     * {@link #exists()}.
     */
    public long lastModified() {
        long t = Fs.mtime(this.path);
        return t == Long.MIN_VALUE ? 0L : t;
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

    /**
     * Los nombres **simples** de lo que hay en este directorio, o `null`.
     *
     * <p>`null` no es un caso de error accidental: el contrato dice que se devuelve cuando esto no es
     * un directorio o hubo una falla de E/S, y es lo que distingue "no pude mirar" de "mire y esta
     * vacio" --que es un arreglo de largo cero--. Perder esa distincion convertiria un error en un
     * resultado, y quien recorra el arbol nunca se enteraria de que le falto una rama.
     *
     * <p>Los nombres son simples, sin la ruta de este directorio adelante. {@link #listFiles()} es la
     * variante que la agrega.
     *
     * <p>El orden es el que da el sistema de archivos: el contrato **no garantiza ninguno**, y
     * ordenarlo aca haria que alguien se apoyara en uno que otra plataforma no le va a dar.
     */
    public String[] list() {
        return Fs.list(this.path);
    }

    /** Idem, quedandose solo con los nombres que el filtro acepta. */
    public String[] list(FilenameFilter filter) {
        String[] todos = this.list();
        if (todos == null) {
            return null;
        }
        if (filter == null) {
            return todos;
        }
        // Se cuenta y despues se copia, en vez de usar una lista: es el mismo recorrido dos veces a
        // cambio de no depender de `java.util` desde `java.io`, que se carga antes.
        int n = 0;
        for (String nombre : todos) {
            if (filter.accept(this, nombre)) {
                n = n + 1;
            }
        }
        String[] out = new String[n];
        int k = 0;
        for (String nombre : todos) {
            if (filter.accept(this, nombre)) {
                out[k] = nombre;
                k = k + 1;
            }
        }
        return out;
    }

    /**
     * Lo que hay en este directorio, como `File`, o `null`.
     *
     * <p>Cada uno se arma con **este** archivo como padre, asi que las rutas quedan completas y
     * relativas o absolutas segun lo sea esta. Es la diferencia con {@link #list()}, que da nombres
     * pelados.
     */
    public File[] listFiles() {
        String[] nombres = this.list();
        if (nombres == null) {
            return null;
        }
        File[] out = new File[nombres.length];
        for (int i = 0; i < nombres.length; i++) {
            out[i] = new File(this, nombres[i]);
        }
        return out;
    }

    /**
     * Idem, filtrando por nombre.
     *
     * <p>El filtro recibe el **directorio y el nombre**, no el `File` armado: es la diferencia con
     * {@link #listFiles(FileFilter)}, y sirve para filtrar sin construir un objeto por entrada.
     */
    public File[] listFiles(FilenameFilter filter) {
        String[] nombres = this.list(filter);
        if (nombres == null) {
            return null;
        }
        File[] out = new File[nombres.length];
        for (int i = 0; i < nombres.length; i++) {
            out[i] = new File(this, nombres[i]);
        }
        return out;
    }

    /** Idem, filtrando por el `File` ya armado --que es lo que permite preguntarle si es directorio--. */
    public File[] listFiles(FileFilter filter) {
        File[] todos = this.listFiles();
        if (todos == null || filter == null) {
            return todos;
        }
        int n = 0;
        for (File f : todos) {
            if (filter.accept(f)) {
                n = n + 1;
            }
        }
        File[] out = new File[n];
        int k = 0;
        for (File f : todos) {
            if (filter.accept(f)) {
                out[k] = f;
                k = k + 1;
            }
        }
        return out;
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

    /**
     * Fija la fecha de ultima modificacion.
     *
     * @throws IllegalArgumentException si `time` es negativo -- el contrato lo pide, y es distinto
     *     de devolver `false`: un `false` dice "no se pudo", esto dice "no se puede pedir eso"
     */
    public boolean setLastModified(long time) {
        if (time < 0L) {
            throw new IllegalArgumentException("Negative time");
        }
        return Fs.setMtime(this.path, time);
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

    // Contador de nombres temporales. Empieza en la hora para que dos corridas seguidas del mismo
    // programa no arranquen por el mismo nombre y se pisen entre si.
    private static long semillaTemp = System.nanoTime();

    /**
     * Crea un archivo temporal **vacio** en `directory` (o en `java.io.tmpdir` si es nulo) y
     * devuelve el {@code File} que lo nombra.
     *
     * <p><strong>No es atomico, y en el JDK si.</strong> El JDK crea el archivo con la bandera
     * "fallar si existe" del sistema, una sola operacion; aca se pregunta si existe y despues se
     * crea, en dos. Entre las dos, otro proceso puede crear ese mismo nombre y esta llamada lo
     * pisaria con vacio. La ventana es minuscula --el nombre lleva un contador y la hora en
     * nanosegundos-- pero existe, y quien use esto como candado entre procesos se va a llevar una
     * sorpresa. Es la misma limitacion de {@link #createNewFile()}, y por la misma razon: el nativo
     * escribe el archivo entero, no lo abre con banderas.
     *
     * @throws IllegalArgumentException si `prefix` tiene menos de tres caracteres
     * @throws IOException si despues de varios intentos no se pudo crear ninguno
     */
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        if (prefix == null) {
            throw new NullPointerException();
        }
        if (prefix.length() < 3) {
            throw new IllegalArgumentException("Prefix string \"" + prefix
                    + "\" too short: length must be at least 3");
        }
        String sufijo = suffix == null ? ".tmp" : suffix;
        File dir = directory;
        if (dir == null) {
            String t = System.getProperty("java.io.tmpdir");
            if (t == null || t.length() == 0) {
                throw new IOException("no temporary directory");
            }
            dir = new File(t);
        }
        // Varios intentos y no uno: el nombre podria estar tomado. Un numero fijo de vueltas para
        // que un directorio que no se puede escribir termine en excepcion y no en un cuelgue.
        for (int intento = 0; intento < 1000; intento++) {
            semillaTemp = semillaTemp * 6364136223846793005L + 1442695040888963407L;
            long n = semillaTemp >>> 1;         // sin signo: el nombre no lleva un menos adelante
            File f = new File(dir, prefix + n + sufijo);
            if (f.exists()) {
                continue;
            }
            if (Fs.writeAllBytes(f.getPath(), new byte[0], false)) {
                return f;
            }
        }
        throw new IOException("Unable to create temporary file in " + dir.getPath());
    }

    /** Idem, en el directorio temporal del sistema. */
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
