package org.w3c.dom.html;

/**
 * A `<param>` of an `<object>` or an `<applet>`.
 */
public interface HTMLParamElement extends HTMLElement {

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);

    /** The current value. */
    String getValue();

    /** It sets the current value. */
    void setValue(String value);

    /** The `valueType` attribute. */
    String getValueType();

    /** It sets the `valueType` attribute. */
    void setValueType(String valueType);
}
