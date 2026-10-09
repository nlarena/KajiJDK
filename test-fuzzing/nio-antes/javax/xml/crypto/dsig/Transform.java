package javax.xml.crypto.dsig;

import java.io.OutputStream;
import java.security.spec.AlgorithmParameterSpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.Data;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.Transform -- one step of the path between the datum and its
 * digest.
 *
 * <p>Transforms are chained: the output of one is the input of the next, and what comes out of the
 * last is what is digested. Each one can turn nodes into nodes or nodes into bytes, and chaining
 * two that do not fit is the classic mistake when building a signature by hand.
 *
 * <p>{@link #ENVELOPED} is the one that almost always appears: it takes the signature's own element
 * out of the document before digesting it. Without it, signing a document that is going to contain
 * the signature is impossible -- the digest would include the signature that does not exist yet.
 *
 * <p>{@link #XSLT} and {@link #XPATH} are the dangerous ones: they run code or expressions chosen
 * by whoever signed. See {@code XSLTTransformParameterSpec}.
 *
 * <p>The overload {@link #transform(Data, XMLCryptoContext, OutputStream)} also writes to a stream.
 * It serves to see what came out of each step, which is how a chain that does not check out is
 * debugged.
 */
public interface Transform extends XMLStructure, AlgorithmMethod {

    /** Decodes base 64. */
    static final String BASE64 = "http://www.w3.org/2000/09/xmldsig#base64";

    /** Takes the signature element out. See the class note. */
    static final String ENVELOPED = "http://www.w3.org/2000/09/xmldsig#enveloped-signature";

    /** Selects nodes with an XPath expression, node by node. */
    static final String XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";

    /** Likewise, by subtrees: much faster. */
    static final String XPATH2 = "http://www.w3.org/2002/06/xmldsig-filter2";

    /** Applies a stylesheet. See the class note. */
    static final String XSLT = "http://www.w3.org/TR/1999/REC-xslt-19991116";

    /** The parameters of this transform, or null. */
    AlgorithmParameterSpec getParameterSpec();

    /**
     * Applies the transform.
     *
     * @throws TransformException if it cannot be applied to that data
     */
    Data transform(Data data, XMLCryptoContext context) throws TransformException;

    /** Likewise, also writing to a stream. See the class note. */
    Data transform(Data data, XMLCryptoContext context, OutputStream os) throws TransformException;
}
