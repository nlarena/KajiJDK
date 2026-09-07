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
// Six members used to be missing here, for two different reasons.
//
// 1) Types that did not exist in KajiLibrary: `ClassLoader getClassLoader(Location)` and
//    `<S> ServiceLoader<S> getServiceLoader(Location, Class<S>)`.
//
// 2) A defect of the frozen compiler: a nested type declared in ANOTHER compilation unit could not be
//    named (see the report; `JavaFileObject.Kind` was a hard error and
//    `import javax.tools.JavaFileObject.Kind` silently degraded to Object). That took down the four
//    members mentioning Kind: `list`, `getJavaFileForInput`, `getJavaFileForOutput` and
//    `getJavaFileForOutputForOriginatingFiles`.
//
// Both causes are gone and all six are here. `Location` was always declarable inside this file: a
// nested type does resolve within its own compilation unit.
public interface JavaFileManager extends Closeable, Flushable, OptionChecker {

    // Where to look, or where to put things. The JDK treats it as an opaque token:
    // StandardLocation brings the thirteen canonical ones and a file manager may invent its own.
    public interface Location {

        String getName();

        boolean isOutputLocation();

        // The real JDK asks whether the name contains "MODULE"; without String.contains in the
        // library, the conservative default is "no".
        default boolean isModuleOrientedLocation() {
            return false;
        }
    }

    /**
     * The loader to run the tools living at that location with.
     *
     * <p>It exists because of annotation processors: they are user code the compiler has to
     * **execute**, and it has to be loaded from somewhere without mixing it with the classpath of
     * what is being compiled.
     *
     * @return the loader, or `null` if the location does not admit one
     */
    ClassLoader getClassLoader(Location location);

    /**
     * Every object at that location and in that package, of the requested file kinds.
     *
     * <p>It is the manager's central operation: it is how the compiler **discovers** what is in a
     * package without anyone enumerating it for it.
     *
     * @param recurse whether to look into the subpackages too
     */
    Iterable<JavaFileObject> list(Location location, String packageName,
            Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException;

    /** The **input** object for that binary class, or `null` if it is not there. */
    JavaFileObject getJavaFileForInput(Location location, String className,
            JavaFileObject.Kind kind) throws IOException;

    /**
     * The **output** object for that binary class.
     *
     * <p>`sibling` is a hint, not a fact: the manager may use it to put the output next to the source
     * that originated it. It may be `null`.
     */
    JavaFileObject getJavaFileForOutput(Location location, String className,
            JavaFileObject.Kind kind, FileObject sibling) throws IOException;

    /** The same, with **all** the sources that originate it; the first acts as `sibling`. */
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
     * The services of that type at that location.
     *
     * <p>It is how the compiler finds the annotation processors declared through
     * `META-INF/services`. **It returns an empty loader**: this library does not read that services
     * directory, and an empty loader is exactly what the JDK returns for a location that declares
     * none.
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

    // Without `throws IOException`, on purpose: KajiLibrary's `java.io.Flushable` /
    // `java.io.Closeable` declares `void flush()` / `void close()` WITHOUT the exception (unlike the
    // real JDK), and the frozen javac refuses to widen an inherited method's throws (§8.4.8.3). The
    // descriptor is identical to the JDK's; the only thing missing is the Exceptions attribute.
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
