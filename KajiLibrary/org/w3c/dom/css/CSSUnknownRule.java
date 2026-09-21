package org.w3c.dom.css;

/**
 * An `@` rule this implementation does not recognise.
 *
 * <p>It adds no member, and that is what it says: of an unknown rule the text is kept --which is in
 * `getCssText`, inherited-- and nothing else. Existing as a type of its own is what allows it to be
 * kept in the sheet instead of discarded.
 */
public interface CSSUnknownRule extends CSSRule {
}
