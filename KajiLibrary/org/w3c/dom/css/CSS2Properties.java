package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * Las 122 propiedades de CSS 2, cada una como un par de accesores.
 *
 * <p>Es una comodidad, no una capa nueva: `getAzimuth()` es exactamente
 * `getPropertyValue("azimuth")` y `setAzimuth(v)` es `setProperty("azimuth", v, "")`. Lo que se
 * gana es que el compilador comprueba el nombre; lo que se pierde es la prioridad, porque ningun
 * setter de aca puede marcar `!important`.
 *
 * <p>Los nombres traducen el guion de CSS a mayuscula: `font-size` es `getFontSize`. Las tres
 * excepciones son las que empiezan con guion o chocan con una palabra de Java, y estan escritas
 * como el DOM las define.
 *
 * <p>Una implementacion la implementa **junto con** {@link CSSStyleDeclaration}, sobre el mismo
 * objeto: el DOM las declara separadas para que la segunda pueda existir sin la primera en un perfil
 * reducido, no porque sean dos cosas.
 */
public interface CSS2Properties {


    /** La propiedad `azimuth`. */
    String getAzimuth();

    /**
     * Fija la propiedad `azimuth`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setAzimuth(String azimuth) throws DOMException;

    /** La propiedad `background`. */
    String getBackground();

    /**
     * Fija la propiedad `background`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBackground(String background) throws DOMException;

    /** La propiedad `background-attachment`. */
    String getBackgroundAttachment();

    /**
     * Fija la propiedad `background-attachment`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBackgroundAttachment(String backgroundAttachment) throws DOMException;

    /** La propiedad `background-color`. */
    String getBackgroundColor();

    /**
     * Fija la propiedad `background-color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBackgroundColor(String backgroundColor) throws DOMException;

    /** La propiedad `background-image`. */
    String getBackgroundImage();

    /**
     * Fija la propiedad `background-image`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBackgroundImage(String backgroundImage) throws DOMException;

    /** La propiedad `background-position`. */
    String getBackgroundPosition();

    /**
     * Fija la propiedad `background-position`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBackgroundPosition(String backgroundPosition) throws DOMException;

    /** La propiedad `background-repeat`. */
    String getBackgroundRepeat();

    /**
     * Fija la propiedad `background-repeat`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBackgroundRepeat(String backgroundRepeat) throws DOMException;

    /** La propiedad `border`. */
    String getBorder();

    /**
     * Fija la propiedad `border`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorder(String border) throws DOMException;

    /** La propiedad `border-collapse`. */
    String getBorderCollapse();

    /**
     * Fija la propiedad `border-collapse`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderCollapse(String borderCollapse) throws DOMException;

    /** La propiedad `border-color`. */
    String getBorderColor();

    /**
     * Fija la propiedad `border-color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderColor(String borderColor) throws DOMException;

    /** La propiedad `border-spacing`. */
    String getBorderSpacing();

    /**
     * Fija la propiedad `border-spacing`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderSpacing(String borderSpacing) throws DOMException;

    /** La propiedad `border-style`. */
    String getBorderStyle();

    /**
     * Fija la propiedad `border-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderStyle(String borderStyle) throws DOMException;

    /** La propiedad `border-top`. */
    String getBorderTop();

    /**
     * Fija la propiedad `border-top`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderTop(String borderTop) throws DOMException;

    /** La propiedad `border-right`. */
    String getBorderRight();

    /**
     * Fija la propiedad `border-right`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderRight(String borderRight) throws DOMException;

    /** La propiedad `border-bottom`. */
    String getBorderBottom();

    /**
     * Fija la propiedad `border-bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderBottom(String borderBottom) throws DOMException;

    /** La propiedad `border-left`. */
    String getBorderLeft();

    /**
     * Fija la propiedad `border-left`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderLeft(String borderLeft) throws DOMException;

    /** La propiedad `border-top-color`. */
    String getBorderTopColor();

    /**
     * Fija la propiedad `border-top-color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderTopColor(String borderTopColor) throws DOMException;

    /** La propiedad `border-right-color`. */
    String getBorderRightColor();

    /**
     * Fija la propiedad `border-right-color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderRightColor(String borderRightColor) throws DOMException;

    /** La propiedad `border-bottom-color`. */
    String getBorderBottomColor();

    /**
     * Fija la propiedad `border-bottom-color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderBottomColor(String borderBottomColor) throws DOMException;

    /** La propiedad `border-left-color`. */
    String getBorderLeftColor();

    /**
     * Fija la propiedad `border-left-color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderLeftColor(String borderLeftColor) throws DOMException;

    /** La propiedad `border-top-style`. */
    String getBorderTopStyle();

    /**
     * Fija la propiedad `border-top-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderTopStyle(String borderTopStyle) throws DOMException;

    /** La propiedad `border-right-style`. */
    String getBorderRightStyle();

    /**
     * Fija la propiedad `border-right-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderRightStyle(String borderRightStyle) throws DOMException;

    /** La propiedad `border-bottom-style`. */
    String getBorderBottomStyle();

    /**
     * Fija la propiedad `border-bottom-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderBottomStyle(String borderBottomStyle) throws DOMException;

    /** La propiedad `border-left-style`. */
    String getBorderLeftStyle();

    /**
     * Fija la propiedad `border-left-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderLeftStyle(String borderLeftStyle) throws DOMException;

    /** La propiedad `border-top-width`. */
    String getBorderTopWidth();

    /**
     * Fija la propiedad `border-top-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderTopWidth(String borderTopWidth) throws DOMException;

    /** La propiedad `border-right-width`. */
    String getBorderRightWidth();

    /**
     * Fija la propiedad `border-right-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderRightWidth(String borderRightWidth) throws DOMException;

    /** La propiedad `border-bottom-width`. */
    String getBorderBottomWidth();

    /**
     * Fija la propiedad `border-bottom-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderBottomWidth(String borderBottomWidth) throws DOMException;

    /** La propiedad `border-left-width`. */
    String getBorderLeftWidth();

    /**
     * Fija la propiedad `border-left-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderLeftWidth(String borderLeftWidth) throws DOMException;

    /** La propiedad `border-width`. */
    String getBorderWidth();

    /**
     * Fija la propiedad `border-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBorderWidth(String borderWidth) throws DOMException;

    /** La propiedad `bottom`. */
    String getBottom();

    /**
     * Fija la propiedad `bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setBottom(String bottom) throws DOMException;

    /** La propiedad `caption-side`. */
    String getCaptionSide();

    /**
     * Fija la propiedad `caption-side`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCaptionSide(String captionSide) throws DOMException;

    /** La propiedad `clear`. */
    String getClear();

    /**
     * Fija la propiedad `clear`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setClear(String clear) throws DOMException;

    /** La propiedad `clip`. */
    String getClip();

    /**
     * Fija la propiedad `clip`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setClip(String clip) throws DOMException;

    /** La propiedad `color`. */
    String getColor();

    /**
     * Fija la propiedad `color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setColor(String color) throws DOMException;

    /** La propiedad `content`. */
    String getContent();

    /**
     * Fija la propiedad `content`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setContent(String content) throws DOMException;

    /** La propiedad `counter-increment`. */
    String getCounterIncrement();

    /**
     * Fija la propiedad `counter-increment`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCounterIncrement(String counterIncrement) throws DOMException;

    /** La propiedad `counter-reset`. */
    String getCounterReset();

    /**
     * Fija la propiedad `counter-reset`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCounterReset(String counterReset) throws DOMException;

    /** La propiedad `cue`. */
    String getCue();

    /**
     * Fija la propiedad `cue`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCue(String cue) throws DOMException;

    /** La propiedad `cue-after`. */
    String getCueAfter();

    /**
     * Fija la propiedad `cue-after`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCueAfter(String cueAfter) throws DOMException;

    /** La propiedad `cue-before`. */
    String getCueBefore();

    /**
     * Fija la propiedad `cue-before`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCueBefore(String cueBefore) throws DOMException;

    /** La propiedad `cursor`. */
    String getCursor();

    /**
     * Fija la propiedad `cursor`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCursor(String cursor) throws DOMException;

    /** La propiedad `direction`. */
    String getDirection();

    /**
     * Fija la propiedad `direction`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setDirection(String direction) throws DOMException;

    /** La propiedad `display`. */
    String getDisplay();

    /**
     * Fija la propiedad `display`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setDisplay(String display) throws DOMException;

    /** La propiedad `elevation`. */
    String getElevation();

    /**
     * Fija la propiedad `elevation`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setElevation(String elevation) throws DOMException;

    /** La propiedad `empty-cells`. */
    String getEmptyCells();

    /**
     * Fija la propiedad `empty-cells`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setEmptyCells(String emptyCells) throws DOMException;

    /** La propiedad `css-float`. */
    String getCssFloat();

    /**
     * Fija la propiedad `css-float`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setCssFloat(String cssFloat) throws DOMException;

    /** La propiedad `font`. */
    String getFont();

    /**
     * Fija la propiedad `font`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFont(String font) throws DOMException;

    /** La propiedad `font-family`. */
    String getFontFamily();

    /**
     * Fija la propiedad `font-family`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFontFamily(String fontFamily) throws DOMException;

    /** La propiedad `font-size`. */
    String getFontSize();

    /**
     * Fija la propiedad `font-size`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFontSize(String fontSize) throws DOMException;

    /** La propiedad `font-size-adjust`. */
    String getFontSizeAdjust();

    /**
     * Fija la propiedad `font-size-adjust`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFontSizeAdjust(String fontSizeAdjust) throws DOMException;

    /** La propiedad `font-stretch`. */
    String getFontStretch();

    /**
     * Fija la propiedad `font-stretch`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFontStretch(String fontStretch) throws DOMException;

    /** La propiedad `font-style`. */
    String getFontStyle();

    /**
     * Fija la propiedad `font-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFontStyle(String fontStyle) throws DOMException;

    /** La propiedad `font-variant`. */
    String getFontVariant();

    /**
     * Fija la propiedad `font-variant`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFontVariant(String fontVariant) throws DOMException;

    /** La propiedad `font-weight`. */
    String getFontWeight();

    /**
     * Fija la propiedad `font-weight`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setFontWeight(String fontWeight) throws DOMException;

    /** La propiedad `height`. */
    String getHeight();

    /**
     * Fija la propiedad `height`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setHeight(String height) throws DOMException;

    /** La propiedad `left`. */
    String getLeft();

    /**
     * Fija la propiedad `left`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setLeft(String left) throws DOMException;

    /** La propiedad `letter-spacing`. */
    String getLetterSpacing();

    /**
     * Fija la propiedad `letter-spacing`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setLetterSpacing(String letterSpacing) throws DOMException;

    /** La propiedad `line-height`. */
    String getLineHeight();

    /**
     * Fija la propiedad `line-height`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setLineHeight(String lineHeight) throws DOMException;

    /** La propiedad `list-style`. */
    String getListStyle();

    /**
     * Fija la propiedad `list-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setListStyle(String listStyle) throws DOMException;

    /** La propiedad `list-style-image`. */
    String getListStyleImage();

    /**
     * Fija la propiedad `list-style-image`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setListStyleImage(String listStyleImage) throws DOMException;

    /** La propiedad `list-style-position`. */
    String getListStylePosition();

    /**
     * Fija la propiedad `list-style-position`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setListStylePosition(String listStylePosition) throws DOMException;

    /** La propiedad `list-style-type`. */
    String getListStyleType();

    /**
     * Fija la propiedad `list-style-type`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setListStyleType(String listStyleType) throws DOMException;

    /** La propiedad `margin`. */
    String getMargin();

    /**
     * Fija la propiedad `margin`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMargin(String margin) throws DOMException;

    /** La propiedad `margin-top`. */
    String getMarginTop();

    /**
     * Fija la propiedad `margin-top`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMarginTop(String marginTop) throws DOMException;

    /** La propiedad `margin-right`. */
    String getMarginRight();

    /**
     * Fija la propiedad `margin-right`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMarginRight(String marginRight) throws DOMException;

    /** La propiedad `margin-bottom`. */
    String getMarginBottom();

    /**
     * Fija la propiedad `margin-bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMarginBottom(String marginBottom) throws DOMException;

    /** La propiedad `margin-left`. */
    String getMarginLeft();

    /**
     * Fija la propiedad `margin-left`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMarginLeft(String marginLeft) throws DOMException;

    /** La propiedad `marker-offset`. */
    String getMarkerOffset();

    /**
     * Fija la propiedad `marker-offset`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMarkerOffset(String markerOffset) throws DOMException;

    /** La propiedad `marks`. */
    String getMarks();

    /**
     * Fija la propiedad `marks`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMarks(String marks) throws DOMException;

    /** La propiedad `max-height`. */
    String getMaxHeight();

    /**
     * Fija la propiedad `max-height`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMaxHeight(String maxHeight) throws DOMException;

    /** La propiedad `max-width`. */
    String getMaxWidth();

    /**
     * Fija la propiedad `max-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMaxWidth(String maxWidth) throws DOMException;

    /** La propiedad `min-height`. */
    String getMinHeight();

    /**
     * Fija la propiedad `min-height`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMinHeight(String minHeight) throws DOMException;

    /** La propiedad `min-width`. */
    String getMinWidth();

    /**
     * Fija la propiedad `min-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setMinWidth(String minWidth) throws DOMException;

    /** La propiedad `orphans`. */
    String getOrphans();

    /**
     * Fija la propiedad `orphans`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setOrphans(String orphans) throws DOMException;

    /** La propiedad `outline`. */
    String getOutline();

    /**
     * Fija la propiedad `outline`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setOutline(String outline) throws DOMException;

    /** La propiedad `outline-color`. */
    String getOutlineColor();

    /**
     * Fija la propiedad `outline-color`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setOutlineColor(String outlineColor) throws DOMException;

    /** La propiedad `outline-style`. */
    String getOutlineStyle();

    /**
     * Fija la propiedad `outline-style`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setOutlineStyle(String outlineStyle) throws DOMException;

    /** La propiedad `outline-width`. */
    String getOutlineWidth();

    /**
     * Fija la propiedad `outline-width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setOutlineWidth(String outlineWidth) throws DOMException;

    /** La propiedad `overflow`. */
    String getOverflow();

    /**
     * Fija la propiedad `overflow`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setOverflow(String overflow) throws DOMException;

    /** La propiedad `padding`. */
    String getPadding();

    /**
     * Fija la propiedad `padding`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPadding(String padding) throws DOMException;

    /** La propiedad `padding-top`. */
    String getPaddingTop();

    /**
     * Fija la propiedad `padding-top`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPaddingTop(String paddingTop) throws DOMException;

    /** La propiedad `padding-right`. */
    String getPaddingRight();

    /**
     * Fija la propiedad `padding-right`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPaddingRight(String paddingRight) throws DOMException;

    /** La propiedad `padding-bottom`. */
    String getPaddingBottom();

    /**
     * Fija la propiedad `padding-bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPaddingBottom(String paddingBottom) throws DOMException;

    /** La propiedad `padding-left`. */
    String getPaddingLeft();

    /**
     * Fija la propiedad `padding-left`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPaddingLeft(String paddingLeft) throws DOMException;

    /** La propiedad `page`. */
    String getPage();

    /**
     * Fija la propiedad `page`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPage(String page) throws DOMException;

    /** La propiedad `page-break-after`. */
    String getPageBreakAfter();

    /**
     * Fija la propiedad `page-break-after`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPageBreakAfter(String pageBreakAfter) throws DOMException;

    /** La propiedad `page-break-before`. */
    String getPageBreakBefore();

    /**
     * Fija la propiedad `page-break-before`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPageBreakBefore(String pageBreakBefore) throws DOMException;

    /** La propiedad `page-break-inside`. */
    String getPageBreakInside();

    /**
     * Fija la propiedad `page-break-inside`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPageBreakInside(String pageBreakInside) throws DOMException;

    /** La propiedad `pause`. */
    String getPause();

    /**
     * Fija la propiedad `pause`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPause(String pause) throws DOMException;

    /** La propiedad `pause-after`. */
    String getPauseAfter();

    /**
     * Fija la propiedad `pause-after`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPauseAfter(String pauseAfter) throws DOMException;

    /** La propiedad `pause-before`. */
    String getPauseBefore();

    /**
     * Fija la propiedad `pause-before`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPauseBefore(String pauseBefore) throws DOMException;

    /** La propiedad `pitch`. */
    String getPitch();

    /**
     * Fija la propiedad `pitch`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPitch(String pitch) throws DOMException;

    /** La propiedad `pitch-range`. */
    String getPitchRange();

    /**
     * Fija la propiedad `pitch-range`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPitchRange(String pitchRange) throws DOMException;

    /** La propiedad `play-during`. */
    String getPlayDuring();

    /**
     * Fija la propiedad `play-during`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPlayDuring(String playDuring) throws DOMException;

    /** La propiedad `position`. */
    String getPosition();

    /**
     * Fija la propiedad `position`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setPosition(String position) throws DOMException;

    /** La propiedad `quotes`. */
    String getQuotes();

    /**
     * Fija la propiedad `quotes`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setQuotes(String quotes) throws DOMException;

    /** La propiedad `richness`. */
    String getRichness();

    /**
     * Fija la propiedad `richness`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setRichness(String richness) throws DOMException;

    /** La propiedad `right`. */
    String getRight();

    /**
     * Fija la propiedad `right`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setRight(String right) throws DOMException;

    /** La propiedad `size`. */
    String getSize();

    /**
     * Fija la propiedad `size`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setSize(String size) throws DOMException;

    /** La propiedad `speak`. */
    String getSpeak();

    /**
     * Fija la propiedad `speak`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setSpeak(String speak) throws DOMException;

    /** La propiedad `speak-header`. */
    String getSpeakHeader();

    /**
     * Fija la propiedad `speak-header`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setSpeakHeader(String speakHeader) throws DOMException;

    /** La propiedad `speak-numeral`. */
    String getSpeakNumeral();

    /**
     * Fija la propiedad `speak-numeral`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setSpeakNumeral(String speakNumeral) throws DOMException;

    /** La propiedad `speak-punctuation`. */
    String getSpeakPunctuation();

    /**
     * Fija la propiedad `speak-punctuation`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setSpeakPunctuation(String speakPunctuation) throws DOMException;

    /** La propiedad `speech-rate`. */
    String getSpeechRate();

    /**
     * Fija la propiedad `speech-rate`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setSpeechRate(String speechRate) throws DOMException;

    /** La propiedad `stress`. */
    String getStress();

    /**
     * Fija la propiedad `stress`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setStress(String stress) throws DOMException;

    /** La propiedad `table-layout`. */
    String getTableLayout();

    /**
     * Fija la propiedad `table-layout`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setTableLayout(String tableLayout) throws DOMException;

    /** La propiedad `text-align`. */
    String getTextAlign();

    /**
     * Fija la propiedad `text-align`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setTextAlign(String textAlign) throws DOMException;

    /** La propiedad `text-decoration`. */
    String getTextDecoration();

    /**
     * Fija la propiedad `text-decoration`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setTextDecoration(String textDecoration) throws DOMException;

    /** La propiedad `text-indent`. */
    String getTextIndent();

    /**
     * Fija la propiedad `text-indent`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setTextIndent(String textIndent) throws DOMException;

    /** La propiedad `text-shadow`. */
    String getTextShadow();

    /**
     * Fija la propiedad `text-shadow`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setTextShadow(String textShadow) throws DOMException;

    /** La propiedad `text-transform`. */
    String getTextTransform();

    /**
     * Fija la propiedad `text-transform`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setTextTransform(String textTransform) throws DOMException;

    /** La propiedad `top`. */
    String getTop();

    /**
     * Fija la propiedad `top`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setTop(String top) throws DOMException;

    /** La propiedad `unicode-bidi`. */
    String getUnicodeBidi();

    /**
     * Fija la propiedad `unicode-bidi`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setUnicodeBidi(String unicodeBidi) throws DOMException;

    /** La propiedad `vertical-align`. */
    String getVerticalAlign();

    /**
     * Fija la propiedad `vertical-align`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setVerticalAlign(String verticalAlign) throws DOMException;

    /** La propiedad `visibility`. */
    String getVisibility();

    /**
     * Fija la propiedad `visibility`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setVisibility(String visibility) throws DOMException;

    /** La propiedad `voice-family`. */
    String getVoiceFamily();

    /**
     * Fija la propiedad `voice-family`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setVoiceFamily(String voiceFamily) throws DOMException;

    /** La propiedad `volume`. */
    String getVolume();

    /**
     * Fija la propiedad `volume`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setVolume(String volume) throws DOMException;

    /** La propiedad `white-space`. */
    String getWhiteSpace();

    /**
     * Fija la propiedad `white-space`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setWhiteSpace(String whiteSpace) throws DOMException;

    /** La propiedad `widows`. */
    String getWidows();

    /**
     * Fija la propiedad `widows`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setWidows(String widows) throws DOMException;

    /** La propiedad `width`. */
    String getWidth();

    /**
     * Fija la propiedad `width`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setWidth(String width) throws DOMException;

    /** La propiedad `word-spacing`. */
    String getWordSpacing();

    /**
     * Fija la propiedad `word-spacing`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setWordSpacing(String wordSpacing) throws DOMException;

    /** La propiedad `z-index`. */
    String getZIndex();

    /**
     * Fija la propiedad `z-index`.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    void setZIndex(String zIndex) throws DOMException;
}
