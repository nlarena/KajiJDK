package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * A `<select>`.
 *
 * <p>`add(element, before)` inserts an option; with `before` at null it appends at the end. If
 * `before` is not a child of this `select`, it is `NOT_FOUND_ERR` -- that is why the method
 * declares {@link org.w3c.dom.DOMException} and the others of this class do not.
 *
 * <p>`getType` answers `"select-one"` or `"select-multiple"` according to `multiple`, which is what
 * the DOM defines even though the element has no `type` attribute.
 */
public interface HTMLSelectElement extends HTMLElement {

    /** The type of the control. */
    String getType();

    /** The position of the chosen option, or -1 if there is none. */
    int getSelectedIndex();

    /** It sets the position of the chosen option, or -1 if there is none. */
    void setSelectedIndex(int selectedIndex);

    /** The current value. */
    String getValue();

    /** It sets the current value. */
    void setValue(String value);

    /** The count. */
    int getLength();

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The options, in a live collection. */
    HTMLCollection getOptions();

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** Whether it admits several selections. */
    boolean getMultiple();

    /** It sets whether it admits several selections. */
    void setMultiple(boolean multiple);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The visible size. */
    int getSize();

    /** It sets the visible size. */
    void setSize(int size);

    /** The position in the tabbing order. */
    int getTabIndex();

    /** It sets the position in the tabbing order. */
    void setTabIndex(int tabIndex);

    /**
     * It adds that option before `before`, or at the end if `before` is null.
     *
     * @throws DOMException `NOT_FOUND_ERR` if `before` is not a child of this element
     */
    void add(HTMLElement element, HTMLElement before) throws org.w3c.dom.DOMException;

    /** It removes the option at that position. An index out of range does nothing. */
    void remove(int index);

    /** It takes the focus away from it. */
    void blur();

    /** It gives it the focus. */
    void focus();
}
