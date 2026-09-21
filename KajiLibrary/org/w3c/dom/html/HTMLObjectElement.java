package org.w3c.dom.html;

import org.w3c.dom.Document;

/**
 * An `<object>`.
 */
public interface HTMLObjectElement extends HTMLElement {

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The `code` attribute. */
    String getCode();

    /** It sets the `code` attribute. */
    void setCode(String code);

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The `archive` attribute. */
    String getArchive();

    /** It sets the `archive` attribute. */
    void setArchive(String archive);

    /** The border. */
    String getBorder();

    /** It sets the border. */
    void setBorder(String border);

    /** The `codeBase` attribute. */
    String getCodeBase();

    /** It sets the `codeBase` attribute. */
    void setCodeBase(String codeBase);

    /** The `codeType` attribute. */
    String getCodeType();

    /** It sets the `codeType` attribute. */
    void setCodeType(String codeType);

    /** The `data` attribute. */
    String getData();

    /** It sets the `data` attribute. */
    void setData(String data);

    /** The `declare` attribute. */
    boolean getDeclare();

    /** It sets the `declare` attribute. */
    void setDeclare(boolean declare);

    /** The height. */
    String getHeight();

    /** It sets the height. */
    void setHeight(String height);

    /** The `hspace` attribute. */
    String getHspace();

    /** It sets the `hspace` attribute. */
    void setHspace(String hspace);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The `standby` attribute. */
    String getStandby();

    /** It sets the `standby` attribute. */
    void setStandby(String standby);

    /** The position in the tabbing order. */
    int getTabIndex();

    /** It sets the position in the tabbing order. */
    void setTabIndex(int tabIndex);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);

    /** The `useMap` attribute. */
    String getUseMap();

    /** It sets the `useMap` attribute. */
    void setUseMap(String useMap);

    /** The `vspace` attribute. */
    String getVspace();

    /** It sets the `vspace` attribute. */
    void setVspace(String vspace);

    /** The width. */
    String getWidth();

    /** It sets the width. */
    void setWidth(String width);

    /** The document loaded inside, or null if there is none or it is from another origin. */
    Document getContentDocument();
}
