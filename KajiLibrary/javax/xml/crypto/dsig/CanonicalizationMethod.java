package javax.xml.crypto.dsig;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.CanonicalizationMethod -- how XML is turned into bytes.
 *
 * <p>It is the transform that makes signing XML possible. The same document can be written in many
 * ways --attribute order, quotes, spaces, prefixes-- and they all mean the same; a cryptographic
 * digest, on the other hand, changes with every byte. Canonicalization chooses <b>one</b> way of
 * writing, and that is the one that is digested.
 *
 * <h2>Inclusive against exclusive</h2>
 *
 * <p>The <b>inclusive</b> one drags along all the namespace declarations of the context, even if
 * the fragment does not use them. The <b>exclusive</b> one drags only the ones it uses.
 *
 * <p>The difference decides whether a signed fragment survives being moved. With the inclusive one,
 * putting the fragment in another document changes its context and breaks the signature; with the
 * exclusive one, it does not. That is why everything that travels inside an envelope --SOAP, above
 * all-- uses exclusive.
 *
 * <p>The {@code WITH_COMMENTS} variants keep the comments. They are almost never wanted: a comment
 * does not change the meaning of the document and signing it makes the signature break over an
 * irrelevant change.
 */
public interface CanonicalizationMethod extends Transform {

    /** Inclusive canonicalization, version 1.0. */
    static final String INCLUSIVE = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    /** Likewise, keeping comments. */
    static final String INCLUSIVE_WITH_COMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments";

    /** Exclusive. See the class note. */
    static final String EXCLUSIVE = "http://www.w3.org/2001/10/xml-exc-c14n#";

    /** Likewise, keeping comments. */
    static final String EXCLUSIVE_WITH_COMMENTS = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    /** Inclusive, version 1.1. */
    static final String INCLUSIVE_11 = "http://www.w3.org/2006/12/xml-c14n11";

    /** Likewise, keeping comments. */
    static final String INCLUSIVE_11_WITH_COMMENTS = "http://www.w3.org/2006/12/xml-c14n11#WithComments";

    /** The parameters, or null. For the exclusive one, an {@code ExcC14NParameterSpec}. */
    AlgorithmParameterSpec getParameterSpec();
}
