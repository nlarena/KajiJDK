package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * Un `<select>`.
 *
 * <p>`add(elemento, antesDe)` inserta una opcion; con `antesDe` en nulo agrega al final. Si
 * `antesDe` no es hijo de este `select`, es `NOT_FOUND_ERR` -- por eso el metodo declara
 * {@link org.w3c.dom.DOMException} y los demas de esta clase no.
 *
 * <p>`getType` contesta `"select-one"` o `"select-multiple"` segun `multiple`, que es lo que el
 * DOM define aunque el elemento no tenga atributo `type`.
 */
public interface HTMLSelectElement extends HTMLElement {

    /** El tipo del control. */
    String getType();

    /** La posicion de la opcion elegida, o -1 si no hay ninguna. */
    int getSelectedIndex();

    /** Fija la posicion de la opcion elegida, o -1 si no hay ninguna. */
    void setSelectedIndex(int selectedIndex);

    /** El valor actual. */
    String getValue();

    /** Fija el valor actual. */
    void setValue(String value);

    /** La cantidad. */
    int getLength();

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** Las opciones, en una coleccion viva. */
    HTMLCollection getOptions();

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** Si admite varias selecciones. */
    boolean getMultiple();

    /** Fija si admite varias selecciones. */
    void setMultiple(boolean multiple);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** El tamanio visible. */
    int getSize();

    /** Fija el tamanio visible. */
    void setSize(int size);

    /** La posicion en el orden de tabulacion. */
    int getTabIndex();

    /** Fija la posicion en el orden de tabulacion. */
    void setTabIndex(int tabIndex);

    /**
     * Agrega esa opcion antes de `before`, o al final si `before` es nulo.
     *
     * @throws DOMException `NOT_FOUND_ERR` si `before` no es hijo de este elemento
     */
    void add(HTMLElement element, HTMLElement before) throws org.w3c.dom.DOMException;

    /** Saca la opcion de esa posicion. Un indice fuera de rango no hace nada. */
    void remove(int index);

    /** Le saca el foco. */
    void blur();

    /** Le da el foco. */
    void focus();
}
