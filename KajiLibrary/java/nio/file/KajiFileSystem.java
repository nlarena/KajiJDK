package java.nio.file;

import java.io.File;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

// El unico `FileSystem` de KajiJDK: el que hay detras de `Path.getFileSystem()` y de
// `FileSystems.getDefault()`.
//
// **Detras si hay un sistema de archivos de verdad** --los seis nativos de `jdk.internal.io.Fs`
// llegan al disco-- y por eso esta clase dice `isReadOnly() == false`. Lo que no hay es con que
// contestar tres preguntas *sobre* ese sistema de archivos: cuales son sus raices, cuales son sus
// volumenes, y quienes son sus usuarios. Las tres se resuelven distinto y a proposito:
//
//   - (`getRootDirectories()` devolvia vacio y `getFileStores()` levantaba. Los dos ya contestan:
//     `Fs.roots()` enumera las unidades y `Fs.diskTotal`/`diskUsable`/`diskUnallocated` dan el
//     espacio, asi que `KajiFileStore` puede existir sin inventar ningun cero.)
//   - `getUserPrincipalLookupService()` sigue levantando `UnsupportedOperationException`: ahi el
//     valor de retorno **es** el dato, y un servicio que invente principals afirmaria cosas falsas.
//   - `supportedFileAttributeViews()` devuelve vacio, y no `{"basic"}`: ver la nota del metodo.
//
// `newWatchService()` falla porque no hay nativo de vigilancia de directorios. `getPathMatcher()`
// **ya no falla**: la nota anterior decia que `glob:` y `regex:` se podian implementar --son
// comparaciones de cadenas, no tocan el disco-- y que era deuda y no un techo de la VM. Se pago.
// El `UnsupportedOperationException` queda para lo que la spec dice que es: una sintaxis que esta
// implementacion no conoce.
final class KajiFileSystem extends FileSystem {

    static final KajiFileSystem INSTANCE = new KajiFileSystem();

    private KajiFileSystem() {
    }

    public FileSystemProvider provider() {
        return KajiFileSystemProvider.INSTANCIA;
    }

    public void close() {
    }

    public boolean isOpen() {
        return true;
    }

    /**
     * `false`.
     *
     * <p>`Files.write`, `createDirectory`, `delete` y `move` funcionan sobre este sistema de
     * archivos, asi que decir `true` --"solo permite acceso de lectura"-- seria falso, y del tipo
     * que hace que un programa ni intente escribir.
     */
    public boolean isReadOnly() {
        return false;
    }

    public String getSeparator() {
        return File.separator;
    }

    /**
     * Las raices del sistema de archivos, una por unidad montada.
     *
     * <p>Devolvia una lista vacia mientras no hubiera con que enumerarlas. `Fs.roots()` existe, asi
     * que ahora son las de verdad -- y se preguntan en cada llamada, no se guardan: una unidad que
     * se conecta agrega una raiz.
     */
    public Iterable<Path> getRootDirectories() {
        List<Path> out = new ArrayList<Path>();
        String[] raices = jdk.internal.io.Fs.roots();
        for (int i = 0; i < raices.length; i++) {
            out.add(this.getPath(raices[i]));
        }
        return out;
    }

    // Un conjunto **vacio** y no `{"basic"}`: decir que se soporta la vista basica obligaria a que
    // `Files.getFileAttributeView(p, BasicFileAttributeView.class)` devolviera algo, y esa vista
    // tiene `setTimes` -- no hay nativo que escriba metadatos.
    //
    // Ojo con la asimetria, que es real y esta bien: los atributos basicos si se **leen**
    // (`Files.readAttributes` los saca de `stat` y `size`). Lo que no hay es la **vista**, que es un
    // objeto de lectura y escritura. Ver `Files.getFileAttributeView`.
    public Set<String> supportedFileAttributeViews() {
        return new HashSet<String>();
    }

    /**
     * Falla.
     *
     * <p>No hay nativo de estadisticas de volumen, asi que no hay con que construir un `FileStore`
     * -- ver la nota de esa clase. Devolver un iterable vacio diria "esta VM no tiene volumenes",
     * que es falso; fallar dice "no se puede saber", que es lo que pasa.
     */
    /**
     * Los volumenes del sistema de archivos: uno por raiz.
     *
     * <p>Levantaba `UnsupportedOperationException` mientras no hubiera con que responder por el
     * espacio de un volumen. Ahora lo hay, y esto devuelve un {@link FileStore} por cada raiz que
     * {@link #getRootDirectories} enumera -- que es lo mismo que hace el JDK, salvo que el suyo
     * ademas lista los montajes que no son raices, y `Fs` no sabe enumerarlos.
     */
    public Iterable<FileStore> getFileStores() {
        java.util.List<FileStore> out = new java.util.ArrayList<FileStore>();
        for (Path raiz : this.getRootDirectories()) {
            String ruta = raiz.toString();
            // Una raiz que no se puede leer se saltea en vez de romper la enumeracion entera: en
            // Windows hay letras de unidad sin medio adentro, y una disquetera vacia no tiene por
            // que impedir ver el resto de los volumenes.
            if (jdk.internal.io.Fs.diskTotal(ruta) >= 0L) {
                out.add(new KajiFileStore(ruta, KajiFileStore.nombreDeVolumen(ruta)));
            }
        }
        return out;
    }

    /**
     * Falla.
     *
     * <p>No hay nativo que consulte la base de usuarios del sistema, y un servicio que devolviera
     * un principal por cualquier nombre estaria inventando identidades.
     */
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("KajiJDK has no principal lookup service");
    }

    public Path getPath(String first, String... more) {
        return Path.of(first, more);
    }

    /**
     * Un comparador de caminos, por `glob:` o por `regex:`.
     *
     * <p>Los dos terminan en un {@link java.util.regex.Pattern} sobre `path.toString()`. La
     * diferencia es quien escribe la expresion: en `regex:` la escribe el llamador, y en `glob:` la
     * traduce {@link #globAExpresion}.
     *
     * @throws IllegalArgumentException si falta el `:` o el patron esta mal formado
     * @throws UnsupportedOperationException si la sintaxis no es ninguna de las dos
     * @throws java.util.regex.PatternSyntaxException si la expresion no compila
     */
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        int corte = syntaxAndPattern.indexOf(':');
        if (corte <= 0 || corte == syntaxAndPattern.length() - 1) {
            throw new IllegalArgumentException(syntaxAndPattern);
        }
        String sintaxis = syntaxAndPattern.substring(0, corte);
        String patron = syntaxAndPattern.substring(corte + 1);
        String expresion;
        if (sintaxis.equalsIgnoreCase("glob")) {
            expresion = globAExpresion(patron);
        } else if (sintaxis.equalsIgnoreCase("regex")) {
            expresion = patron;
        } else {
            throw new UnsupportedOperationException("sintaxis desconocida: " + sintaxis);
        }
        final java.util.regex.Pattern compilado = java.util.regex.Pattern.compile(expresion);
        return new PathMatcher() {
            public boolean matches(Path path) {
                // Contra el camino **entero** y no contra el nombre: es lo que la spec dice, y es lo
                // que hace que `**\/*.java` pueda distinguirse de `*.java`. Quien quiera comparar
                // solo el nombre le pasa `path.getFileName()`, que es lo que hace `Files`.
                return path != null && compilado.matcher(path.toString()).matches();
            }
        };
    }

    /**
     * Traduce un glob a una expresion regular.
     *
     * <p>La regla que define todo el resto: **`*` no cruza separadores y `**` si**. De ahi salen las
     * dos traducciones distintas, y de ahi que haya que mirar el caracter siguiente antes de decidir.
     *
     * <p>Lo demas es mecanico: `?` es un caracter que no es separador, `[...]` pasa casi tal cual
     * --con `!` en vez de `^` para negar--, `{a,b}` es una alternativa, y `\\` escapa. Todo caracter
     * que la expresion regular trate especial y el glob no se escapa.
     *
     * <p>En Windows los dos separadores valen, asi que "no es separador" es `[^\\/]` y no `[^/]`.
     */
    private static String globAExpresion(String glob) {
        StringBuilder re = new StringBuilder();
        // `\Q...\E` no se usa a proposito: hay que intercalar metacaracteres nuestros con texto del
        // usuario, y las citas anidadas se vuelven ilegibles enseguida. Se escapa caracter por
        // caracter, que es mas largo de escribir y mucho mas facil de leer.
        int i = 0;
        int llaves = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            i = i + 1;
            if (c == '\\') {
                if (i >= glob.length()) {
                    throw new java.util.regex.PatternSyntaxException(
                            "el patron termina en una barra de escape", glob, i - 1);
                }
                re.append(java.util.regex.Pattern.quote(String.valueOf(glob.charAt(i))));
                i = i + 1;
            } else if (c == '/') {
                re.append(SEPARADOR);
            } else if (c == '*') {
                if (i < glob.length() && glob.charAt(i) == '*') {
                    // `**` cruza separadores. Es la unica diferencia con `*`, y es toda la gracia.
                    re.append(".*");
                    i = i + 1;
                } else {
                    re.append(NO_SEPARADOR).append('*');
                }
            } else if (c == '?') {
                re.append(NO_SEPARADOR);
            } else if (c == '[') {
                i = claseDeCaracteres(glob, i, re);
            } else if (c == '{') {
                if (llaves > 0) {
                    throw new java.util.regex.PatternSyntaxException(
                            "los grupos de un glob no se anidan", glob, i - 1);
                }
                llaves = llaves + 1;
                re.append('(');
            } else if (c == ',' && llaves > 0) {
                re.append('|');
            } else if (c == '}') {
                if (llaves == 0) {
                    throw new java.util.regex.PatternSyntaxException(
                            "cierra un grupo que no abrio", glob, i - 1);
                }
                llaves = llaves - 1;
                re.append(')');
            } else {
                escapar(re, c);
            }
        }
        if (llaves > 0) {
            throw new java.util.regex.PatternSyntaxException(
                    "falta cerrar un grupo", glob, glob.length());
        }
        return re.toString();
    }

    // La clase `[...]`. Devuelve donde sigue el patron despues del `]`.
    private static int claseDeCaracteres(String glob, int desde, StringBuilder re) {
        int i = desde;
        re.append('[');
        if (i < glob.length() && (glob.charAt(i) == '!' || glob.charAt(i) == '^')) {
            // El glob niega con `!`; la expresion regular con `^`. Un `^` literal al principio de
            // una clase de glob **no** niega, pero escribirlo asi es tan raro que el JDK tampoco lo
            // distingue.
            re.append('^');
            i = i + 1;
        }
        boolean vacia = true;
        while (i < glob.length() && (glob.charAt(i) != ']' || vacia)) {
            char c = glob.charAt(i);
            i = i + 1;
            vacia = false;
            if (c == '\\' && i < glob.length()) {
                re.append('\\').append(glob.charAt(i));
                i = i + 1;
            } else if (c == '-' || c == ']') {
                re.append('\\').append(c);
            } else if (c == '[' || c == '&' || c == '^') {
                // `&&` es interseccion en una clase de Java y no significa nada en un glob.
                re.append('\\').append(c);
            } else {
                re.append(c);
            }
        }
        if (i >= glob.length()) {
            throw new java.util.regex.PatternSyntaxException(
                    "falta cerrar una clase de caracteres", glob, glob.length());
        }
        re.append(']');
        return i + 1;
    }

    private static void escapar(StringBuilder re, char c) {
        if ("\\.[]{}()*+-?^$|".indexOf(c) >= 0) {
            re.append('\\');
        }
        re.append(c);
    }

    // Que cuenta como separador. En Windows valen los dos, y por eso un glob escrito con `/` matchea
    // un camino que el sistema escribe con `\\`.
    private static final String SEPARADOR =
            java.io.File.separatorChar == '\\' ? "[\\\\/]" : "/";
    private static final String NO_SEPARADOR =
            java.io.File.separatorChar == '\\' ? "[^\\\\/]" : "[^/]";

    public WatchService newWatchService() {
        throw new UnsupportedOperationException("KajiJDK has no watch service");
    }
}
