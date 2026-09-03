package org.w3c.dom.html;

/**
 * Un `<param>` de un `<object>` o un `<applet>`.
 */
public interface HTMLParamElement extends HTMLElement {

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);

    /** El valor actual. */
    String getValue();

    /** Fija el valor actual. */
    void setValue(String value);

    /** El atributo `valueType`. */
    String getValueType();

    /** Fija el atributo `valueType`. */
    void setValueType(String valueType);
}
