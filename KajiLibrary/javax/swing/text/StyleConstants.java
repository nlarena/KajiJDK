package javax.swing.text;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;

/**
 * The keys of the attributes Swing understands, and the typed accessors for reading and
 * setting them.
 *
 * <h2>Keys that are objects, not strings</h2>
 *
 * <p>Each key is an <em>instance</em> of this class, not a {@code String}. Two advantages: two
 * different attributes cannot clash by being called the same, and the key carries on it which
 * family it belongs to --character, paragraph, colour, font-- through the interfaces it
 * implements ({@link AttributeSet.CharacterAttribute} and company). That last thing is what
 * allows an editor to ask "which paragraph attributes does this have" without a list written by
 * hand.
 *
 * <p>The static accessors are the only convenient way of reading an attribute: the set keeps
 * {@code Object}s, and here are each one's cast and default value. An attribute that is not there
 * is not an error: whatever corresponds is answered --{@code false}, zero, the foreground
 * colour.
 *
 * <p>{@link #Family} and {@link #FontFamily} are the same key, just like {@link #Size} and
 * {@link #FontSize}: they are two historical names of the same object.
 */
public class StyleConstants {

    /** The element name of an embedded component. */
    public static final String ComponentElementName = "component";

    /** The element name of an embedded icon. */
    public static final String IconElementName = "icon";

    /** The name of the attribute that keeps an element's or a style's name. */
    public static final Object NameAttribute = new StyleConstants("name");

    /** The resolving parent; see {@link MutableAttributeSet}. */
    public static final Object ResolveAttribute = new StyleConstants("resolver");

    /** An embedded component's model. */
    public static final Object ModelAttribute = new StyleConstants("model");

    /** The text's bidirectional level, for mixing scripts of different direction. */
    public static final Object BidiLevel = new CharacterConstants("bidiLevel");

    public static final Object FontFamily = new FontConstants("family");

    /** {@link #FontFamily}'s other name; it is the same object. */
    public static final Object Family = FontFamily;

    public static final Object FontSize = new FontConstants("size");

    /** {@link #FontSize}'s other name; it is the same object. */
    public static final Object Size = FontSize;

    public static final Object Bold = new FontConstants("bold");

    public static final Object Italic = new FontConstants("italic");

    public static final Object Underline = new CharacterConstants("underline");

    public static final Object StrikeThrough = new CharacterConstants("strikethrough");

    public static final Object Superscript = new CharacterConstants("superscript");

    public static final Object Subscript = new CharacterConstants("subscript");

    public static final Object Foreground = new ColorConstants("foreground");

    public static final Object Background = new ColorConstants("background");

    /** The component embedded in the text. */
    public static final Object ComponentAttribute = new CharacterConstants("component");

    /** The icon embedded in the text. */
    public static final Object IconAttribute = new CharacterConstants("icon");

    /** An input method's text being composed. */
    public static final Object ComposedTextAttribute = new StyleConstants("composed text");

    public static final Object FirstLineIndent = new ParagraphConstants("FirstLineIndent");

    public static final Object LeftIndent = new ParagraphConstants("LeftIndent");

    public static final Object RightIndent = new ParagraphConstants("RightIndent");

    public static final Object LineSpacing = new ParagraphConstants("LineSpacing");

    public static final Object SpaceAbove = new ParagraphConstants("SpaceAbove");

    public static final Object SpaceBelow = new ParagraphConstants("SpaceBelow");

    public static final Object Alignment = new ParagraphConstants("Alignment");

    public static final Object TabSet = new ParagraphConstants("TabSet");

    /** The paragraph's writing direction. */
    public static final Object Orientation = new ParagraphConstants("Orientation");

    public static final int ALIGN_LEFT = 0;

    public static final int ALIGN_CENTER = 1;

    public static final int ALIGN_RIGHT = 2;

    /** The text is stretched to reach both margins. */
    public static final int ALIGN_JUSTIFIED = 3;

    /**
     * Every key, for walking them.
     *
     * <p>It is a public and modifiable array, which today would be a design mistake; it is so in
     * the JDK since 1.2 and changing it would break programs.
     */
    public static Object[] keys = {
        NameAttribute, ResolveAttribute, BidiLevel,
        FontFamily, FontSize, Bold, Italic, Underline,
        StrikeThrough, Superscript, Subscript,
        Foreground, Background, ComponentAttribute,
        FirstLineIndent, LeftIndent, RightIndent, LineSpacing,
        SpaceAbove, SpaceBelow, Alignment, TabSet, Orientation,
        ModelAttribute, ComponentElementName, IconElementName,
        IconAttribute, ComposedTextAttribute };

    private String representation;

    /** A new key with that display name; only the nested ones use it. */
    StyleConstants(String representation) {
        this.representation = representation;
    }

    /** The key's name; it is what is seen when printing an attribute set. */
    public String toString() {
        return representation;
    }

    // -- character --------------------------------------------------------------------------------

    /** The bidirectional level; zero if it is not there. */
    public static int getBidiLevel(AttributeSet a) {
        Integer o = (Integer) a.getAttribute(BidiLevel);
        if (o != null) {
            return o.intValue();
        }
        return 0;
    }

    public static void setBidiLevel(MutableAttributeSet a, int o) {
        a.addAttribute(BidiLevel, Integer.valueOf(o));
    }

    /** The embedded component, or {@code null}. */
    public static Component getComponent(AttributeSet a) {
        return (Component) a.getAttribute(ComponentAttribute);
    }

    /** It embeds a component; it also sets the element name that corresponds to it. */
    public static void setComponent(MutableAttributeSet a, Component c) {
        a.addAttribute(AbstractDocument.ElementNameAttribute, ComponentElementName);
        a.addAttribute(ComponentAttribute, c);
    }

    /** The embedded icon, or {@code null}. */
    public static Icon getIcon(AttributeSet a) {
        return (Icon) a.getAttribute(IconAttribute);
    }

    public static void setIcon(MutableAttributeSet a, Icon c) {
        a.addAttribute(AbstractDocument.ElementNameAttribute, IconElementName);
        a.addAttribute(IconAttribute, c);
    }

    /** The typeface family; "Monospaced" if it is not there. */
    public static String getFontFamily(AttributeSet a) {
        String family = (String) a.getAttribute(FontFamily);
        if (family == null) {
            family = "Monospaced";
        }
        return family;
    }

    public static void setFontFamily(MutableAttributeSet a, String fam) {
        a.addAttribute(FontFamily, fam);
    }

    /** The font's size; 12 if it is not there. */
    public static int getFontSize(AttributeSet a) {
        Integer size = (Integer) a.getAttribute(FontSize);
        if (size != null) {
            return size.intValue();
        }
        return 12;
    }

    public static void setFontSize(MutableAttributeSet a, int s) {
        a.addAttribute(FontSize, Integer.valueOf(s));
    }

    public static boolean isBold(AttributeSet a) {
        Boolean bold = (Boolean) a.getAttribute(Bold);
        if (bold != null) {
            return bold.booleanValue();
        }
        return false;
    }

    public static void setBold(MutableAttributeSet a, boolean b) {
        a.addAttribute(Bold, Boolean.valueOf(b));
    }

    public static boolean isItalic(AttributeSet a) {
        Boolean italic = (Boolean) a.getAttribute(Italic);
        if (italic != null) {
            return italic.booleanValue();
        }
        return false;
    }

    public static void setItalic(MutableAttributeSet a, boolean b) {
        a.addAttribute(Italic, Boolean.valueOf(b));
    }

    public static boolean isUnderline(AttributeSet a) {
        Boolean underline = (Boolean) a.getAttribute(Underline);
        if (underline != null) {
            return underline.booleanValue();
        }
        return false;
    }

    public static boolean isStrikeThrough(AttributeSet a) {
        Boolean strike = (Boolean) a.getAttribute(StrikeThrough);
        if (strike != null) {
            return strike.booleanValue();
        }
        return false;
    }

    public static boolean isSuperscript(AttributeSet a) {
        Boolean superscript = (Boolean) a.getAttribute(Superscript);
        if (superscript != null) {
            return superscript.booleanValue();
        }
        return false;
    }

    public static boolean isSubscript(AttributeSet a) {
        Boolean subscript = (Boolean) a.getAttribute(Subscript);
        if (subscript != null) {
            return subscript.booleanValue();
        }
        return false;
    }

    public static void setUnderline(MutableAttributeSet a, boolean b) {
        a.addAttribute(Underline, Boolean.valueOf(b));
    }

    public static void setStrikeThrough(MutableAttributeSet a, boolean b) {
        a.addAttribute(StrikeThrough, Boolean.valueOf(b));
    }

    public static void setSuperscript(MutableAttributeSet a, boolean b) {
        a.addAttribute(Superscript, Boolean.valueOf(b));
    }

    public static void setSubscript(MutableAttributeSet a, boolean b) {
        a.addAttribute(Subscript, Boolean.valueOf(b));
    }

    /** The text's colour; black if it is not there. */
    public static Color getForeground(AttributeSet a) {
        Color fg = (Color) a.getAttribute(Foreground);
        if (fg == null) {
            fg = Color.black;
        }
        return fg;
    }

    public static void setForeground(MutableAttributeSet a, Color fg) {
        a.addAttribute(Foreground, fg);
    }

    /** The background's colour; white if it is not there. */
    public static Color getBackground(AttributeSet a) {
        Color fg = (Color) a.getAttribute(Background);
        if (fg == null) {
            fg = Color.black;
        }
        return fg;
    }

    public static void setBackground(MutableAttributeSet a, Color fg) {
        a.addAttribute(Background, fg);
    }

    // -- paragraph --------------------------------------------------------------------------------

    /** The first line's indent; zero if it is not there. */
    public static float getFirstLineIndent(AttributeSet a) {
        Float indent = (Float) a.getAttribute(FirstLineIndent);
        if (indent != null) {
            return indent.floatValue();
        }
        return 0;
    }

    public static void setFirstLineIndent(MutableAttributeSet a, float i) {
        a.addAttribute(FirstLineIndent, Float.valueOf(i));
    }

    public static float getRightIndent(AttributeSet a) {
        Float indent = (Float) a.getAttribute(RightIndent);
        if (indent != null) {
            return indent.floatValue();
        }
        return 0;
    }

    public static void setRightIndent(MutableAttributeSet a, float i) {
        a.addAttribute(RightIndent, Float.valueOf(i));
    }

    public static float getLeftIndent(AttributeSet a) {
        Float indent = (Float) a.getAttribute(LeftIndent);
        if (indent != null) {
            return indent.floatValue();
        }
        return 0;
    }

    public static void setLeftIndent(MutableAttributeSet a, float i) {
        a.addAttribute(LeftIndent, Float.valueOf(i));
    }

    /** The line spacing, as a fraction of the line's height; zero if it is not there. */
    public static float getLineSpacing(AttributeSet a) {
        Float space = (Float) a.getAttribute(LineSpacing);
        if (space != null) {
            return space.floatValue();
        }
        return 0;
    }

    public static void setLineSpacing(MutableAttributeSet a, float i) {
        a.addAttribute(LineSpacing, Float.valueOf(i));
    }

    public static float getSpaceAbove(AttributeSet a) {
        Float space = (Float) a.getAttribute(SpaceAbove);
        if (space != null) {
            return space.floatValue();
        }
        return 0;
    }

    public static void setSpaceAbove(MutableAttributeSet a, float i) {
        a.addAttribute(SpaceAbove, Float.valueOf(i));
    }

    public static float getSpaceBelow(AttributeSet a) {
        Float space = (Float) a.getAttribute(SpaceBelow);
        if (space != null) {
            return space.floatValue();
        }
        return 0;
    }

    public static void setSpaceBelow(MutableAttributeSet a, float i) {
        a.addAttribute(SpaceBelow, Float.valueOf(i));
    }

    /** The alignment; to the left if it is not there. */
    public static int getAlignment(AttributeSet a) {
        Integer align = (Integer) a.getAttribute(Alignment);
        if (align != null) {
            return align.intValue();
        }
        return ALIGN_LEFT;
    }

    public static void setAlignment(MutableAttributeSet a, int align) {
        a.addAttribute(Alignment, Integer.valueOf(align));
    }

    /** The paragraph's tab stops, or {@code null}. */
    public static TabSet getTabSet(AttributeSet a) {
        return (TabSet) a.getAttribute(StyleConstants.TabSet);
    }

    public static void setTabSet(MutableAttributeSet a, TabSet tabs) {
        a.addAttribute(StyleConstants.TabSet, tabs);
    }

    /** A character attribute key. */
    public static final class CharacterConstants extends StyleConstants
            implements AttributeSet$CharacterAttribute {

        private CharacterConstants(String representation) {
            super(representation);
        }
    }

    /** A colour key; it is a character one too. */
    public static final class ColorConstants extends StyleConstants
            implements AttributeSet$ColorAttribute, AttributeSet$CharacterAttribute {

        private ColorConstants(String representation) {
            super(representation);
        }
    }

    /** A font key; it is a character one too. */
    public static final class FontConstants extends StyleConstants
            implements AttributeSet$FontAttribute, AttributeSet$CharacterAttribute {

        private FontConstants(String representation) {
            super(representation);
        }
    }

    /** A paragraph attribute key. */
    public static final class ParagraphConstants extends StyleConstants
            implements AttributeSet$ParagraphAttribute {

        private ParagraphConstants(String representation) {
            super(representation);
        }
    }
}
