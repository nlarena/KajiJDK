package org.w3c.dom.html;

/**
 * An `<img>`.
 */
public interface HTMLImageElement extends HTMLElement {

    /** The `lowSrc` attribute. */
    String getLowSrc();

    /** It sets the `lowSrc` attribute. */
    void setLowSrc(String lowSrc);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The alternative text. */
    String getAlt();

    /** It sets the alternative text. */
    void setAlt(String alt);

    /** The border. */
    String getBorder();

    /** It sets the border. */
    void setBorder(String border);

    /** The height. */
    String getHeight();

    /** It sets the height. */
    void setHeight(String height);

    /** The `hspace` attribute. */
    String getHspace();

    /** It sets the `hspace` attribute. */
    void setHspace(String hspace);

    /** The `isMap` attribute. */
    boolean getIsMap();

    /** It sets the `isMap` attribute. */
    void setIsMap(boolean isMap);

    /** The `longDesc` attribute. */
    String getLongDesc();

    /** It sets the `longDesc` attribute. */
    void setLongDesc(String longDesc);

    /** The source. */
    String getSrc();

    /** It sets the source. */
    void setSrc(String src);

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
}
