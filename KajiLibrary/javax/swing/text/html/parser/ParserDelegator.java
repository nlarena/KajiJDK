package javax.swing.text.html.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

import javax.swing.text.html.HTMLEditorKit;

/**
 * The parser used when nobody asks for another.
 *
 * <h2>What it adds over {@link DocumentParser}</h2>
 *
 * <p>The DTD. A {@code DocumentParser} needs one and does not know where to get it from; this one
 * builds the HTML 3.2 one once and shares it among all the program's parsers.
 *
 * <p>Sharing it is safe because, once built, the DTD does not change: parsing only consults it.
 * Building one per document would cost eighty elements and five hundred entities every time.
 *
 * <h2>Where the DTD comes from</h2>
 *
 * <p>From {@link Html32}, which has it written as data. The JDK reads it from a binary file in
 * its image; here there is no file to look up. See that class's note.
 */
public class ParserDelegator extends HTMLEditorKit.Parser implements Serializable {

    private static DTD dtd = null;

    /** Builds the usual DTD, if it was not there yet. */
    protected static synchronized void setDefaultDTD() {
        if (dtd == null) {
            dtd = createDTD(new DTD("html32"), "html32");
        }
    }

    /** Fills that DTD with HTML 3.2 and registers it under that name. */
    protected static DTD createDTD(DTD dtd, String name) {
        Html32.fill(dtd);
        DTD.putDTDHash(name, dtd);
        return dtd;
    }

    /** A parser ready to use. */
    public ParserDelegator() {
        setDefaultDTD();
    }

    /** It parses and forwards to whoever listens. */
    public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharSet)
            throws IOException {
        // A new one per call: the parser keeps the state of the document it is reading, and
                // sharing it between two readings at once would mix the two trees.
        new DocumentParser(dtd).parse(r, cb, ignoreCharSet);
    }
}
