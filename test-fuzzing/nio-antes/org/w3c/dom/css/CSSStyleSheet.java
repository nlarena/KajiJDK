package org.w3c.dom.css;

import org.w3c.dom.DOMException;
import org.w3c.dom.stylesheets.StyleSheet;

/**
 * A CSS style sheet: the part of {@link StyleSheet} that does know about rules.
 *
 * <p>`getOwnerRule` and `getOwnerNode` --the inherited one-- are mutually exclusive, as in
 * `StyleSheet`: an imported sheet has an owner rule and no node; a linked one, the other way round.
 *
 * <p>`insertRule` returns the position where the rule ended up, which is not always the index that
 * was asked for: the `@charset` and `@import` rules have to go before the others, and the
 * implementation may rearrange them.
 */
public interface CSSStyleSheet extends StyleSheet {

    /** The `@import` that brought this sheet, or null if it is linked from the document. */
    CSSRule getOwnerRule();

    /** The rules of the sheet, in a live list. */
    CSSRuleList getCssRules();

    /**
     * It inserts that rule at that position and returns where it ended up.
     *
     * @throws DOMException `HIERARCHY_REQUEST_ERR` if the rule cannot go there --an `@import` after
     *     a style rule, for example--; `INDEX_SIZE_ERR` if the index is out of range; `SYNTAX_ERR`
     *     if the text does not parse
     */
    int insertRule(String rule, int index) throws DOMException;

    /**
     * It deletes the rule at that position.
     *
     * @throws DOMException `INDEX_SIZE_ERR` if the index is out of range
     */
    void deleteRule(int index) throws DOMException;
}
