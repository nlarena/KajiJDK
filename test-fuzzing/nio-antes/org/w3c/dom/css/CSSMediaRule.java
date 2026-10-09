package org.w3c.dom.css;

import org.w3c.dom.DOMException;
import org.w3c.dom.stylesheets.MediaList;

/**
 * A `@media`: rules that only apply to certain media.
 *
 * <p>It is the only rule that contains others, and that is why it is the only one that has
 * `insertRule` and `deleteRule` besides the sheet.
 */
public interface CSSMediaRule extends CSSRule {

    /** The media the rules inside apply to. */
    MediaList getMedia();

    /** The rules inside, in a live list. */
    CSSRuleList getCssRules();

    /**
     * It inserts that rule at that position and returns the position where it ended up.
     *
     * @throws DOMException `HIERARCHY_REQUEST_ERR` if the rule cannot go inside a `@media` --an
     *     `@import` or a `@charset`, for example--; `INDEX_SIZE_ERR` if the index is out of range;
     *     `SYNTAX_ERR` if the text does not parse
     */
    int insertRule(String rule, int index) throws DOMException;

    /**
     * It deletes the rule at that position.
     *
     * @throws DOMException `INDEX_SIZE_ERR` if the index is out of range
     */
    void deleteRule(int index) throws DOMException;
}
