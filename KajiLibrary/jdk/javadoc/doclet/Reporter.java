package jdk.javadoc.doclet;

import java.io.PrintWriter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import com.sun.source.util.DocTreePath;

/**
 * Where a plug-in reports problems, so that they come out like those of the tool.
 *
 * <h2>Why printing is not enough</h2>
 *
 * <p>Because a diagnostic has a place. Writing "@param missing" on the output is of use to nobody;
 * what is of use is that it comes out with the file and the line, in the same format as the other
 * messages, so that an editor can jump there. The overloads of {@code print} are told apart
 * precisely by how much precision there is available about the place.
 *
 * <p>It also counts: the tool knows how many errors there were and finishes accordingly. A plug-in
 * that prints on its own takes no part in that.
 *
 * <h2>The two outputs</h2>
 *
 * <p>{@link #getStandardWriter} is for what the plug-in produces; {@link #getDiagnosticWriter} for
 * what goes wrong. They are separated so that the output can be redirected without taking the
 * errors along with it.
 *
 * @since 9
 */
public interface Reporter {

    /**
     * A diagnostic with no place.
     *
     * @param kind the severity
     * @param msg the message
     */
    void print(Diagnostic.Kind kind, String msg);

    /**
     * A diagnostic located at a node of the documentation.
     *
     * @param kind the severity
     * @param path where, inside the comment
     * @param msg the message
     */
    void print(Diagnostic.Kind kind, DocTreePath path, String msg);

    /**
     * A diagnostic located at a range inside a node of the documentation.
     *
     * <p>The three positions are those of a diagnostic of the compiler: where what is pointed at
     * begins, where the character that is marked is, and where it ends. The middle one is not
     * redundant -- it is the one that decides where the arrow points when the range spans several
     * lines.
     *
     * <p>By default it discards the positions and delegates to the version without them: an
     * implementation that does not know how to use them should not lose the message because of
     * that.
     *
     * @param kind the severity
     * @param path where, inside the comment
     * @param start where it begins
     * @param pos where it points
     * @param end where it ends
     * @param msg the message
     */
    default void print(Diagnostic.Kind kind, DocTreePath path, int start, int pos, int end,
            String msg) {
        print(kind, path, msg);
    }

    /**
     * A diagnostic located at an element of the program.
     *
     * @param kind the severity
     * @param e the element
     * @param msg the message
     */
    void print(Diagnostic.Kind kind, Element e, String msg);

    /**
     * A diagnostic located at a range of any file.
     *
     * <p>It serves for what is not Java code: a resource file, a template. By default it loses the
     * place and emits only the message.
     *
     * @param kind the severity
     * @param file the file
     * @param start where it begins
     * @param pos where it points
     * @param end where it ends
     * @param msg the message
     */
    default void print(Diagnostic.Kind kind, FileObject file, int start, int pos, int end,
            String msg) {
        print(kind, msg);
    }

    /**
     * Where to write the normal output of the plug-in.
     *
     * @return the writer
     * @throws UnsupportedOperationException if this implementation does not offer it
     */
    default PrintWriter getStandardWriter() {
        throw new UnsupportedOperationException(
                "this Reporter does not expose the standard output");
    }

    /**
     * Where to write the diagnostics.
     *
     * @return the writer
     * @throws UnsupportedOperationException if this implementation does not offer it
     */
    default PrintWriter getDiagnosticWriter() {
        throw new UnsupportedOperationException(
                "this Reporter does not expose the diagnostics output");
    }
}
