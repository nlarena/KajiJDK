package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.Data -- what is signed or transformed.
 *
 * <p>A <b>marker</b> interface, without methods. The two that matter are its two implementations:
 * {@link NodeSetData}, which is a set of nodes, and {@link OctetStreamData}, which is a stream of
 * bytes.
 *
 * <p>The division is not one of convenience: an XML-DSig transform receives one thing and returns
 * the other, and which is which determines whether the chain of transforms fits together. A
 * canonicalization turns nodes into bytes; an XPath one turns nodes into nodes. Chaining two that
 * do not fit is the commonest mistake when building a signature by hand, and the type makes it
 * visible.
 */
public interface Data {
}
