package org.w3c.dom.html;

import org.w3c.dom.Document;

/**
 * Un `<object>`.
 */
public interface HTMLObjectElement extends HTMLElement {

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** El atributo `code`. */
    String getCode();

    /** Fija el atributo `code`. */
    void setCode(String code);

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El atributo `archive`. */
    String getArchive();

    /** Fija el atributo `archive`. */
    void setArchive(String archive);

    /** El borde. */
    String getBorder();

    /** Fija el borde. */
    void setBorder(String border);

    /** El atributo `codeBase`. */
    String getCodeBase();

    /** Fija el atributo `codeBase`. */
    void setCodeBase(String codeBase);

    /** El atributo `codeType`. */
    String getCodeType();

    /** Fija el atributo `codeType`. */
    void setCodeType(String codeType);

    /** El atributo `data`. */
    String getData();

    /** Fija el atributo `data`. */
    void setData(String data);

    /** El atributo `declare`. */
    boolean getDeclare();

    /** Fija el atributo `declare`. */
    void setDeclare(boolean declare);

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

    /** El atributo `standby`. */
    String getStandby();

    /** Fija el atributo `standby`. */
    void setStandby(String standby);

    /** La posicion en el orden de tabulacion. */
    int getTabIndex();

    /** Fija la posicion en el orden de tabulacion. */
    void setTabIndex(int tabIndex);

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);

    /** El atributo `useMap`. */
    String getUseMap();

    /** Fija el atributo `useMap`. */
    void setUseMap(String useMap);

    /** El atributo `vspace`. */
    String getVspace();

    /** Fija el atributo `vspace`. */
    void setVspace(String vspace);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);

    /** El documento cargado adentro, o nulo si no hay o es de otro origen. */
    Document getContentDocument();
}
