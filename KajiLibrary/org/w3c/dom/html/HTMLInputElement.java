package org.w3c.dom.html;

/**
 * Un `<input>`, de cualquiera de sus tipos.
 *
 * <p>La distincion que hay que tener presente es `defaultValue`/`value` (y `defaultChecked`/
 * `checked`): el primero es lo que dice el documento y el segundo lo que el control tiene ahora.
 * Un `reset()` del formulario devuelve el segundo al primero.
 */
public interface HTMLInputElement extends HTMLElement {

    /** El valor que dice el documento. */
    String getDefaultValue();

    /** Fija el valor que dice el documento. */
    void setDefaultValue(String defaultValue);

    /** Si el documento lo marca. */
    boolean getDefaultChecked();

    /** Fija si el documento lo marca. */
    void setDefaultChecked(boolean defaultChecked);

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** El atributo `accept`. */
    String getAccept();

    /** Fija el atributo `accept`. */
    void setAccept(String accept);

    /** El atajo de teclado. */
    String getAccessKey();

    /** Fija el atajo de teclado. */
    void setAccessKey(String accessKey);

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El texto alternativo. */
    String getAlt();

    /** Fija el texto alternativo. */
    void setAlt(String alt);

    /** Si esta marcado ahora. */
    boolean getChecked();

    /** Fija si esta marcado ahora. */
    void setChecked(boolean checked);

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** El atributo `maxLength`. */
    int getMaxLength();

    /** Fija el atributo `maxLength`. */
    void setMaxLength(int maxLength);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** Si es de solo lectura. */
    boolean getReadOnly();

    /** Fija si es de solo lectura. */
    void setReadOnly(boolean readOnly);

    /** El tamanio visible. */
    String getSize();

    /** Fija el tamanio visible. */
    void setSize(String size);

    /** El origen. */
    String getSrc();

    /** Fija el origen. */
    void setSrc(String src);

    /** La posicion en el orden de tabulacion. */
    int getTabIndex();

    /** Fija la posicion en el orden de tabulacion. */
    void setTabIndex(int tabIndex);

    /** El tipo del control. */
    String getType();

    /** El atributo `useMap`. */
    String getUseMap();

    /** Fija el atributo `useMap`. */
    void setUseMap(String useMap);

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

    /** Simula un clic. */
    void click();
}
