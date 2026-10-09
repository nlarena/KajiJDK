package javax.xml.crypto.dsig.spec;

import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec -- the stylesheet of an XSLT
 * transform.
 *
 * <p>XML-DSig's XSLT transform applies a stylesheet before signing, and this class carries it.
 *
 * <p>It is the most dangerous transform of the set and it is worth saying: validating a signature
 * that uses it means <b>running</b> a stylesheet written by whoever signed, with everything XSLT
 * can do --read documents, in some implementations call code--. The specification defines it and
 * the practice is not to accept it in signatures of unknown origin.
 */
public final class XSLTTransformParameterSpec implements TransformParameterSpec {

    /** The stylesheet. */
    private final XMLStructure stylesheet;

    /**
     * @param stylesheet the stylesheet
     * @throws NullPointerException if it is null
     */
    public XSLTTransformParameterSpec(XMLStructure stylesheet) {
        if (stylesheet == null) {
            throw new NullPointerException("stylesheet cannot be null");
        }
        this.stylesheet = stylesheet;
    }

    /** The stylesheet. */
    public XMLStructure getStylesheet() {
        return this.stylesheet;
    }
}
