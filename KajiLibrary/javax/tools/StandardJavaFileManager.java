package javax.tools;

// KajiLibrary's javax.tools.StandardJavaFileManager — the file manager that actually reads
// and writes the host file system, and the one ToolProvider's compiler hands you by default.
// Everything it adds over JavaFileManager is a convenience for turning host file names into
// JavaFileObjects, plus per-location search-path configuration.
//
// La nota que estaba aca describia esta clase como "la mas amputada del paquete", y con razon: su
// razon de ser es traducir entre el sistema de archivos del host y los `JavaFileObject`, y faltaban
// las dos puntas -- `java.io.File` y `java.nio.file.Path`. Las dos existen ahora, asi que la clase
// dice lo que tiene que decir.
//
// Que haya **dos** familias de metodos, una con `File` y otra con `Path`, no es duplicacion
// gratuita: `File` es la API vieja y `Path` la que entiende de sistemas de archivos que no son el
// del host. El JDK mantiene las dos y esta clase tambien.
//
public interface StandardJavaFileManager extends JavaFileManager {

    boolean isSameFile(FileObject a, FileObject b);

    Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names);

    Iterable<? extends JavaFileObject> getJavaFileObjects(String... names);

    // ---- la familia `File` -------------------------------------------------------------------------

    /** Los objetos de esos archivos. */
    Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(
            Iterable<? extends java.io.File> files);

    /** El de arriba, con la comodidad de los varargs. */
    Iterable<? extends JavaFileObject> getJavaFileObjects(java.io.File... files);

    /**
     * Fija donde buscar para esa ubicacion.
     *
     * @param path los directorios, o `null` para volver al valor por defecto
     */
    void setLocation(JavaFileManager.Location location, Iterable<? extends java.io.File> path)
            throws java.io.IOException;

    /** Donde se busca hoy para esa ubicacion, o `null` si no esta fijada. */
    Iterable<? extends java.io.File> getLocation(JavaFileManager.Location location);

    // ---- la familia `Path` -------------------------------------------------------------------------

    /** Los objetos de esos caminos. */
    default Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(
            java.util.Collection<? extends java.nio.file.Path> paths) {
        java.util.ArrayList<java.io.File> archivos = new java.util.ArrayList<java.io.File>();
        for (java.nio.file.Path p : paths) {
            archivos.add(p.toFile());
        }
        return this.getJavaFileObjectsFromFiles(archivos);
    }

    /**
     * El de arriba con `Iterable`.
     *
     * @deprecated en el JDK a favor de la version con `Collection`, porque un `Iterable` se puede
     *     recorrer una sola vez y este metodo no promete cuantas veces lo recorre.
     */
    default Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(
            Iterable<? extends java.nio.file.Path> paths) {
        java.util.ArrayList<java.nio.file.Path> copia =
                new java.util.ArrayList<java.nio.file.Path>();
        for (java.nio.file.Path p : paths) {
            copia.add(p);
        }
        return this.getJavaFileObjectsFromPaths(copia);
    }

    /** Los objetos de esos caminos, con varargs. */
    default Iterable<? extends JavaFileObject> getJavaFileObjects(java.nio.file.Path... paths) {
        return this.getJavaFileObjectsFromPaths(java.util.Arrays.asList(paths));
    }

    /** Donde se busca hoy para esa ubicacion, como caminos. */
    default Iterable<? extends java.nio.file.Path> getLocationAsPaths(
            JavaFileManager.Location location) {
        Iterable<? extends java.io.File> archivos = this.getLocation(location);
        if (archivos == null) {
            return null;
        }
        java.util.ArrayList<java.nio.file.Path> caminos =
                new java.util.ArrayList<java.nio.file.Path>();
        for (java.io.File f : archivos) {
            caminos.add(f.toPath());
        }
        return caminos;
    }

    /** Fija donde buscar para esa ubicacion, con caminos. */
    default void setLocationFromPaths(JavaFileManager.Location location,
            java.util.Collection<? extends java.nio.file.Path> paths) throws java.io.IOException {
        if (paths == null) {
            this.setLocation(location, null);
            return;
        }
        java.util.ArrayList<java.io.File> archivos = new java.util.ArrayList<java.io.File>();
        for (java.nio.file.Path p : paths) {
            archivos.add(p.toFile());
        }
        this.setLocation(location, archivos);
    }

    /** El camino de ese objeto, si tiene uno. */
    default java.nio.file.Path asPath(FileObject file) {
        throw new UnsupportedOperationException();
    }

    /** Fija donde buscar para **un modulo** dentro de una ubicacion orientada a modulos. */
    default void setLocationForModule(JavaFileManager.Location location, String moduleName,
            java.util.Collection<? extends java.nio.file.Path> paths) throws java.io.IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Como se construye un `Path` a partir de una cadena.
     *
     * <p>Es un gancho de una sola operacion, y existe para que una herramienta pueda dirigir todas
     * las rutas a un sistema de archivos que no sea el del host -- un ZIP montado, uno en memoria--
     * sin que el gestor tenga que saberlo.
     */
    interface PathFactory {

        /** El camino que forman ese primer segmento y los que siguen. */
        java.nio.file.Path getPath(String first, String... more);
    }

    /**
     * Cambia la fabrica de caminos.
     *
     * <p>Por defecto no hay nada que cambiar --se usa el sistema de archivos del host-- asi que la
     * implementacion por defecto se niega en vez de aceptar una fabrica que despues ignoraria.
     */
    default void setPathFactory(PathFactory f) {
        throw new UnsupportedOperationException();
    }
}
