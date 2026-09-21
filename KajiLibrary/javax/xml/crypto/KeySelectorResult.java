package javax.xml.crypto;

import java.security.Key;

/**
 * KajiLibrary's javax.xml.crypto.KeySelectorResult -- the key a {@link KeySelector} chose.
 *
 * <p>A wrapper of a single key, and it looks superfluous until one sees what it is for: whoever
 * validates a signature needs to know <b>with which key</b> it was validated, not only whether it
 * validated. Without this, the result would be a boolean and the application could not decide
 * whether that key was one it trusts.
 *
 * <p>That is the central trap of XML-DSig and it is worth saying: a signature that validates only
 * shows that whoever holds <b>that</b> key produced it. If the key came from the document itself
 * --from its {@code KeyInfo}-- that shows nothing, because whoever wrote the document chose the
 * key. Comparing the key here against a trust list is the missing step, and the one that gets
 * forgotten.
 */
public interface KeySelectorResult {

    /** The chosen key. See the class note on why it has to be looked at. */
    Key getKey();
}
