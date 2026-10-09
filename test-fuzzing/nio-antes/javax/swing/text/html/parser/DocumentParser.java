package javax.swing.text.html.parser;

import java.io.IOException;
import java.io.Reader;

import javax.swing.text.ChangedCharSetException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTMLEditorKit;

/**
 * A {@link Parser} that forwards what it finds to an
 * {@link HTMLEditorKit.ParserCallback}.
 *
 * <h2>It is an adapter</h2>
 *
 * <p>The parser reports by overriding protected methods; the rest of the library listens through
 * a callback interface. This class is the bridge between the two forms, and does nothing else.
 *
 * <p>The translation is not only of names: the parser talks about {@link Element}, which comes
 * from the DTD, and whoever listens talks about {@link javax.swing.text.html.HTML.Tag}, which is
 * how it is shown. {@link TagElement} does the conversion.
 */
public class DocumentParser extends Parser {

    private HTMLEditorKit.ParserCallback callback;
    private boolean ignoreCharSet = false;

    /** A parser that uses that DTD. */
    public DocumentParser(DTD dtd) {
        super(dtd);
    }

    /** It parses and forwards everything to whoever listens. */
    public void parse(Reader in, HTMLEditorKit.ParserCallback callback, boolean ignoreCharSet)
            throws IOException {
        this.callback = callback;
        this.ignoreCharSet = ignoreCharSet;
        try {
            super.parse(in);
        } finally {
            this.callback = null;
        }
    }

    protected void handleStartTag(TagElement tag) {
        if (callback != null) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            a.addAttributes(getAttributes());
            if (tag.fictional()) {
                a.addAttribute(HTMLEditorKit.ParserCallback.IMPLIED, Boolean.TRUE);
            }
            callback.handleStartTag(tag.getHTMLTag(), a, getCurrentPos());
            flushAttributes();
        }
    }

    protected void handleComment(char[] text) {
        if (callback != null) {
            callback.handleComment(text, getCurrentPos());
        }
    }

    /**
     * A tag with no closing.
     *
     * @throws ChangedCharSetException if it was a {@code <meta>} that changes the encoding and
     *     ignoring it was not asked for.
     */
    protected void handleEmptyTag(TagElement tag) throws ChangedCharSetException {
        if (callback != null) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            a.addAttributes(getAttributes());
            callback.handleSimpleTag(tag.getHTMLTag(), a, getCurrentPos());
            flushAttributes();
        }
    }

    protected void handleEndTag(TagElement tag) {
        if (callback != null) {
            callback.handleEndTag(tag.getHTMLTag(), getCurrentPos());
        }
    }

    protected void handleText(char[] data) {
        if (callback != null) {
            callback.handleText(data, getCurrentPos());
        }
    }

    protected void handleError(int ln, String errorMsg) {
        if (callback != null) {
            callback.handleError(errorMsg, getCurrentPos());
        }
    }
}
