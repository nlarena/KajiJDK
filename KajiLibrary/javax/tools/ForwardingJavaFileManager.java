package javax.tools;

import java.util.Iterator;
import java.util.Objects;

// KajiLibrary's javax.tools.ForwardingJavaFileManager<M> — the file manager you subclass when
// you want the standard one's behaviour everywhere except in one place. Intercepting
// getJavaFileForOutput here is the classic trick for compiling to memory.
//
// Most of this class used to be missing, for three reasons that have all since gone away.
//
// 1) Types that did not exist in KajiLibrary: `ClassLoader getClassLoader(Location)` and
//    `<S> ServiceLoader<S> getServiceLoader(Location, Class<S>)`.
//
// 2) A defect of the frozen compiler: a type nested in another compilation unit could not be named.
//    Here that struck TWICE and took away nearly the whole class, because `JavaFileManager.Location`
//    appears in the signature of almost every method and `JavaFileObject.Kind` in the Java-file ones.
//
// 3) And consequently the `implements JavaFileManager` clause was left out too. With those fourteen
//    methods impossible to declare, the frozen javac rejected the class ("it is not abstract and does
//    not implement `inferBinaryName` from `JavaFileManager`") — and that check does work for directly
//    implemented interfaces. The two ways out were to mark the class `abstract` (which the real JDK
//    does NOT: it is a `public class`) or to leave the superinterface out.
//
// The superinterface **is here**, and with it the delegations that mention `Location`. The earlier
// note left them out and preferred a declared absence to a phantom superinterface -- which was right
// at the time. It is no longer needed: the type can be named, and the class does what its name says,
// forward everything.
public class ForwardingJavaFileManager<M extends JavaFileManager> implements JavaFileManager {

    protected final M fileManager;

    protected ForwardingJavaFileManager(M fileManager) {
        this.fileManager = Objects.requireNonNull(fileManager);
    }

    public boolean isSameFile(FileObject a, FileObject b) {
        return this.fileManager.isSameFile(a, b);
    }

    // ---- the ones that mention `Location` --------------------------------------------------------
    //
    // They all forward without looking. That there are so many of them and that they are so dull is
    // the point of the class: it exists so that someone can change **one** and inherit the rest,
    // instead of reimplementing the whole manager.

    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return this.fileManager.getClassLoader(location);
    }

    public boolean hasLocation(JavaFileManager.Location location) {
        return this.fileManager.hasLocation(location);
    }

    public Iterable<JavaFileObject> list(JavaFileManager.Location location, String packageName,
            java.util.Set<JavaFileObject.Kind> kinds, boolean recurse) throws java.io.IOException {
        return this.fileManager.list(location, packageName, kinds, recurse);
    }

    public String inferBinaryName(JavaFileManager.Location location, JavaFileObject file) {
        return this.fileManager.inferBinaryName(location, file);
    }

    public JavaFileObject getJavaFileForInput(JavaFileManager.Location location, String className,
            JavaFileObject.Kind kind) throws java.io.IOException {
        return this.fileManager.getJavaFileForInput(location, className, kind);
    }

    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
            JavaFileObject.Kind kind, FileObject sibling) throws java.io.IOException {
        return this.fileManager.getJavaFileForOutput(location, className, kind, sibling);
    }

    public FileObject getFileForInput(JavaFileManager.Location location, String packageName,
            String relativeName) throws java.io.IOException {
        return this.fileManager.getFileForInput(location, packageName, relativeName);
    }

    public FileObject getFileForOutput(JavaFileManager.Location location, String packageName,
            String relativeName, FileObject sibling) throws java.io.IOException {
        return this.fileManager.getFileForOutput(location, packageName, relativeName, sibling);
    }

    public String inferModuleName(JavaFileManager.Location location) throws java.io.IOException {
        return this.fileManager.inferModuleName(location);
    }

    public Iterable<java.util.Set<JavaFileManager.Location>> listLocationsForModules(
            JavaFileManager.Location location) throws java.io.IOException {
        return this.fileManager.listLocationsForModules(location);
    }

    public boolean contains(JavaFileManager.Location location, FileObject fo)
            throws java.io.IOException {
        return this.fileManager.contains(location, fo);
    }

    public <S> java.util.ServiceLoader<S> getServiceLoader(JavaFileManager.Location location,
            Class<S> service) throws java.io.IOException {
        return this.fileManager.getServiceLoader(location, service);
    }

    public boolean handleOption(String current, Iterator<String> remaining) {
        return this.fileManager.handleOption(current, remaining);
    }

    public int isSupportedOption(String option) {
        return this.fileManager.isSupportedOption(option);
    }

    // Without `throws IOException`: JavaFileManager does not declare it either (KajiLibrary's
    // java.io.Flushable and java.io.Closeable do not have it).
    public void flush() {
        this.fileManager.flush();
    }

    public void close() {
        this.fileManager.close();
    }
}
