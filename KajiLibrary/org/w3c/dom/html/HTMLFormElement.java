package org.w3c.dom.html;

/**
 * Un `<form>`.
 *
 * <p>`getElements` da los controles del formulario, no sus hijos: un `<input>` metido dentro de un
 * `<div>` que a su vez esta en el formulario aparece igual. La coleccion es viva.
 *
 * <p>`submit()` envia el formulario **sin disparar el evento `onsubmit`**, que es la diferencia
 * observable con apretar el boton. `reset()` si dispara `onreset`.
 */
public interface HTMLFormElement extends HTMLElement {

    /** Los controles, en una coleccion viva. */
    HTMLCollection getElements();

    /** La cantidad. */
    int getLength();

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** El atributo `acceptCharset`. */
    String getAcceptCharset();

    /** Fija el atributo `acceptCharset`. */
    void setAcceptCharset(String acceptCharset);

    /** El atributo `action`. */
    String getAction();

    /** Fija el atributo `action`. */
    void setAction(String action);

    /** El atributo `enctype`. */
    String getEnctype();

    /** Fija el atributo `enctype`. */
    void setEnctype(String enctype);

    /** El atributo `method`. */
    String getMethod();

    /** Fija el atributo `method`. */
    void setMethod(String method);

    /** El marco de destino. */
    String getTarget();

    /** Fija el marco de destino. */
    void setTarget(String target);

    /** Envia el formulario, **sin** disparar `onsubmit`. */
    void submit();

    /** Vuelve los controles a sus valores por omision y dispara `onreset`. */
    void reset();
}
