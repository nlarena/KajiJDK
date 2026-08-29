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
// OMITIDO (salida (a), omitir el miembro):
//   - `StandardJavaFileManager getStandardFileManager(DiagnosticListener<? super JavaFileObject>,
//      Locale, Charset)` — java.nio.charset no existe en KajiLibrary (cero clases). Es el unico
//      lugar del paquete donde hace falta Charset; sin el, el metodo no se puede declarar sin
//      mentir sobre el tercer parametro.
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

    CompilationTask getTask(Writer out,
                            JavaFileManager fileManager,
                            DiagnosticListener<? super JavaFileObject> diagnosticListener,
                            Iterable<String> options,
                            Iterable<String> classes,
                            Iterable<? extends JavaFileObject> compilationUnits);
}
