package org.w3c.dom.html;

/**
 * An `<input>`, of any of its types.
 *
 * <p>The distinction to keep in mind is `defaultValue`/`value` (and `defaultChecked`/`checked`):
 * the first is what the document says and the second what the control has now. A `reset()` of the
 * form returns the second to the first.
 */
public interface HTMLInputElement extends HTMLElement {

    /** The value the document states. */
    String getDefaultValue();

    /** It sets the value the document states. */
    void setDefaultValue(String defaultValue);

    /** Whether the document checks it. */
    boolean getDefaultChecked();

    /** It sets whether the document checks it. */
    void setDefaultChecked(boolean defaultChecked);

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The `accept` attribute. */
    String getAccept();

    /** It sets the `accept` attribute. */
    void setAccept(String accept);

    /** The keyboard shortcut. */
    String getAccessKey();

    /** It sets the keyboard shortcut. */
    void setAccessKey(String accessKey);

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The alternative text. */
    String getAlt();

    /** It sets the alternative text. */
    void setAlt(String alt);

    /** Whether it is checked now. */
    boolean getChecked();

    /** It sets whether it is checked now. */
    void setChecked(boolean checked);

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** The `maxLength` attribute. */
    int getMaxLength();

    /** It sets the `maxLength` attribute. */
    void setMaxLength(int maxLength);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** Whether it is read-only. */
    boolean getReadOnly();

    /** It sets whether it is read-only. */
    void setReadOnly(boolean readOnly);

    /** The visible size. */
    String getSize();

    /** It sets the visible size. */
    void setSize(String size);

    /** The source. */
    String getSrc();

    /** It sets the source. */
    void setSrc(String src);

    /** The position in the tabbing order. */
    int getTabIndex();

    /** It sets the position in the tabbing order. */
    void setTabIndex(int tabIndex);

    /** The type of the control. */
    String getType();

    /** The `useMap` attribute. */
    String getUseMap();

    /** It sets the `useMap` attribute. */
    void setUseMap(String useMap);

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

    /** It simulates a click. */
    void click();
}
