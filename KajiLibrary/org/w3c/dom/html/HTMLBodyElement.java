package org.w3c.dom.html;

/**
 * Un `<body>`. Sus propiedades son las de presentacion de HTML 3.2
 * --colores y fondo-- que HTML 4 desaconseja en favor de la hoja de estilos.
 */
public interface HTMLBodyElement extends HTMLElement {

    /** El atributo `aLink`. */
    String getALink();

    /** Fija el atributo `aLink`. */
    void setALink(String aLink);

    /** El atributo `background`. */
    String getBackground();

    /** Fija el atributo `background`. */
    void setBackground(String background);

    /** El atributo `bgColor`. */
    String getBgColor();

    /** Fija el atributo `bgColor`. */
    void setBgColor(String bgColor);

    /** El atributo `link`. */
    String getLink();

    /** Fija el atributo `link`. */
    void setLink(String link);

    /** El texto que se muestra. */
    String getText();

    /** Fija el texto que se muestra. */
    void setText(String text);

    /** El atributo `vLink`. */
    String getVLink();

    /** Fija el atributo `vLink`. */
    void setVLink(String vLink);
}
