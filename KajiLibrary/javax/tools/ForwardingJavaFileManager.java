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
//    Se omite la superinterfaz: una ausencia es un subconjunto, un `abstract` inventado seria
//    una declaracion falsa que el gate daria por buena. La relacion con JavaFileManager
//    sobrevive igual en la cota del parametro, `M extends JavaFileManager`.
//
// Quedan los cinco metodos cuya firma no menciona ningun tipo anidado — mas el campo y el
// constructor, que son la estructura misma de la clase.
public class ForwardingJavaFileManager<M extends JavaFileManager> {

    protected final M fileManager;

    protected ForwardingJavaFileManager(M fileManager) {
        this.fileManager = Objects.requireNonNull(fileManager);
    }

    public boolean isSameFile(FileObject a, FileObject b) {
        return this.fileManager.isSameFile(a, b);
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
