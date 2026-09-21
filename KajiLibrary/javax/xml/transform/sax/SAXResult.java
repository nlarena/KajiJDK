package javax.xml.transform.sax;

import javax.xml.transform.Result;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * KajiLibrary's javax.xml.transform.sax.SAXResult -- the output comes out as events.
 *
 * <p>Instead of writing text or building a tree, the transformer keeps telling a {@link
 * ContentHandler} what it produces. It is the way to chain transformations without materializing
 * the intermediate step, and to consume a large output without having it all in memory.
 *
 * <h2>Why there is a second handler</h2>
 *
 * <p>{@link #setLexicalHandler} is optional and not a duplicate: {@code ContentHandler} has no
 * methods for <b>comments</b>, CDATA sections or entities. Without a lexical handler, all that goes
 * by without anybody noticing -- which is almost always fine, and which is a problem when what is
 * being built has to keep the original's comments.
 *
 * <p>The two are set separately even if the same object implements both interfaces: setting the
 * content one does <b>not</b> install the lexical one, and {@link #getLexicalHandler} keeps
 * returning null until somebody sets it. It is worth knowing because it invites the mistake of
 * expecting comments that never arrive. The JDK does the same -- the deduction, when there is one,
 * is made by the transformer and not by this class.
 */
public class SAXResult implements Result {

    /** With this a {@code TransformerFactory} is asked whether it accepts this destination. */
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXResult/feature";

    private ContentHandler handler;

    private LexicalHandler lexicalHandler;

    private String systemId;

    /** Empty, to be filled. */
    public SAXResult() {
    }

    /** With the handler that is going to receive the output. */
    public SAXResult(ContentHandler handler) {
        setHandler(handler);
    }

    /** Who receives the events of the output. */
    public void setHandler(ContentHandler handler) {
        this.handler = handler;
    }

    /** Ver {@link #setHandler}. */
    public ContentHandler getHandler() {
        return this.handler;
    }

    /** Who receives comments, CDATA and entities. See the class note. */
    public void setLexicalHandler(LexicalHandler handler) {
        this.lexicalHandler = handler;
    }

    /** See {@link #setLexicalHandler}; null if nobody set one. */
    public LexicalHandler getLexicalHandler() {
        return this.lexicalHandler;
    }

    /** Where the result comes from; informative. */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** Ver {@link #setSystemId}. */
    public String getSystemId() {
        return this.systemId;
    }
}
