package javax.xml.transform.sax;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * KajiLibrary's javax.xml.transform.sax.TransformerHandler -- transforms what keeps arriving.
 *
 * <p>It is the piece that makes it possible to put a transformation <b>in the middle of a SAX
 * chain</b>: it receives the document as events and writes the result to the {@link Result} it was
 * given. With that, reading, transforming and writing happen at once and none of the intermediate
 * steps exists in memory.
 *
 * <p>It implements the <b>three</b> SAX input interfaces --content, lexical and DTD-- and that is
 * not excess of zeal: if it only received content, the original's comments and CDATA sections would
 * disappear from the output, and a stylesheet has every authority to decide what to do with them.
 * Receiving the DTD matters because of notations and unparsed entities, which are also part of the
 * document.
 *
 * <p>{@link #setResult} has to be called <b>before</b> the first event: without a destination there
 * is nowhere to write, and that is why it throws if the result is no good instead of waiting to
 * fail halfway.
 *
 * <p>{@link #getTransformer} returns the inner transformer, and it is there to set parameters and
 * output properties on it before starting -- not to transform something separately.
 */
public interface TransformerHandler extends ContentHandler, LexicalHandler, DTDHandler {

    /**
     * Where the result goes.
     *
     * <p>Before the first event; see the class note.
     *
     * @throws IllegalArgumentException if that destination does not suit this implementation
     */
    void setResult(Result result) throws IllegalArgumentException;

    /**
     * Where the document comes from.
     *
     * <p>Whatever is relative that appears during the transformation is resolved against this.
     */
    void setSystemId(String systemID);

    /** Ver {@link #setSystemId}. */
    String getSystemId();

    /** The inner transformer, to configure it. See the class note. */
    Transformer getTransformer();
}
