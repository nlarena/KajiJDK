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
//   - `StandardJavaFileManager getStandardFileManager(DiagnosticListener<? super JavaFileObject>,
//      Locale, Charset)` — sin java.nio.charset en KajiLibrary. Igual que en JavaCompiler.
//   - En el anidado `Location`, la clausula `implements JavaFileManager.Location`: el javac
//     congelado no puede nombrar un tipo anidado de otra unidad de compilacion, y con el import
//     la clausula se descarta en silencio. Los dos metodos del contrato quedan con su firma
//     exacta.
public interface DocumentationTool extends Tool, OptionChecker {

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
    public enum Location {

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
