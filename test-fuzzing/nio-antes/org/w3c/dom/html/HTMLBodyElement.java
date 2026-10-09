package org.w3c.dom.html;

/**
 * A `<body>`. Its properties are the presentational ones of HTML 3.2 --colours and background--
 * that HTML 4 deprecates in favour of the style sheet.
 */
public interface HTMLBodyElement extends HTMLElement {

    /** The `aLink` attribute. */
    String getALink();

    /** It sets the `aLink` attribute. */
    void setALink(String aLink);

    /** The `background` attribute. */
    String getBackground();

    /** It sets the `background` attribute. */
    void setBackground(String background);

    /** The `bgColor` attribute. */
    String getBgColor();

    /** It sets the `bgColor` attribute. */
    void setBgColor(String bgColor);

    /** The `link` attribute. */
    String getLink();

    /** It sets the `link` attribute. */
    void setLink(String link);

    /** The text that is shown. */
    String getText();

    /** It sets the text that is shown. */
    void setText(String text);

    /** The `vLink` attribute. */
    String getVLink();

    /** It sets the `vLink` attribute. */
    void setVLink(String vLink);
}
