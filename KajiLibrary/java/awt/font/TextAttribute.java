package java.awt.font;

import java.io.InvalidObjectException;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

/**
 * The keys a text is styled with, attribute by attribute.
 *
 * <p>A {@link java.awt.Font} describes a whole text's style. These attributes describe **one
 * stretch**'s: they hang off an `AttributedCharacterIterator` and hold from such a character to such
 * another, which is how a paragraph with one word in bold is represented.
 *
 * <p>The values are not enumerations but numbers with a meaning. Weight is a multiple of the
 * regular, so {@link #WEIGHT_BOLD} is 2.0 because bold is twice as thick, and nothing stops one
 * asking for 1.6; posture is a tangent, and {@link #POSTURE_OBLIQUE} is 0.20 because that is the
 * angle of the usual italic. The constants are the usual values, not the only possible ones.
 *
 * <p>Each instance is unique: they are always the same ones and they are compared by identity. Hence
 * {@link #readResolve}, which returns the canonical instance when one arrives deserialized.
 */
public final class TextAttribute extends AttributedCharacterIterator.Attribute {

    private static final long serialVersionUID = 7744112784117861702L;

    private static final Map<String, TextAttribute> instanceMap =
            new HashMap<String, TextAttribute>(29);

    /**
     * A new one with that name.
     *
     * <p>It is protected because the attributes that exist are this class's; a subclass adding its
     * own has to be of this very package.
     */
    protected TextAttribute(String name) {
        super(name);
        if (this.getClass() == TextAttribute.class) {
            instanceMap.put(name, this);
        }
    }

    /**
     * The canonical instance corresponding to this name.
     *
     * <p>Without this, a deserialized attribute would be a different object from the one in the
     * constants and identity comparisons would fail silently.
     *
     * @throws InvalidObjectException if the subclass did not override it, or if the name is none of
     *     the known attributes'
     */
    protected Object readResolve() throws InvalidObjectException {
        if (this.getClass() != TextAttribute.class) {
            throw new InvalidObjectException(
                    "subclass didn't correctly implement readResolve");
        }
        TextAttribute instance = instanceMap.get(this.getName());
        if (instance != null) {
            return instance;
        }
        throw new InvalidObjectException("unknown attribute name");
    }

    /** The type family, by name. */
    public static final TextAttribute FAMILY = new TextAttribute("family");

    /** The stroke's weight, as a multiple of the regular. */
    public static final TextAttribute WEIGHT = new TextAttribute("weight");

    /** The glyphs' width, as a multiple of the regular. */
    public static final TextAttribute WIDTH = new TextAttribute("width");

    /** The posture; 0 is upright and 0.20 the usual italic. */
    public static final TextAttribute POSTURE = new TextAttribute("posture");

    /** The size, in points. */
    public static final TextAttribute SIZE = new TextAttribute("size");

    /** An affine transform applied to the glyphs. */
    public static final TextAttribute TRANSFORM = new TextAttribute("transform");

    /** Superscript or subscript. */
    public static final TextAttribute SUPERSCRIPT = new TextAttribute("superscript");

    /** An already built font, which replaces every other attribute. */
    public static final TextAttribute FONT = new TextAttribute("font");

    /** A drawing that takes the character's place. */
    public static final TextAttribute CHAR_REPLACEMENT = new TextAttribute("char_replacement");

    /** What the text is painted with. */
    public static final TextAttribute FOREGROUND = new TextAttribute("foreground");

    /** What the text's background is painted with. */
    public static final TextAttribute BACKGROUND = new TextAttribute("background");

    /** The underline. */
    public static final TextAttribute UNDERLINE = new TextAttribute("underline");

    /** The strikethrough. */
    public static final TextAttribute STRIKETHROUGH = new TextAttribute("strikethrough");

    /** The paragraph's base direction. */
    public static final TextAttribute RUN_DIRECTION = new TextAttribute("run_direction");

    /** The bidirectional embedding level. */
    public static final TextAttribute BIDI_EMBEDDING = new TextAttribute("bidi_embedding");

    /** What part of the slack this stretch absorbs when justifying. */
    public static final TextAttribute JUSTIFICATION = new TextAttribute("justification");

    /** The highlight of the text the input method is still composing. */
    public static final TextAttribute INPUT_METHOD_HIGHLIGHT = new TextAttribute("input method highlight");

    /** The underline of the text the input method is still composing. */
    public static final TextAttribute INPUT_METHOD_UNDERLINE = new TextAttribute("input method underline");

    /** Swaps the text's colour with the background's. */
    public static final TextAttribute SWAP_COLORS = new TextAttribute("swap_colors");

    /** How the digits are drawn according to the language. */
    public static final TextAttribute NUMERIC_SHAPING = new TextAttribute("numeric_shaping");

    /** The fine adjustment of space between pairs of letters. */
    public static final TextAttribute KERNING = new TextAttribute("kerning");

    /** Whether the font's ligatures are used. */
    public static final TextAttribute LIGATURES = new TextAttribute("ligatures");

    /** Space added or taken away between all the letters. */
    public static final TextAttribute TRACKING = new TextAttribute("tracking");

    /** Half the regular weight. */
    public static final Float WEIGHT_EXTRA_LIGHT = Float.valueOf(0.5f);

    /** Light. */
    public static final Float WEIGHT_LIGHT = Float.valueOf(0.75f);

    /** Between light and regular. */
    public static final Float WEIGHT_DEMILIGHT = Float.valueOf(0.875f);

    /** The normal weight. */
    public static final Float WEIGHT_REGULAR = Float.valueOf(1.0f);

    /** Barely thicker than the regular. */
    public static final Float WEIGHT_SEMIBOLD = Float.valueOf(1.25f);

    /** Between regular and bold. */
    public static final Float WEIGHT_MEDIUM = Float.valueOf(1.5f);

    /** Almost bold. */
    public static final Float WEIGHT_DEMIBOLD = Float.valueOf(1.75f);

    /** Twice the regular weight: the usual bold. */
    public static final Float WEIGHT_BOLD = Float.valueOf(2.0f);

    /** More than bold. */
    public static final Float WEIGHT_HEAVY = Float.valueOf(2.25f);

    /** Rather more than bold. */
    public static final Float WEIGHT_EXTRABOLD = Float.valueOf(2.5f);

    /** The heaviest weight provided for. */
    public static final Float WEIGHT_ULTRABOLD = Float.valueOf(2.75f);

    /** Condensed. */
    public static final Float WIDTH_CONDENSED = Float.valueOf(0.75f);

    /** Barely condensed. */
    public static final Float WIDTH_SEMI_CONDENSED = Float.valueOf(0.875f);

    /** The normal width. */
    public static final Float WIDTH_REGULAR = Float.valueOf(1.0f);

    /** Barely extended. */
    public static final Float WIDTH_SEMI_EXTENDED = Float.valueOf(1.25f);

    /** Extended. */
    public static final Float WIDTH_EXTENDED = Float.valueOf(1.5f);

    /** Upright. */
    public static final Float POSTURE_REGULAR = Float.valueOf(0.0f);

    /** The usual italic's posture. */
    public static final Float POSTURE_OBLIQUE = Float.valueOf(0.20f);

    /** One level of superscript. */
    public static final Integer SUPERSCRIPT_SUPER = Integer.valueOf(1);

    /** One level of subscript. */
    public static final Integer SUPERSCRIPT_SUB = Integer.valueOf(-1);

    /** The normal underline. */
    public static final Integer UNDERLINE_ON = Integer.valueOf(0);

    /** Struck through. */
    public static final Boolean STRIKETHROUGH_ON = Boolean.TRUE;

    /** A left-to-right paragraph. */
    public static final Boolean RUN_DIRECTION_LTR = Boolean.FALSE;

    /** A right-to-left paragraph. */
    public static final Boolean RUN_DIRECTION_RTL = Boolean.TRUE;

    /** It absorbs all the slack that falls to it. */
    public static final Float JUSTIFICATION_FULL = Float.valueOf(1.0f);

    /** It does not stretch. */
    public static final Float JUSTIFICATION_NONE = Float.valueOf(0.0f);

    /** A one-pixel underline, for input methods. */
    public static final Integer UNDERLINE_LOW_ONE_PIXEL = Integer.valueOf(1);

    /** A two-pixel underline. */
    public static final Integer UNDERLINE_LOW_TWO_PIXEL = Integer.valueOf(2);

    /** A dotted underline. */
    public static final Integer UNDERLINE_LOW_DOTTED = Integer.valueOf(3);

    /** A grey underline. */
    public static final Integer UNDERLINE_LOW_GRAY = Integer.valueOf(4);

    /** A dashed underline. */
    public static final Integer UNDERLINE_LOW_DASHED = Integer.valueOf(5);

    /** The colours are swapped. */
    public static final Boolean SWAP_COLORS_ON = Boolean.TRUE;

    /** Kerning is applied. */
    public static final Integer KERNING_ON = Integer.valueOf(1);

    /** The ligatures are used. */
    public static final Integer LIGATURES_ON = Integer.valueOf(1);

    /** Tight letters. */
    public static final Float TRACKING_TIGHT = Float.valueOf(-0.04f);

    /** Loose letters. */
    public static final Float TRACKING_LOOSE = Float.valueOf(0.04f);
}
