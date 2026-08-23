package javax.tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import javax.lang.model.SourceVersion;

// KajiLibrary's javax.tools.Tool — the common shape of a command-line tool invoked from
// inside a running VM: give it the three standard streams and an argv, get back an exit
// code. JavaCompiler and DocumentationTool are the two the platform ships.
public interface Tool {

    // El JDK real devuelve el nombre del modulo que provee la herramienta; sin java.lang.Module
    // el default honesto es la cadena vacia. La firma es la de verdad.
    default String name() {
        return "";
    }

    int run(InputStream in, OutputStream out, OutputStream err, String... arguments);

    Set<SourceVersion> getSourceVersions();
}
