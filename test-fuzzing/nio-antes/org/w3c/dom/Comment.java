package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Comment -- the contents of a `&lt;!-- ... --&gt;`.
 *
 * <p>It adds no members over `CharacterData` for the same reason `CDATASection` adds nothing over
 * `Text`: a comment is a string of characters and the only difference is in how it is written. The
 * text it keeps is the one **inside** the delimiters.
 */
public interface Comment extends CharacterData {
}
