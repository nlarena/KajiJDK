package javax.tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import javax.lang.model.SourceVersion;

// KajiLibrary's javax.tools.Tool — the common shape of a command-line tool invoked from
// inside a running VM: give it the three standard streams and an argv, get back an exit
// code. JavaCompiler and DocumentationTool are the two the platform ships.
public interface Tool {

    // The real JDK returns the name of the module providing the tool; without java.lang.Module the
    // honest default is the empty string. The signature is the real one.
    default String name() {
        return "";
    }

    int run(InputStream in, OutputStream out, OutputStream err, String... arguments);

    Set<SourceVersion> getSourceVersions();
}
