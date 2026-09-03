package javax.tools;

import java.io.Writer;
import java.util.Locale;
import java.util.concurrent.Callable;
import javax.annotation.processing.Processor;

// KajiLibrary's javax.tools.JavaCompiler — the compiler, as an object you can call from a
// running program instead of a process you spawn. getTask does not compile anything: it
// hands back a CompilationTask you configure and then call(), which is what makes annotation
// processors and a custom file manager attachable before the first source is read.
//
// `getStandardFileManager(...)` **esta**: la nota anterior lo omitia porque `java.nio.charset` no
// existia, y ahora existe entero.
public interface JavaCompiler extends Tool, OptionChecker {

    // El trabajo pendiente: ya sabe que compilar, todavia no empezo. Es Callable<Boolean>
    // porque el resultado de una compilacion es un si/no, y porque asi se puede mandar a un
    // ExecutorService sin envolverla.
    public interface CompilationTask extends Callable<Boolean> {

        void addModules(Iterable<String> moduleNames);

        void setProcessors(Iterable<? extends Processor> processors);

        void setLocale(Locale locale);

        Boolean call();
    }

    /**
     * El gestor de archivos estandar de esta herramienta.
     *
     * <p>Los tres argumentos son los tres canales por los que una herramienta habla con el mundo: a
     * donde van los diagnosticos, en que idioma, y con que codificacion se leen las fuentes. `null`
     * en cualquiera de ellos significa "lo que el sistema use por defecto".
     */
    StandardJavaFileManager getStandardFileManager(
            DiagnosticListener<? super JavaFileObject> diagnosticListener, Locale locale,
            java.nio.charset.Charset charset);

    CompilationTask getTask(Writer out,
                            JavaFileManager fileManager,
                            DiagnosticListener<? super JavaFileObject> diagnosticListener,
                            Iterable<String> options,
                            Iterable<String> classes,
                            Iterable<? extends JavaFileObject> compilationUnits);
}
