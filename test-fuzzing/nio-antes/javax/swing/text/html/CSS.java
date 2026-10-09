package javax.swing.text.html;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * The CSS property names, as constants.
 *
 * <h2>The same idea as {@link HTML}, for another language</h2>
 *
 * <p>Each property is a unique object that serves as a key in a document's attributes. What it
 * adds over {@link HTML.Attribute} are two data that in CSS are needed at every step: the default
 * value and whether the property is inherited from the element above.
 *
 * <h2>Why what is inherited matters</h2>
 *
 * <p>The letter's colour is inherited and the background colour is not. That difference is not a
 * detail: it is what makes putting a colour on the <code>&lt;body&gt;</code> paint all the page's
 * text but not paint each paragraph's background. Without this table it would have to be decided
 * case by case, and the decisions would not agree with each other.
 *
 * <p>The properties that are not inherited have a default value that applies when nobody sets
 * them. Those that are inherited have one too, but it only gets used at the root.
 */
public class CSS implements Serializable {

    private static final Hashtable<String, Attribute> attributeMap =
            new Hashtable<String, Attribute>();

    /** Nothing to build; the class is only the place where the constants live. */
    public CSS() {
    }

    /** Every known property; it is a copy. */
    public static Attribute[] getAllAttributeKeys() {
        Attribute[] keys = new Attribute[Attribute.allAttributes.length];
        System.arraycopy(Attribute.allAttributes, 0, keys, 0, Attribute.allAttributes.length);
        return keys;
    }

    /** The property with that name, or null. The name goes in lower case, as in the sheet. */
    public static final Attribute getAttribute(String name) {
        return attributeMap.get(name);
    }

    /**
     * A CSS property's name.
     *
     * <p>It is final and its constructor is not public: the list is closed. A property that is not
     * here is ignored when reading the style sheet, which is better than keeping it and not knowing
     * what to do with it later.
     */
    public static final class Attribute {

        private String name;
        private String defaultValue;
        private boolean inherited;

        Attribute(String name, String defaultValue, boolean inherited) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.inherited = inherited;
        }

        /** The name just as it is written in the style sheet. */
        public String toString() {
            return name;
        }

        /** What it is worth if nobody sets it; null if it has no single value. */
        public String getDefaultValue() {
            return defaultValue;
        }

        /** Whether the element inside receives it without declaring it; see {@link CSS}'s note. */
        public boolean isInherited() {
            return inherited;
        }

        public static final Attribute BACKGROUND =
                new Attribute("background", null, false);

        public static final Attribute BACKGROUND_ATTACHMENT =
                new Attribute("background-attachment", "scroll", false);

        public static final Attribute BACKGROUND_COLOR =
                new Attribute("background-color", "transparent", false);

        public static final Attribute BACKGROUND_IMAGE =
                new Attribute("background-image", "none", false);

        public static final Attribute BACKGROUND_POSITION =
                new Attribute("background-position", "0% 0%", false);

        public static final Attribute BACKGROUND_REPEAT =
                new Attribute("background-repeat", "repeat", false);

        public static final Attribute BORDER =
                new Attribute("border", null, false);

        public static final Attribute BORDER_BOTTOM =
                new Attribute("border-bottom", null, false);

        public static final Attribute BORDER_BOTTOM_WIDTH =
                new Attribute("border-bottom-width", "medium", false);

        public static final Attribute BORDER_COLOR =
                new Attribute("border-color", null, false);

        public static final Attribute BORDER_LEFT =
                new Attribute("border-left", null, false);

        public static final Attribute BORDER_LEFT_WIDTH =
                new Attribute("border-left-width", "medium", false);

        public static final Attribute BORDER_RIGHT =
                new Attribute("border-right", null, false);

        public static final Attribute BORDER_RIGHT_WIDTH =
                new Attribute("border-right-width", "medium", false);

        public static final Attribute BORDER_STYLE =
                new Attribute("border-style", "none", false);

        public static final Attribute BORDER_TOP =
                new Attribute("border-top", null, false);

        public static final Attribute BORDER_TOP_WIDTH =
                new Attribute("border-top-width", "medium", false);

        public static final Attribute BORDER_WIDTH =
                new Attribute("border-width", "medium", false);

        public static final Attribute BORDER_TOP_STYLE =
                new Attribute("border-top-style", "none", false);

        public static final Attribute BORDER_RIGHT_STYLE =
                new Attribute("border-right-style", "none", false);

        public static final Attribute BORDER_BOTTOM_STYLE =
                new Attribute("border-bottom-style", "none", false);

        public static final Attribute BORDER_LEFT_STYLE =
                new Attribute("border-left-style", "none", false);

        public static final Attribute BORDER_TOP_COLOR =
                new Attribute("border-top-color", null, false);

        public static final Attribute BORDER_RIGHT_COLOR =
                new Attribute("border-right-color", null, false);

        public static final Attribute BORDER_BOTTOM_COLOR =
                new Attribute("border-bottom-color", null, false);

        public static final Attribute BORDER_LEFT_COLOR =
                new Attribute("border-left-color", null, false);

        public static final Attribute CLEAR =
                new Attribute("clear", "none", false);

        public static final Attribute COLOR =
                new Attribute("color", "black", true);

        public static final Attribute DISPLAY =
                new Attribute("display", "block", false);

        public static final Attribute FLOAT =
                new Attribute("float", "none", false);

        public static final Attribute FONT =
                new Attribute("font", null, true);

        public static final Attribute FONT_FAMILY =
                new Attribute("font-family", null, true);

        public static final Attribute FONT_SIZE =
                new Attribute("font-size", "medium", true);

        public static final Attribute FONT_STYLE =
                new Attribute("font-style", "normal", true);

        public static final Attribute FONT_VARIANT =
                new Attribute("font-variant", "normal", true);

        public static final Attribute FONT_WEIGHT =
                new Attribute("font-weight", "normal", true);

        public static final Attribute HEIGHT =
                new Attribute("height", "auto", false);

        public static final Attribute LETTER_SPACING =
                new Attribute("letter-spacing", "normal", true);

        public static final Attribute LINE_HEIGHT =
                new Attribute("line-height", "normal", true);

        public static final Attribute LIST_STYLE =
                new Attribute("list-style", null, true);

        public static final Attribute LIST_STYLE_IMAGE =
                new Attribute("list-style-image", "none", true);

        public static final Attribute LIST_STYLE_POSITION =
                new Attribute("list-style-position", "outside", true);

        public static final Attribute LIST_STYLE_TYPE =
                new Attribute("list-style-type", "disc", true);

        public static final Attribute MARGIN =
                new Attribute("margin", null, false);

        public static final Attribute MARGIN_BOTTOM =
                new Attribute("margin-bottom", "0", false);

        public static final Attribute MARGIN_LEFT =
                new Attribute("margin-left", "0", false);

        public static final Attribute MARGIN_RIGHT =
                new Attribute("margin-right", "0", false);

        public static final Attribute MARGIN_TOP =
                new Attribute("margin-top", "0", false);

        public static final Attribute PADDING =
                new Attribute("padding", null, false);

        public static final Attribute PADDING_BOTTOM =
                new Attribute("padding-bottom", "0", false);

        public static final Attribute PADDING_LEFT =
                new Attribute("padding-left", "0", false);

        public static final Attribute PADDING_RIGHT =
                new Attribute("padding-right", "0", false);

        public static final Attribute PADDING_TOP =
                new Attribute("padding-top", "0", false);

        public static final Attribute TEXT_ALIGN =
                new Attribute("text-align", null, true);

        public static final Attribute TEXT_DECORATION =
                new Attribute("text-decoration", "none", true);

        public static final Attribute TEXT_INDENT =
                new Attribute("text-indent", "0", true);

        public static final Attribute TEXT_TRANSFORM =
                new Attribute("text-transform", "none", true);

        public static final Attribute VERTICAL_ALIGN =
                new Attribute("vertical-align", "baseline", false);

        public static final Attribute WORD_SPACING =
                new Attribute("word-spacing", "normal", true);

        public static final Attribute WHITE_SPACE =
                new Attribute("white-space", "normal", true);

        public static final Attribute WIDTH =
                new Attribute("width", "auto", false);

        static final Attribute BORDER_SPACING =
                new Attribute("border-spacing", "0", true);

        static final Attribute CAPTION_SIDE =
                new Attribute("caption-side", "left", true);

        static final Attribute MARGIN_LEFT_LTR =
                new Attribute("margin-left-ltr", "-2147483648", false);

        static final Attribute MARGIN_LEFT_RTL =
                new Attribute("margin-left-rtl", "-2147483648", false);

        static final Attribute MARGIN_RIGHT_LTR =
                new Attribute("margin-right-ltr", "-2147483648", false);

        static final Attribute MARGIN_RIGHT_RTL =
                new Attribute("margin-right-rtl", "-2147483648", false);

        static final Attribute[] allAttributes = {
            BACKGROUND, BACKGROUND_ATTACHMENT, BACKGROUND_COLOR, BACKGROUND_IMAGE,
            BACKGROUND_POSITION, BACKGROUND_REPEAT, BORDER, BORDER_BOTTOM, BORDER_BOTTOM_WIDTH,
            BORDER_COLOR, BORDER_LEFT, BORDER_LEFT_WIDTH, BORDER_RIGHT, BORDER_RIGHT_WIDTH,
            BORDER_STYLE, BORDER_TOP, BORDER_TOP_WIDTH, BORDER_WIDTH, BORDER_TOP_STYLE,
            BORDER_RIGHT_STYLE, BORDER_BOTTOM_STYLE, BORDER_LEFT_STYLE, BORDER_TOP_COLOR,
            BORDER_RIGHT_COLOR, BORDER_BOTTOM_COLOR, BORDER_LEFT_COLOR, CLEAR, COLOR, DISPLAY, FLOAT,
            FONT, FONT_FAMILY, FONT_SIZE, FONT_STYLE, FONT_VARIANT, FONT_WEIGHT, HEIGHT,
            LETTER_SPACING, LINE_HEIGHT, LIST_STYLE, LIST_STYLE_IMAGE, LIST_STYLE_POSITION,
            LIST_STYLE_TYPE, MARGIN, MARGIN_BOTTOM, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, PADDING,
            PADDING_BOTTOM, PADDING_LEFT, PADDING_RIGHT, PADDING_TOP, TEXT_ALIGN, TEXT_DECORATION,
            TEXT_INDENT, TEXT_TRANSFORM, VERTICAL_ALIGN, WORD_SPACING, WHITE_SPACE, WIDTH,
            BORDER_SPACING, CAPTION_SIDE, MARGIN_LEFT_LTR, MARGIN_LEFT_RTL, MARGIN_RIGHT_LTR,
            MARGIN_RIGHT_RTL
        };
    }

    static {
        for (int i = 0; i < Attribute.allAttributes.length; i++) {
            attributeMap.put(Attribute.allAttributes[i].toString(), Attribute.allAttributes[i]);
        }
    }
}
