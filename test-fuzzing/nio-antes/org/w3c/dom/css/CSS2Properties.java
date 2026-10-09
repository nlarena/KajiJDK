package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * The 122 properties of CSS 2, each one as a pair of accessors.
 *
 * <p>It is a convenience, not a new layer: `getAzimuth()` is exactly
 * `getPropertyValue("azimuth")` and `setAzimuth(v)` is `setProperty("azimuth", v, "")`. What is
 * gained is that the compiler checks the name; what is lost is the priority, because no setter here
 * can mark `!important`.
 *
 * <p>The names turn the CSS hyphen into a capital letter: `font-size` is `getFontSize`. There is
 * one exception, not the three the note used to count: `float` is a Java keyword, so its accessors
 * are `getCssFloat`/`setCssFloat`, as the DOM defines them.
 *
 * <p>An implementation implements it **together with** {@link CSSStyleDeclaration}, on the same
 * object: the DOM declares them separately so that the second can exist without the first in a
 * reduced profile, not because they are two things.
 */
public interface CSS2Properties {


    /** The property `azimuth`. */
    String getAzimuth();

    /**
     * It sets the property `azimuth`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setAzimuth(String azimuth) throws DOMException;

    /** The property `background`. */
    String getBackground();

    /**
     * It sets the property `background`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBackground(String background) throws DOMException;

    /** The property `background-attachment`. */
    String getBackgroundAttachment();

    /**
     * It sets the property `background-attachment`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBackgroundAttachment(String backgroundAttachment) throws DOMException;

    /** The property `background-color`. */
    String getBackgroundColor();

    /**
     * It sets the property `background-color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBackgroundColor(String backgroundColor) throws DOMException;

    /** The property `background-image`. */
    String getBackgroundImage();

    /**
     * It sets the property `background-image`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBackgroundImage(String backgroundImage) throws DOMException;

    /** The property `background-position`. */
    String getBackgroundPosition();

    /**
     * It sets the property `background-position`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBackgroundPosition(String backgroundPosition) throws DOMException;

    /** The property `background-repeat`. */
    String getBackgroundRepeat();

    /**
     * It sets the property `background-repeat`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBackgroundRepeat(String backgroundRepeat) throws DOMException;

    /** The property `border`. */
    String getBorder();

    /**
     * It sets the property `border`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorder(String border) throws DOMException;

    /** The property `border-collapse`. */
    String getBorderCollapse();

    /**
     * It sets the property `border-collapse`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderCollapse(String borderCollapse) throws DOMException;

    /** The property `border-color`. */
    String getBorderColor();

    /**
     * It sets the property `border-color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderColor(String borderColor) throws DOMException;

    /** The property `border-spacing`. */
    String getBorderSpacing();

    /**
     * It sets the property `border-spacing`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderSpacing(String borderSpacing) throws DOMException;

    /** The property `border-style`. */
    String getBorderStyle();

    /**
     * It sets the property `border-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderStyle(String borderStyle) throws DOMException;

    /** The property `border-top`. */
    String getBorderTop();

    /**
     * It sets the property `border-top`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderTop(String borderTop) throws DOMException;

    /** The property `border-right`. */
    String getBorderRight();

    /**
     * It sets the property `border-right`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderRight(String borderRight) throws DOMException;

    /** The property `border-bottom`. */
    String getBorderBottom();

    /**
     * It sets the property `border-bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderBottom(String borderBottom) throws DOMException;

    /** The property `border-left`. */
    String getBorderLeft();

    /**
     * It sets the property `border-left`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderLeft(String borderLeft) throws DOMException;

    /** The property `border-top-color`. */
    String getBorderTopColor();

    /**
     * It sets the property `border-top-color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderTopColor(String borderTopColor) throws DOMException;

    /** The property `border-right-color`. */
    String getBorderRightColor();

    /**
     * It sets the property `border-right-color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderRightColor(String borderRightColor) throws DOMException;

    /** The property `border-bottom-color`. */
    String getBorderBottomColor();

    /**
     * It sets the property `border-bottom-color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderBottomColor(String borderBottomColor) throws DOMException;

    /** The property `border-left-color`. */
    String getBorderLeftColor();

    /**
     * It sets the property `border-left-color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderLeftColor(String borderLeftColor) throws DOMException;

    /** The property `border-top-style`. */
    String getBorderTopStyle();

    /**
     * It sets the property `border-top-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderTopStyle(String borderTopStyle) throws DOMException;

    /** The property `border-right-style`. */
    String getBorderRightStyle();

    /**
     * It sets the property `border-right-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderRightStyle(String borderRightStyle) throws DOMException;

    /** The property `border-bottom-style`. */
    String getBorderBottomStyle();

    /**
     * It sets the property `border-bottom-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderBottomStyle(String borderBottomStyle) throws DOMException;

    /** The property `border-left-style`. */
    String getBorderLeftStyle();

    /**
     * It sets the property `border-left-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderLeftStyle(String borderLeftStyle) throws DOMException;

    /** The property `border-top-width`. */
    String getBorderTopWidth();

    /**
     * It sets the property `border-top-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderTopWidth(String borderTopWidth) throws DOMException;

    /** The property `border-right-width`. */
    String getBorderRightWidth();

    /**
     * It sets the property `border-right-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderRightWidth(String borderRightWidth) throws DOMException;

    /** The property `border-bottom-width`. */
    String getBorderBottomWidth();

    /**
     * It sets the property `border-bottom-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderBottomWidth(String borderBottomWidth) throws DOMException;

    /** The property `border-left-width`. */
    String getBorderLeftWidth();

    /**
     * It sets the property `border-left-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderLeftWidth(String borderLeftWidth) throws DOMException;

    /** The property `border-width`. */
    String getBorderWidth();

    /**
     * It sets the property `border-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBorderWidth(String borderWidth) throws DOMException;

    /** The property `bottom`. */
    String getBottom();

    /**
     * It sets the property `bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setBottom(String bottom) throws DOMException;

    /** The property `caption-side`. */
    String getCaptionSide();

    /**
     * It sets the property `caption-side`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCaptionSide(String captionSide) throws DOMException;

    /** The property `clear`. */
    String getClear();

    /**
     * It sets the property `clear`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setClear(String clear) throws DOMException;

    /** The property `clip`. */
    String getClip();

    /**
     * It sets the property `clip`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setClip(String clip) throws DOMException;

    /** The property `color`. */
    String getColor();

    /**
     * It sets the property `color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setColor(String color) throws DOMException;

    /** The property `content`. */
    String getContent();

    /**
     * It sets the property `content`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setContent(String content) throws DOMException;

    /** The property `counter-increment`. */
    String getCounterIncrement();

    /**
     * It sets the property `counter-increment`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCounterIncrement(String counterIncrement) throws DOMException;

    /** The property `counter-reset`. */
    String getCounterReset();

    /**
     * It sets the property `counter-reset`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCounterReset(String counterReset) throws DOMException;

    /** The property `cue`. */
    String getCue();

    /**
     * It sets the property `cue`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCue(String cue) throws DOMException;

    /** The property `cue-after`. */
    String getCueAfter();

    /**
     * It sets the property `cue-after`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCueAfter(String cueAfter) throws DOMException;

    /** The property `cue-before`. */
    String getCueBefore();

    /**
     * It sets the property `cue-before`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCueBefore(String cueBefore) throws DOMException;

    /** The property `cursor`. */
    String getCursor();

    /**
     * It sets the property `cursor`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCursor(String cursor) throws DOMException;

    /** The property `direction`. */
    String getDirection();

    /**
     * It sets the property `direction`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setDirection(String direction) throws DOMException;

    /** The property `display`. */
    String getDisplay();

    /**
     * It sets the property `display`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setDisplay(String display) throws DOMException;

    /** The property `elevation`. */
    String getElevation();

    /**
     * It sets the property `elevation`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setElevation(String elevation) throws DOMException;

    /** The property `empty-cells`. */
    String getEmptyCells();

    /**
     * It sets the property `empty-cells`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setEmptyCells(String emptyCells) throws DOMException;

    /** The property `float` (see the note of the class for the name). */
    String getCssFloat();

    /**
     * It sets the property `float`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setCssFloat(String cssFloat) throws DOMException;

    /** The property `font`. */
    String getFont();

    /**
     * It sets the property `font`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFont(String font) throws DOMException;

    /** The property `font-family`. */
    String getFontFamily();

    /**
     * It sets the property `font-family`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFontFamily(String fontFamily) throws DOMException;

    /** The property `font-size`. */
    String getFontSize();

    /**
     * It sets the property `font-size`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFontSize(String fontSize) throws DOMException;

    /** The property `font-size-adjust`. */
    String getFontSizeAdjust();

    /**
     * It sets the property `font-size-adjust`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFontSizeAdjust(String fontSizeAdjust) throws DOMException;

    /** The property `font-stretch`. */
    String getFontStretch();

    /**
     * It sets the property `font-stretch`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFontStretch(String fontStretch) throws DOMException;

    /** The property `font-style`. */
    String getFontStyle();

    /**
     * It sets the property `font-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFontStyle(String fontStyle) throws DOMException;

    /** The property `font-variant`. */
    String getFontVariant();

    /**
     * It sets the property `font-variant`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFontVariant(String fontVariant) throws DOMException;

    /** The property `font-weight`. */
    String getFontWeight();

    /**
     * It sets the property `font-weight`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setFontWeight(String fontWeight) throws DOMException;

    /** The property `height`. */
    String getHeight();

    /**
     * It sets the property `height`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setHeight(String height) throws DOMException;

    /** The property `left`. */
    String getLeft();

    /**
     * It sets the property `left`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setLeft(String left) throws DOMException;

    /** The property `letter-spacing`. */
    String getLetterSpacing();

    /**
     * It sets the property `letter-spacing`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setLetterSpacing(String letterSpacing) throws DOMException;

    /** The property `line-height`. */
    String getLineHeight();

    /**
     * It sets the property `line-height`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setLineHeight(String lineHeight) throws DOMException;

    /** The property `list-style`. */
    String getListStyle();

    /**
     * It sets the property `list-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setListStyle(String listStyle) throws DOMException;

    /** The property `list-style-image`. */
    String getListStyleImage();

    /**
     * It sets the property `list-style-image`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setListStyleImage(String listStyleImage) throws DOMException;

    /** The property `list-style-position`. */
    String getListStylePosition();

    /**
     * It sets the property `list-style-position`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setListStylePosition(String listStylePosition) throws DOMException;

    /** The property `list-style-type`. */
    String getListStyleType();

    /**
     * It sets the property `list-style-type`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setListStyleType(String listStyleType) throws DOMException;

    /** The property `margin`. */
    String getMargin();

    /**
     * It sets the property `margin`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMargin(String margin) throws DOMException;

    /** The property `margin-top`. */
    String getMarginTop();

    /**
     * It sets the property `margin-top`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMarginTop(String marginTop) throws DOMException;

    /** The property `margin-right`. */
    String getMarginRight();

    /**
     * It sets the property `margin-right`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMarginRight(String marginRight) throws DOMException;

    /** The property `margin-bottom`. */
    String getMarginBottom();

    /**
     * It sets the property `margin-bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMarginBottom(String marginBottom) throws DOMException;

    /** The property `margin-left`. */
    String getMarginLeft();

    /**
     * It sets the property `margin-left`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMarginLeft(String marginLeft) throws DOMException;

    /** The property `marker-offset`. */
    String getMarkerOffset();

    /**
     * It sets the property `marker-offset`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMarkerOffset(String markerOffset) throws DOMException;

    /** The property `marks`. */
    String getMarks();

    /**
     * It sets the property `marks`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMarks(String marks) throws DOMException;

    /** The property `max-height`. */
    String getMaxHeight();

    /**
     * It sets the property `max-height`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMaxHeight(String maxHeight) throws DOMException;

    /** The property `max-width`. */
    String getMaxWidth();

    /**
     * It sets the property `max-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMaxWidth(String maxWidth) throws DOMException;

    /** The property `min-height`. */
    String getMinHeight();

    /**
     * It sets the property `min-height`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMinHeight(String minHeight) throws DOMException;

    /** The property `min-width`. */
    String getMinWidth();

    /**
     * It sets the property `min-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setMinWidth(String minWidth) throws DOMException;

    /** The property `orphans`. */
    String getOrphans();

    /**
     * It sets the property `orphans`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setOrphans(String orphans) throws DOMException;

    /** The property `outline`. */
    String getOutline();

    /**
     * It sets the property `outline`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setOutline(String outline) throws DOMException;

    /** The property `outline-color`. */
    String getOutlineColor();

    /**
     * It sets the property `outline-color`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setOutlineColor(String outlineColor) throws DOMException;

    /** The property `outline-style`. */
    String getOutlineStyle();

    /**
     * It sets the property `outline-style`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setOutlineStyle(String outlineStyle) throws DOMException;

    /** The property `outline-width`. */
    String getOutlineWidth();

    /**
     * It sets the property `outline-width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setOutlineWidth(String outlineWidth) throws DOMException;

    /** The property `overflow`. */
    String getOverflow();

    /**
     * It sets the property `overflow`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setOverflow(String overflow) throws DOMException;

    /** The property `padding`. */
    String getPadding();

    /**
     * It sets the property `padding`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPadding(String padding) throws DOMException;

    /** The property `padding-top`. */
    String getPaddingTop();

    /**
     * It sets the property `padding-top`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPaddingTop(String paddingTop) throws DOMException;

    /** The property `padding-right`. */
    String getPaddingRight();

    /**
     * It sets the property `padding-right`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPaddingRight(String paddingRight) throws DOMException;

    /** The property `padding-bottom`. */
    String getPaddingBottom();

    /**
     * It sets the property `padding-bottom`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPaddingBottom(String paddingBottom) throws DOMException;

    /** The property `padding-left`. */
    String getPaddingLeft();

    /**
     * It sets the property `padding-left`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPaddingLeft(String paddingLeft) throws DOMException;

    /** The property `page`. */
    String getPage();

    /**
     * It sets the property `page`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPage(String page) throws DOMException;

    /** The property `page-break-after`. */
    String getPageBreakAfter();

    /**
     * It sets the property `page-break-after`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPageBreakAfter(String pageBreakAfter) throws DOMException;

    /** The property `page-break-before`. */
    String getPageBreakBefore();

    /**
     * It sets the property `page-break-before`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPageBreakBefore(String pageBreakBefore) throws DOMException;

    /** The property `page-break-inside`. */
    String getPageBreakInside();

    /**
     * It sets the property `page-break-inside`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPageBreakInside(String pageBreakInside) throws DOMException;

    /** The property `pause`. */
    String getPause();

    /**
     * It sets the property `pause`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPause(String pause) throws DOMException;

    /** The property `pause-after`. */
    String getPauseAfter();

    /**
     * It sets the property `pause-after`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPauseAfter(String pauseAfter) throws DOMException;

    /** The property `pause-before`. */
    String getPauseBefore();

    /**
     * It sets the property `pause-before`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPauseBefore(String pauseBefore) throws DOMException;

    /** The property `pitch`. */
    String getPitch();

    /**
     * It sets the property `pitch`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPitch(String pitch) throws DOMException;

    /** The property `pitch-range`. */
    String getPitchRange();

    /**
     * It sets the property `pitch-range`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPitchRange(String pitchRange) throws DOMException;

    /** The property `play-during`. */
    String getPlayDuring();

    /**
     * It sets the property `play-during`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPlayDuring(String playDuring) throws DOMException;

    /** The property `position`. */
    String getPosition();

    /**
     * It sets the property `position`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setPosition(String position) throws DOMException;

    /** The property `quotes`. */
    String getQuotes();

    /**
     * It sets the property `quotes`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setQuotes(String quotes) throws DOMException;

    /** The property `richness`. */
    String getRichness();

    /**
     * It sets the property `richness`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setRichness(String richness) throws DOMException;

    /** The property `right`. */
    String getRight();

    /**
     * It sets the property `right`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setRight(String right) throws DOMException;

    /** The property `size`. */
    String getSize();

    /**
     * It sets the property `size`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setSize(String size) throws DOMException;

    /** The property `speak`. */
    String getSpeak();

    /**
     * It sets the property `speak`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setSpeak(String speak) throws DOMException;

    /** The property `speak-header`. */
    String getSpeakHeader();

    /**
     * It sets the property `speak-header`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setSpeakHeader(String speakHeader) throws DOMException;

    /** The property `speak-numeral`. */
    String getSpeakNumeral();

    /**
     * It sets the property `speak-numeral`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setSpeakNumeral(String speakNumeral) throws DOMException;

    /** The property `speak-punctuation`. */
    String getSpeakPunctuation();

    /**
     * It sets the property `speak-punctuation`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setSpeakPunctuation(String speakPunctuation) throws DOMException;

    /** The property `speech-rate`. */
    String getSpeechRate();

    /**
     * It sets the property `speech-rate`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setSpeechRate(String speechRate) throws DOMException;

    /** The property `stress`. */
    String getStress();

    /**
     * It sets the property `stress`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setStress(String stress) throws DOMException;

    /** The property `table-layout`. */
    String getTableLayout();

    /**
     * It sets the property `table-layout`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setTableLayout(String tableLayout) throws DOMException;

    /** The property `text-align`. */
    String getTextAlign();

    /**
     * It sets the property `text-align`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setTextAlign(String textAlign) throws DOMException;

    /** The property `text-decoration`. */
    String getTextDecoration();

    /**
     * It sets the property `text-decoration`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setTextDecoration(String textDecoration) throws DOMException;

    /** The property `text-indent`. */
    String getTextIndent();

    /**
     * It sets the property `text-indent`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setTextIndent(String textIndent) throws DOMException;

    /** The property `text-shadow`. */
    String getTextShadow();

    /**
     * It sets the property `text-shadow`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setTextShadow(String textShadow) throws DOMException;

    /** The property `text-transform`. */
    String getTextTransform();

    /**
     * It sets the property `text-transform`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setTextTransform(String textTransform) throws DOMException;

    /** The property `top`. */
    String getTop();

    /**
     * It sets the property `top`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setTop(String top) throws DOMException;

    /** The property `unicode-bidi`. */
    String getUnicodeBidi();

    /**
     * It sets the property `unicode-bidi`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setUnicodeBidi(String unicodeBidi) throws DOMException;

    /** The property `vertical-align`. */
    String getVerticalAlign();

    /**
     * It sets the property `vertical-align`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setVerticalAlign(String verticalAlign) throws DOMException;

    /** The property `visibility`. */
    String getVisibility();

    /**
     * It sets the property `visibility`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setVisibility(String visibility) throws DOMException;

    /** The property `voice-family`. */
    String getVoiceFamily();

    /**
     * It sets the property `voice-family`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setVoiceFamily(String voiceFamily) throws DOMException;

    /** The property `volume`. */
    String getVolume();

    /**
     * It sets the property `volume`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setVolume(String volume) throws DOMException;

    /** The property `white-space`. */
    String getWhiteSpace();

    /**
     * It sets the property `white-space`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setWhiteSpace(String whiteSpace) throws DOMException;

    /** The property `widows`. */
    String getWidows();

    /**
     * It sets the property `widows`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setWidows(String widows) throws DOMException;

    /** The property `width`. */
    String getWidth();

    /**
     * It sets the property `width`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setWidth(String width) throws DOMException;

    /** The property `word-spacing`. */
    String getWordSpacing();

    /**
     * It sets the property `word-spacing`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setWordSpacing(String wordSpacing) throws DOMException;

    /** The property `z-index`. */
    String getZIndex();

    /**
     * It sets the property `z-index`.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    void setZIndex(String zIndex) throws DOMException;
}
