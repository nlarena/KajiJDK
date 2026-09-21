package javax.xml.crypto.dsig.dom;

import java.security.Key;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLValidateContext;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dsig.dom.DOMValidateContext -- which signature to validate, over
 * DOM.
 *
 * <p>The mirror of {@link DOMSignContext}, and simpler: a single node, the {@code Signature}
 * element one wants to validate. There is no parent nor sibling because validating <b>does not
 * modify</b> the tree.
 *
 * <h2>Key or selector: here the difference matters</h2>
 *
 * <p>With a {@link Key} it validates against that key and only that one. With a {@link KeySelector}
 * the key is chosen by looking at the signature's own {@code KeyInfo}.
 *
 * <p>And there lies the risk to understand: a {@code KeyInfo} is written by whoever signed, who can
 * be anybody. A selector that trusts it validates any well-built signature, with the key the
 * attacker wants. The direct key, or a selector that consults a trust store of one's own, are the
 * two correct ways.
 *
 * <h2>The identifiers</h2>
 *
 * <p>If the signature has references of the form {@code #id}, those identifiers have to be
 * registered with {@code setIdAttributeNS} before validating; see {@link DOMCryptoContext}. It is
 * the part that most often makes a validation that should work fail.
 */
public class DOMValidateContext extends DOMCryptoContext implements XMLValidateContext {

    /** The signature element to validate. */
    private Node node;

    /**
     * Validates with whatever the selector chooses. See the class note on the risk.
     *
     * @throws NullPointerException if either of the two is null
     */
    public DOMValidateContext(KeySelector ks, Node node) {
        if (ks == null) {
            throw new NullPointerException("key selector is null");
        }
        if (node == null) {
            throw new NullPointerException("node is null");
        }
        this.node = node;
        setKeySelector(ks);
    }

    /**
     * Validates against that key and only that one.
     *
     * @throws NullPointerException if either of the two is null
     */
    public DOMValidateContext(Key validatingKey, Node node) {
        if (validatingKey == null) {
            throw new NullPointerException("validatingKey is null");
        }
        if (node == null) {
            throw new NullPointerException("node is null");
        }
        this.node = node;
        setKeySelector(KeySelector.singletonKeySelector(validatingKey));
    }

    /**
     * Changes the element to validate.
     *
     * @throws NullPointerException if it is null
     */
    public void setNode(Node node) {
        if (node == null) {
            throw new NullPointerException();
        }
        this.node = node;
    }

    /** The element to validate. */
    public Node getNode() {
        return this.node;
    }
}
