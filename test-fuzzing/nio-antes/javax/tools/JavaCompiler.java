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
// `getStandardFileManager(...)` **is here**: the earlier note left it out because
// `java.nio.charset` did not exist, and now it exists in full.
public interface JavaCompiler extends Tool, OptionChecker {

    // The pending work: it already knows what to compile, it has not started yet. It is
    // Callable<Boolean> because the result of a compilation is a yes/no, and because that way it can
    // be handed to an ExecutorService without wrapping it.
    public interface CompilationTask extends Callable<Boolean> {

        void addModules(Iterable<String> moduleNames);

        void setProcessors(Iterable<? extends Processor> processors);

        void setLocale(Locale locale);

        Boolean call();
    }

    /**
     * This tool's standard file manager.
     *
     * <p>The three arguments are the three channels a tool talks to the world through: where the
     * diagnostics go, in which language, and with which encoding the sources are read. `null` in any
     * of them means "whatever the system uses by default".
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
