package org.w3c.dom.stylesheets;

import org.w3c.dom.DOMException;

/**
 * The media a sheet or a rule applies to: `screen`, `print`, `all`.
 *
 * <p>It is **live** and ordered, and it can be seen in two ways that are the same: as the whole
 * text (`getMediaText`) or as an indexable list. Changing either of the two changes the other.
 *
 * <p>An **empty** list does not mean "no medium" but **all**, which is the opposite of what one
 * would expect of an empty list. It is what CSS 2 says for a sheet with no `media` attribute.
 */
public interface MediaList {

    /** The media as text, separated by commas. */
    String getMediaText();

    /**
     * It replaces the whole list with that text.
     *
     * @throws DOMException `SYNTAX_ERR` if the text cannot be parsed;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the list is read-only
     */
    void setMediaText(String mediaText) throws DOMException;

    /** How many media there are. */
    int getLength();

    /** The medium at that position, or null if the index is out of range. */
    String item(int index);

    /**
     * It removes that medium from the list.
     *
     * @throws DOMException `NOT_FOUND_ERR` if the medium is not there;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the list is read-only
     */
    void deleteMedium(String oldMedium) throws DOMException;

    /**
     * It appends that medium at the end. If it was already there, it moves to the end instead of
     * being duplicated.
     *
     * @throws DOMException `INVALID_CHARACTER_ERR` if the medium is not valid;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the list is read-only
     */
    void appendMedium(String newMedium) throws DOMException;
}
