package javax.swing.text.html.parser;

import java.io.IOException;
import java.io.Reader;

import javax.swing.text.ChangedCharSetException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Un {@link Parser} que reenvia lo que encuentra a un
 * {@link HTMLEditorKit.ParserCallback}.
 *
 * <h2>Es un adaptador</h2>
 *
 * <p>El analizador avisa sobrescribiendo metodos protegidos; el resto de la biblioteca escucha por
 * una interfaz de devolucion. Esta clase es el puente entre las dos formas, y no hace nada mas.
 *
 * <p>La traduccion no es solo de nombres: el analizador habla de {@link Element}, que viene de la
 * DTD, y quien escucha habla de {@link javax.swing.text.html.HTML.Tag}, que es como se muestra. La
 * conversion la hace {@link TagElement}.
 */
public class DocumentParser extends Parser {

    private HTMLEditorKit.ParserCallback callback;
    private boolean ignoreCharSet = false;

    /** Un analizador que usa esa DTD. */
    public DocumentParser(DTD dtd) {
        super(dtd);
    }

    /** Analiza y reenvia todo a quien escucha. */
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
     * Una etiqueta sin cierre.
     *
     * @throws ChangedCharSetException si era un {@code <meta>} que cambia la codificacion y no se
     *     pidio ignorarla.
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
