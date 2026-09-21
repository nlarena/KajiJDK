package javax.xml.crypto.dsig.dom;

import java.security.Key;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLSignContext;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dsig.dom.DOMSignContext -- where and with what to sign, over DOM.
 *
 * <p>It carries two things: the key, and <b>where in the tree</b> to write the signature element.
 *
 * <h2>Parent and next sibling</h2>
 *
 * <p>The parent is mandatory and says under which element the signature hangs. The next sibling is
 * optional and decides the exact position: the signature is inserted <b>before</b> it, and without
 * it it is appended at the end.
 *
 * <p>It matters more than it seems. There are schemas that fix the order of the children, and a
 * signature appended at the end breaks schema validation even if the signature itself is correct.
 *
 * <h2>Key or selector</h2>
 *
 * <p>The constructors come in pairs. With a {@link Key} it signs with that one, full stop; with a
 * {@link KeySelector} the key is chosen during the operation, by looking at the {@code KeyInfo}.
 * For signing the direct key is the normal thing -- the selector makes more sense when validating.
 *
 * <h2>The tree is modified</h2>
 *
 * <p>Signing <b>inserts</b> the signature element in the document that was passed. It is not a
 * read-only operation, and the document has to be modifiable.
 */
public class DOMSignContext extends DOMCryptoContext implements XMLSignContext {

    /** Under which element the signature hangs. */
    private Node parent;

    /** Before which one to insert it, or null for the end. */
    private Node nextSibling;

    /**
     * Signs with that key, hanging from the end of that element.
     *
     * @throws NullPointerException if either of the two is null
     */
    public DOMSignContext(Key signingKey, Node parent) {
        if (signingKey == null) {
            throw new NullPointerException("signingKey cannot be null");
        }
        if (parent == null) {
            throw new NullPointerException("parent cannot be null");
        }
        this.parent = parent;
        setKeySelector(KeySelector.singletonKeySelector(signingKey));
    }

    /**
     * Likewise, inserting before that sibling. See the class note.
     *
     * @throws NullPointerException if the key, the parent or the sibling is null
     */
    public DOMSignContext(Key signingKey, Node parent, Node nextSibling) {
        this(signingKey, parent);
        if (nextSibling == null) {
            throw new NullPointerException("nextSibling cannot be null");
        }
        this.nextSibling = nextSibling;
    }

    /**
     * Signs with the key that selector chooses.
     *
     * @throws NullPointerException if either of the two is null
     */
    public DOMSignContext(KeySelector ks, Node parent) {
        if (ks == null) {
            throw new NullPointerException("key selector cannot be null");
        }
        if (parent == null) {
            throw new NullPointerException("parent cannot be null");
        }
        this.parent = parent;
        setKeySelector(ks);
    }

    /**
     * Likewise, inserting before that sibling.
     *
     * @throws NullPointerException if any of the three is null
     */
    public DOMSignContext(KeySelector ks, Node parent, Node nextSibling) {
        this(ks, parent);
        if (nextSibling == null) {
            throw new NullPointerException("nextSibling cannot be null");
        }
        this.nextSibling = nextSibling;
    }

    /**
     * Changes under which element it hangs.
     *
     * @throws NullPointerException if it is null; the parent is not optional
     */
    public void setParent(Node parent) {
        if (parent == null) {
            throw new NullPointerException("parent is null");
        }
        this.parent = parent;
    }

    /**
     * Changes before which one to insert it.
     *
     * <p>Null is valid here and means "at the end"; see the class note.
     */
    public void setNextSibling(Node nextSibling) {
        this.nextSibling = nextSibling;
    }

    /** Under which element it hangs. */
    public Node getParent() {
        return this.parent;
    }

    /** Before which one it is inserted, or null. */
    public Node getNextSibling() {
        return this.nextSibling;
    }
}
