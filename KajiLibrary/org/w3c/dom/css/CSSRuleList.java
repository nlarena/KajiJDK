package org.w3c.dom.css;

/**
 * The rules of a sheet or of a `@media`, in the order of the document.
 *
 * <p>It is **live**: inserting a rule changes what `getLength` answers without asking for the list
 * again.
 */
public interface CSSRuleList {

    /** How many rules there are. */
    int getLength();

    /** The rule at that position, or null if the index is out of range. */
    CSSRule item(int index);
}
