package javax.tools;

import java.io.IOException;
import java.io.Closeable;
import java.io.Flushable;
import java.util.Iterator;
import java.util.Set;

// KajiLibrary's javax.tools.JavaFileManager — the indirection that lets a compiler read and
// write "files" without knowing what a file is. Every lookup is (location, name, kind), so
// the same front end works over a directory tree, a jar, or a map held in memory.
//
// OMITIDOS — salida (a), omitir el miembro. Dos causas distintas:
//
// 1) Tipos que no existen en KajiLibrary:
//      - `ClassLoader getClassLoader(Location)`             -> no hay java.lang.ClassLoader.
//      - `<S> ServiceLoader<S> getServiceLoader(Location, Class<S>)` -> no hay ServiceLoader.
//
// 2) Defecto del compilador congelado: un tipo anidado declarado en OTRA unidad de
//    compilacion no se puede nombrar (ver el informe; `JavaFileObject.Kind` da error duro y
//    `import javax.tools.JavaFileObject.Kind` degrada a Object en silencio). Por eso caen
//    los cuatro miembros que mencionan Kind:
//      - `Iterable<JavaFileObject> list(Location, String, Set<JavaFileObject.Kind>, boolean)`
//      - `JavaFileObject getJavaFileForInput(Location, String, JavaFileObject.Kind)`
//      - `JavaFileObject getJavaFileForOutput(Location, String, JavaFileObject.Kind, FileObject)`
//      - `JavaFileObject getJavaFileForOutputForOriginatingFiles(Location, String, JavaFileObject.Kind, FileObject...)`
//    Se dejan los imports de Iterator y Set porque los demas miembros si los usan.
//
// `Location`, en cambio, se declara aca adentro: dentro de la misma unidad de compilacion el
// tipo anidado si resuelve.
public interface JavaFileManager extends Closeable, Flushable, OptionChecker {

    // Donde buscar, o donde dejar. El JDK la trata como token opaco: StandardLocation trae
    // las trece canonicas y un file manager puede inventar las suyas.
    public interface Location {

        String getName();

        boolean isOutputLocation();

        // El JDK real pregunta si el nombre contiene "MODULE"; sin String.contains en la
        // biblioteca, el default conservador es "no".
        default boolean isModuleOrientedLocation() {
            return false;
        }
    }

    /**
     * El cargador con el que correr las herramientas que viven en esa ubicacion.
     *
     * <p>Existe por los procesadores de anotaciones: son codigo del usuario que el compilador tiene
     * que **ejecutar**, y hay que cargarlo de algun lado sin mezclarlo con el classpath de lo que se
     * esta compilando.
     *
     * @return el cargador, o `null` si la ubicacion no lo admite
     */
    ClassLoader getClassLoader(Location location);

    /**
     * Todos los objetos de esa ubicacion y ese paquete, de las clases de archivo pedidas.
     *
     * <p>Es la operacion central del gestor: es como el compilador **descubre** que hay en un
     * paquete sin que nadie se lo enumere.
     *
     * @param recurse si tambien mirar los subpaquetes
     */
    Iterable<JavaFileObject> list(Location location, String packageName,
            Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException;

    /** El objeto de **entrada** de esa clase binaria, o `null` si no esta. */
    JavaFileObject getJavaFileForInput(Location location, String className,
            JavaFileObject.Kind kind) throws IOException;

    /**
     * El objeto de **salida** para esa clase binaria.
     *
     * <p>`sibling` es una pista, no un dato: el gestor puede usarla para poner la salida al lado de
     * la fuente que la origino. Puede ser `null`.
     */
    JavaFileObject getJavaFileForOutput(Location location, String className,
            JavaFileObject.Kind kind, FileObject sibling) throws IOException;

    /** Igual, con **todas** las fuentes que lo originan; la primera hace de `sibling`. */
    default JavaFileObject getJavaFileForOutputForOriginatingFiles(Location location,
            String className, JavaFileObject.Kind kind, FileObject... originatingFiles)
            throws IOException {
        FileObject sibling = null;
        if (originatingFiles != null && originatingFiles.length > 0) {
            sibling = originatingFiles[0];
        }
        return getJavaFileForOutput(location, className, kind, sibling);
    }

    /**
     * Los servicios de ese tipo que hay en esa ubicacion.
     *
     * <p>Es como el compilador encuentra los procesadores de anotaciones declarados por
     * `META-INF/services`. **Devuelve un cargador vacio**: esta biblioteca no lee ese directorio de
     * servicios, y un cargador vacio es exactamente lo que el JDK devuelve para una ubicacion que no
     * declara ninguno.
     */
    default <S> java.util.ServiceLoader<S> getServiceLoader(Location location, Class<S> service)
            throws IOException {
        return java.util.ServiceLoader.load(service, this.getClassLoader(location));
    }

    String inferBinaryName(Location location, JavaFileObject file);

    boolean isSameFile(FileObject a, FileObject b);

    boolean handleOption(String current, Iterator<String> remaining);

    boolean hasLocation(Location location);

    FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException;

    FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException;

    default FileObject getFileForOutputForOriginatingFiles(Location location, String packageName, String relativeName, FileObject... originatingFiles) throws IOException {
        FileObject sibling = null;
        if (originatingFiles != null && originatingFiles.length > 0) {
            sibling = originatingFiles[0];
        }
        return getFileForOutput(location, packageName, relativeName, sibling);
    }

    // Sin `throws IOException`, a proposito: el `java.io.Flushable` / `java.io.Closeable` de
    // KajiLibrary declara `void flush()` / `void close()` SIN excepcion (a diferencia del JDK
    // real), y el javac congelado rechaza ensanchar el throws de un metodo heredado (§8.4.8.3).
    // El descriptor es identico al del JDK; lo unico que falta es el atributo Exceptions.
    void flush();

    void close();

    default Location getLocationForModule(Location location, String moduleName) throws IOException {
        throw new UnsupportedOperationException();
    }

    default Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
        throw new UnsupportedOperationException();
    }

    default String inferModuleName(Location location) throws IOException {
        throw new UnsupportedOperationException();
    }

    default Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
        throw new UnsupportedOperationException();
    }

    default boolean contains(Location location, FileObject fo) throws IOException {
        throw new UnsupportedOperationException();
    }
}
