package javax.tools;

import java.io.Writer;
import java.util.Locale;
import java.util.concurrent.Callable;

// KajiLibrary's javax.tools.DocumentationTool — javadoc, exposed the same way JavaCompiler
// exposes the compiler: a task you configure and then call(). It reuses the compiler's file
// manager and diagnostic plumbing wholesale, and adds only what documentation needs — a
// doclet class to run, and its own output/doclet/taglet/snippet locations.
//
// The two omissions the earlier note listed --`getStandardFileManager`, for want of
// `java.nio.charset`, and the nested enum's `implements JavaFileManager.Location` clause, for not
// being able to name a type nested in another unit-- no longer apply: both things exist.
public interface DocumentationTool extends Tool, OptionChecker {

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

    DocumentationTask getTask(Writer out,
                              JavaFileManager fileManager,
                              DiagnosticListener<? super JavaFileObject> diagnosticListener,
                              Class<?> docletClass,
                              Iterable<String> options,
                              Iterable<? extends JavaFileObject> compilationUnits);

    // The same as CompilationTask, for documentation: configurable until it is called.
    public interface DocumentationTask extends Callable<Boolean> {

        void addModules(Iterable<String> moduleNames);

        void setLocale(Locale locale);

        Boolean call();
    }

    // The four locations that only make sense while documenting.
    public enum Location implements JavaFileManager.Location {

        DOCUMENTATION_OUTPUT,
        DOCLET_PATH,
        TAGLET_PATH,
        SNIPPET_PATH;

        public String getName() {
            return name();
        }

        public boolean isOutputLocation() {
            return this == DOCUMENTATION_OUTPUT;
        }
    }
}
