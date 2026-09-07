package javax.tools;

// KajiLibrary's javax.tools.StandardJavaFileManager — the file manager that actually reads
// and writes the host file system, and the one ToolProvider's compiler hands you by default.
// Everything it adds over JavaFileManager is a convenience for turning host file names into
// JavaFileObjects, plus per-location search-path configuration.
//
// The note that used to be here described this class as "the most amputated in the package", and
// rightly so: its reason for being is to translate between the host's file system and
// `JavaFileObject`s, and both ends were missing -- `java.io.File` and `java.nio.file.Path`. Both
// exist now, so the class says what it has to say.
//
// That there are **two** families of methods, one with `File` and one with `Path`, is not gratuitous
// duplication: `File` is the old API and `Path` the one that understands file systems other than the
// host's. The JDK keeps both and so does this class.
//
public interface StandardJavaFileManager extends JavaFileManager {

    boolean isSameFile(FileObject a, FileObject b);

    Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names);

    Iterable<? extends JavaFileObject> getJavaFileObjects(String... names);

    // ---- the `File` family -------------------------------------------------------------------------

    /** The objects for those files. */
    Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(
            Iterable<? extends java.io.File> files);

    /** The one above, with the convenience of varargs. */
    Iterable<? extends JavaFileObject> getJavaFileObjects(java.io.File... files);

    /**
     * Sets where to search for that location.
     *
     * @param path the directories, or `null` to go back to the default
     */
    void setLocation(JavaFileManager.Location location, Iterable<? extends java.io.File> path)
            throws java.io.IOException;

    /** Where that location is searched today, or `null` if it is not set. */
    Iterable<? extends java.io.File> getLocation(JavaFileManager.Location location);

    // ---- the `Path` family -------------------------------------------------------------------------

    /** The objects for those paths. */
    default Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(
            java.util.Collection<? extends java.nio.file.Path> paths) {
        java.util.ArrayList<java.io.File> files = new java.util.ArrayList<java.io.File>();
        for (java.nio.file.Path p : paths) {
            files.add(p.toFile());
        }
        return this.getJavaFileObjectsFromFiles(files);
    }

    /**
     * The one above with `Iterable`.
     *
     * @deprecated in the JDK in favour of the `Collection` version, because an `Iterable` may be
     *     traversable only once and this method does not promise how many times it traverses it.
     */
    default Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(
            Iterable<? extends java.nio.file.Path> paths) {
        java.util.ArrayList<java.nio.file.Path> copy =
                new java.util.ArrayList<java.nio.file.Path>();
        for (java.nio.file.Path p : paths) {
            copy.add(p);
        }
        return this.getJavaFileObjectsFromPaths(copy);
    }

    /** The objects for those paths, with varargs. */
    default Iterable<? extends JavaFileObject> getJavaFileObjects(java.nio.file.Path... paths) {
        return this.getJavaFileObjectsFromPaths(java.util.Arrays.asList(paths));
    }

    /** Where that location is searched today, as paths. */
    default Iterable<? extends java.nio.file.Path> getLocationAsPaths(
            JavaFileManager.Location location) {
        Iterable<? extends java.io.File> files = this.getLocation(location);
        if (files == null) {
            return null;
        }
        java.util.ArrayList<java.nio.file.Path> paths =
                new java.util.ArrayList<java.nio.file.Path>();
        for (java.io.File f : files) {
            paths.add(f.toPath());
        }
        return paths;
    }

    /** Sets where to search for that location, with paths. */
    default void setLocationFromPaths(JavaFileManager.Location location,
            java.util.Collection<? extends java.nio.file.Path> paths) throws java.io.IOException {
        if (paths == null) {
            this.setLocation(location, null);
            return;
        }
        java.util.ArrayList<java.io.File> files = new java.util.ArrayList<java.io.File>();
        for (java.nio.file.Path p : paths) {
            files.add(p.toFile());
        }
        this.setLocation(location, files);
    }

    /** That object's path, if it has one. */
    default java.nio.file.Path asPath(FileObject file) {
        throw new UnsupportedOperationException();
    }

    /** Sets where to search for **one module** inside a module-oriented location. */
    default void setLocationForModule(JavaFileManager.Location location, String moduleName,
            java.util.Collection<? extends java.nio.file.Path> paths) throws java.io.IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * How a `Path` is built out of a string.
     *
     * <p>It is a one-operation hook, and it exists so that a tool can direct every path to a file
     * system other than the host's -- a mounted ZIP, an in-memory one -- without the manager having
     * to know.
     */
    interface PathFactory {

        /** The path formed by that first segment and the ones that follow. */
        java.nio.file.Path getPath(String first, String... more);
    }

    /**
     * Changes the path factory.
     *
     * <p>By default there is nothing to change --the host's file system is used-- so the default
     * implementation refuses instead of accepting a factory it would then ignore.
     */
    default void setPathFactory(PathFactory f) {
        throw new UnsupportedOperationException();
    }
}
