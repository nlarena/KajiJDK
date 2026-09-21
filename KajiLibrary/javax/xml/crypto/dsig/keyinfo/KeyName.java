package javax.xml.crypto.dsig.keyinfo;

import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyName -- the key, named.
 *
 * <p>A string, and nothing more. It is the <b>right</b> way of using a {@link KeyInfo}: it does not
 * bring the key but a name that whoever validates looks up in their own store.
 *
 * <p>That inverts the trust relationship and that is why it works: whoever validates chooses the
 * key among the ones they already have, and the document only says which. A name that is not in the
 * store makes validation fail, which is exactly what is wanted.
 *
 * <p>The standard does not define the name's format: it can be an identifier, an email, a
 * distinguished name. The two parties have to agree on it outside.
 */
public interface KeyName extends XMLStructure {

    /** The key's name. */
    String getName();
}
