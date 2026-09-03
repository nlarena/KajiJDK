package org.w3c.dom.html;

/**
 * Un `<applet>`. Obsoleto ya en HTML 4, y la interfaz sigue existiendo
 * porque el DOM Nivel 1 de HTML la define.
 */
public interface HTMLAppletElement extends HTMLElement {

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El texto alternativo. */
    String getAlt();

    /** Fija el texto alternativo. */
    void setAlt(String alt);

    /** El atributo `archive`. */
    String getArchive();

    /** Fija el atributo `archive`. */
    void setArchive(String archive);

    /** El atributo `code`. */
    String getCode();

    /** Fija el atributo `code`. */
    void setCode(String code);

    /** El atributo `codeBase`. */
    String getCodeBase();

    /** Fija el atributo `codeBase`. */
    void setCodeBase(String codeBase);

    /** El alto. */
    String getHeight();

    /** Fija el alto. */
    void setHeight(String height);

    /** El atributo `hspace`. */
    String getHspace();

    /** Fija el atributo `hspace`. */
    void setHspace(String hspace);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** El atributo `object`. */
    String getObject();

    /** Fija el atributo `object`. */
    void setObject(String object);

    /** El atributo `vspace`. */
    String getVspace();

    /** Fija el atributo `vspace`. */
    void setVspace(String vspace);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);
}
