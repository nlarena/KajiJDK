package javax.tools;

import java.util.Iterator;
import java.util.Objects;

// KajiLibrary's javax.tools.ForwardingJavaFileManager<M> — the file manager you subclass when
// you want the standard one's behaviour everywhere except in one place. Intercepting
// getJavaFileForOutput here is the classic trick for compiling to memory.
//
// OMITIDOS — salida (a), omitir el miembro. Tres causas:
//
// 1) Tipos que no existen en KajiLibrary:
//      - `ClassLoader getClassLoader(Location)`                       -> sin java.lang.ClassLoader.
//      - `<S> ServiceLoader<S> getServiceLoader(Location, Class<S>)`  -> sin java.util.ServiceLoader.
//
// 2) Defecto del compilador congelado: no se puede nombrar un tipo anidado declarado en otra
//    unidad de compilacion. Aca eso pega DOS veces y es lo que se lleva casi toda la clase,
//    porque `JavaFileManager.Location` aparece en la firma de casi todos los metodos y
//    `JavaFileObject.Kind` en los de Java files:
//      - list, inferBinaryName, hasLocation, getFileForInput, getFileForOutput,
//        getFileForOutputForOriginatingFiles, getJavaFileForInput, getJavaFileForOutput,
//        getJavaFileForOutputForOriginatingFiles, getLocationForModule (x2), inferModuleName,
//        listLocationsForModules, contains.
//
// 3) Y en consecuencia, la clausula `implements JavaFileManager` tambien esta OMITIDA. Con los
//    catorce metodos de arriba imposibles de declarar, el javac congelado rechaza la clase
//    ("no es abstracta y no implementa `inferBinaryName` de `JavaFileManager`") — y ese chequeo
//    si funciona para interfaces implementadas directamente. Las dos salidas eran marcar la
//    clase `abstract` (que el JDK real NO hace: es `public class`) u omitir la superinterfaz.
// La superinterfaz **esta**, y con ella las ocho delegaciones que mencionan `Location`. La nota
// anterior las omitia porque el javac no podia nombrar un tipo anidado de otra unidad, y prefería
// una ausencia declarada a una superinterfaz fantasma -- lo cual era correcto entonces. Ya no hace
// falta: se puede nombrar, y la clase hace lo que su nombre dice, reenviar todo.
public class ForwardingJavaFileManager<M extends JavaFileManager> implements JavaFileManager {

    protected final M fileManager;

    protected ForwardingJavaFileManager(M fileManager) {
        this.fileManager = Objects.requireNonNull(fileManager);
    }

    public boolean isSameFile(FileObject a, FileObject b) {
        return this.fileManager.isSameFile(a, b);
    }

    // ---- las ocho que mencionan `Location` -------------------------------------------------------
    //
    // Todas reenvian sin mirar. Que sean tantas y tan tontas es el punto de la clase: existe para que
    // alguien pueda cambiar **una** y heredar el resto, en vez de reimplementar el gestor entero.

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

    // Sin `throws IOException`: JavaFileManager tampoco lo declara (java.io.Flushable y
    // java.io.Closeable de KajiLibrary no lo tienen).
    public void flush() {
        this.fileManager.flush();
    }

    public void close() {
        this.fileManager.close();
    }
}
