package org.w3c.dom.html;

/**
 * A `<textarea>`. The same note on `defaultValue`/`value` as in {@link HTMLInputElement} holds.
 */
public interface HTMLTextAreaElement extends HTMLElement {

    /** The value the document states. */
    String getDefaultValue();

    /** It sets the value the document states. */
    void setDefaultValue(String defaultValue);

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The keyboard shortcut. */
    String getAccessKey();

    /** It sets the keyboard shortcut. */
    void setAccessKey(String accessKey);

    /** The `cols` attribute. */
    int getCols();

    /** It sets the `cols` attribute. */
    void setCols(int cols);

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** Whether it is read-only. */
    boolean getReadOnly();

    /** It sets whether it is read-only. */
    void setReadOnly(boolean readOnly);

    /** The number of visible text lines. */
    int getRows();

    /** It sets the number of visible text lines. */
    void setRows(int rows);

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

    /** It takes the focus away from it. */
    void blur();

    /** It gives it the focus. */
    void focus();

    /** It selects all of its content. */
    void select();
}
