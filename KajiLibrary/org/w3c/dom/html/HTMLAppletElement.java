package org.w3c.dom.html;

/**
 * An `<applet>`. Already obsolete in HTML 4, and the interface still exists because DOM Level 1
 * HTML defines it.
 */
public interface HTMLAppletElement extends HTMLElement {

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The alternative text. */
    String getAlt();

    /** It sets the alternative text. */
    void setAlt(String alt);

    /** The `archive` attribute. */
    String getArchive();

    /** It sets the `archive` attribute. */
    void setArchive(String archive);

    /** The `code` attribute. */
    String getCode();

    /** It sets the `code` attribute. */
    void setCode(String code);

    /** The `codeBase` attribute. */
    String getCodeBase();

    /** It sets the `codeBase` attribute. */
    void setCodeBase(String codeBase);

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

    /** The `object` attribute. */
    String getObject();

    /** It sets the `object` attribute. */
    void setObject(String object);

    /** The `vspace` attribute. */
    String getVspace();

    /** It sets the `vspace` attribute. */
    void setVspace(String vspace);

    /** The width. */
    String getWidth();

    /** It sets the width. */
    void setWidth(String width);
}
