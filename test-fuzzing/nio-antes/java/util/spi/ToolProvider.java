package java.util.spi;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * KajiLibrary's java.util.spi.ToolProvider -- a command-line tool, callable from code.
 *
 * <p>It is what lets {@code javac} or {@code jar} be run without launching a process: the tool is
 * looked up by name and runs in the same virtual machine, with its outputs redirected wherever one
 * wants. For a build or a test that changes a great deal -- no parsing a process's output and no
 * fighting with the console's encoding.
 *
 * <h2>Why the character version is the principal one</h2>
 *
 * <p>{@code run}'s two overloads do the same thing, and the {@code PrintStream} one <b>wraps</b> the
 * {@code PrintWriter} one, not the other way round. It has to be in that order: a
 * {@code PrintStream} writes bytes with an encoding already fixed, so if the tool wrote there
 * directly, a message with accents would come out wrong and there would be no way of fixing it from
 * outside. With characters, the caller chooses the encoding when constructing the writer.
 *
 * <p>The default flushes both writers in a {@code finally}: if the tool threw, what it managed to
 * write is exactly what is needed to know why.
 */
public interface ToolProvider {

    /** The name it is found by: {@code "javac"}, {@code "jar"}. */
    String name();

    /** A line of description, or empty if it has none. */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * It runs the tool.
     *
     * @return the exit code; 0 if it worked
     */
    int run(PrintWriter out, PrintWriter err, String... args);

    /** The same, with byte streams. See the class's note on why this one is the wrapper. */
    default int run(PrintStream out, PrintStream err, String... args) {
        if (out == null || err == null) {
            throw new NullPointerException();
        }
        PrintWriter outWriter = new PrintWriter(out, true);
        PrintWriter errWriter = new PrintWriter(err, true);
        try {
            return run(outWriter, errWriter, args);
        } finally {
            outWriter.flush();
            errWriter.flush();
        }
    }

    /**
     * The first tool by that name, looked up among the ones registered as a service.
     *
     * @return empty if there is none
     */
    static Optional<ToolProvider> findFirst(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        ServiceLoader<ToolProvider> loader = ServiceLoader.load(ToolProvider.class);
        Iterator<ToolProvider> it = loader.iterator();
        while (it.hasNext()) {
            ToolProvider p = it.next();
            if (name.equals(p.name())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
