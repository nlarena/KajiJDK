package org.w3c.dom.html;

/**
 * Un `<textarea>`. Vale la misma nota de `defaultValue`/`value` que en
 * {@link HTMLInputElement}.
 */
public interface HTMLTextAreaElement extends HTMLElement {

    /** El valor que dice el documento. */
    String getDefaultValue();

    /** Fija el valor que dice el documento. */
    void setDefaultValue(String defaultValue);

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** El atajo de teclado. */
    String getAccessKey();

    /** Fija el atajo de teclado. */
    void setAccessKey(String accessKey);

    /** El atributo `cols`. */
    int getCols();

    /** Fija el atributo `cols`. */
    void setCols(int cols);

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** Si es de solo lectura. */
    boolean getReadOnly();

    /** Fija si es de solo lectura. */
    void setReadOnly(boolean readOnly);

    /** Las filas, en una coleccion viva. */
    int getRows();

    /** Fija las filas, en una coleccion viva. */
    void setRows(int rows);

    /** La posicion en el orden de tabulacion. */
    int getTabIndex();

    /** Fija la posicion en el orden de tabulacion. */
    void setTabIndex(int tabIndex);

    /** El tipo del control. */
    String getType();

    /** El valor actual. */
    String getValue();

    /** Fija el valor actual. */
    void setValue(String value);

    /** Le saca el foco. */
    void blur();

    /** Le da el foco. */
    void focus();

    /** Selecciona todo su contenido. */
    void select();
}
