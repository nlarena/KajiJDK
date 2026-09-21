package org.w3c.dom.stylesheets;

/**
 * The style sheets of a document, in the order in which they are declared.
 *
 * <p>It is **live**: adding a `<link>` to the document changes what `getLength` answers without
 * asking for the list again.
 */
public interface StyleSheetList {

    /** How many sheets there are. */
    int getLength();

    /** The sheet at that position, or null if the index is out of range. */
    StyleSheet item(int index);
}
