package org.w3c.dom.html;

/**
 * A `<button>`.
 */
public interface HTMLButtonElement extends HTMLElement {

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The keyboard shortcut. */
    String getAccessKey();

    /** It sets the keyboard shortcut. */
    void setAccessKey(String accessKey);

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The position in the tabbing order. */
    int getTabIndex();

    /** It sets the position in the tabbing order. */
    void setTabIndex(int tabIndex);

    /** The type of the control. */
    String getType();

    /** The current value. */
    String getValue();

    /** It sets the current value. */
    void setValue(String value);
}
