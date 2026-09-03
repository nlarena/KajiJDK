package javax.tools;

import java.io.Writer;
import java.util.Locale;
import java.util.concurrent.Callable;

// KajiLibrary's javax.tools.DocumentationTool — javadoc, exposed the same way JavaCompiler
// exposes the compiler: a task you configure and then call(). It reuses the compiler's file
// manager and diagnostic plumbing wholesale, and adds only what documentation needs — a
// doclet class to run, and its own output/doclet/taglet/snippet locations.
//
// OMITIDOS (salida (a), omitir el miembro):
// Las dos omisiones que la nota anterior listaba --`getStandardFileManager` por falta de
// `java.nio.charset`, y la clausula `implements JavaFileManager.Location` del anidado por no poder
// nombrar un tipo anidado de otra unidad-- ya no aplican: las dos cosas existen.
public interface DocumentationTool extends Tool, OptionChecker {

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

    DocumentationTask getTask(Writer out,
                              JavaFileManager fileManager,
                              DiagnosticListener<? super JavaFileObject> diagnosticListener,
                              Class<?> docletClass,
                              Iterable<String> options,
                              Iterable<? extends JavaFileObject> compilationUnits);

    // Lo mismo que CompilationTask, para documentacion: configurable hasta que se llama.
    public interface DocumentationTask extends Callable<Boolean> {

        void addModules(Iterable<String> moduleNames);

        void setLocale(Locale locale);

        Boolean call();
    }

    // Las cuatro ubicaciones que solo tienen sentido documentando.
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
