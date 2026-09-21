package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A font: which family, which style, which size.
 *
 * <p>An object of this class is a **description**, not a font file. It says "Serif, bold, 12
 * points"; the outlines of the letters are somewhere else. That distinction, which in the JDK is
 * invisible because there is always a font engine behind it, is here the line that splits the class
 * in two halves.
 *
 * <p><strong>What comes out of the description works.</strong> The name, the family, the style, the
 * size, the transform, the attributes, all the {@code deriveFont} methods, {@link #decode},
 * equality, and {@link #textRequiresLayout}, which depends on the text and not on the font.
 *
 * <p><strong>What needs the outlines does not.</strong> {@link #getStringBounds}, the {@code
 * createGlyphVector} methods, {@link #canDisplay}, {@link #getNumGlyphs}, {@link #getItalicAngle},
 * {@link #getLineMetrics} and {@link #createFont} throw `UnsupportedOperationException`: without a
 * font engine that reads font files there are no glyphs to measure. Answering anything else —zero,
 * an empty rectangle, an estimated width— would be inventing a number that somebody later uses to
 * lay out a page. A member that is missing is a legal subset; one that lies is not.
 *
 * <p>The size is there twice, as `int` and as `float`. It is not redundancy: {@link #getSize}
 * rounds and has existed since 1.0, {@link #getSize2D} is the real value. A font of 11.5 points
 * says 12 and 11.5, and they are both the right answer to different questions.
 */
public class Font implements Serializable {

    private static final long serialVersionUID = -4206021311591459213L;

    /** The logical family of the system dialog. */
    public static final String DIALOG = "Dialog";

    /** The logical family of text input. */
    public static final String DIALOG_INPUT = "DialogInput";

    /** The logical family without serifs. */
    public static final String SANS_SERIF = "SansSerif";

    /** The logical family with serifs. */
    public static final String SERIF = "Serif";

    /** The logical family of fixed width. */
    public static final String MONOSPACED = "Monospaced";

    /** Neither bold nor italic. */
    public static final int PLAIN = 0;

    /** Bold. */
    public static final int BOLD = 1;

    /** Cursiva. */
    public static final int ITALIC = 2;

    /** The baseline the Latin, Cyrillic and Greek scripts sit on. */
    public static final int ROMAN_BASELINE = 0;

    /** The baseline the ideographic scripts are centred on. */
    public static final int CENTER_BASELINE = 1;

    /** The baseline the Indic scripts hang from. */
    public static final int HANGING_BASELINE = 2;

    /** TrueType or OpenType font format. */
    public static final int TRUETYPE_FONT = 0;

    /** Type 1 font format. */
    public static final int TYPE1_FONT = 1;

    /** The text is laid out from left to right. */
    public static final int LAYOUT_LEFT_TO_RIGHT = 0;

    /** The text is laid out from right to left. */
    public static final int LAYOUT_RIGHT_TO_LEFT = 1;

    /** What is before the stretch does not count as context. */
    public static final int LAYOUT_NO_START_CONTEXT = 2;

    /** What is after the stretch does not count as context. */
    public static final int LAYOUT_NO_LIMIT_CONTEXT = 4;

    /** The logical name of the font. */
    protected String name;

    /** The combination of {@link #BOLD} and {@link #ITALIC}. */
    protected int style;

    /** The size rounded to an integer. */
    protected int size;

    /** The real size. */
    protected float pointSize;

    /** The hash, worked out only once. */
    transient int hash;

    /** The transform of its own, or `null` if it is the identity. */
    private transient AffineTransform transform;

    /** The attributes it was built with, or `null` if it was built by name and style. */
    private transient Map<TextAttribute, Object> attributes;

    /** The first code point from which laying the text out may be needed. */
    private static final int MIN_LAYOUT_CHARCODE = 0x0300;

    /** The last one. */
    private static final int MAX_LAYOUT_CHARCODE = 0x206F;

    /**
     * With a name, a style and a size.
     *
     * <p>A `null` name gives the name `"Default"` —not the dialog family, against what this note
     * used to say— which is what the JDK does; a style that is not a combination of {@link #BOLD}
     * and {@link #ITALIC} is taken as {@link #PLAIN}, without throwing, also as in the JDK.
     */
    public Font(String name, int style, int size) {
        this.name = name == null ? "Default" : name;
        if ((style & ~0x03) == 0) {
            this.style = style;
        } else {
            this.style = 0;
        }
        this.size = size;
        this.pointSize = size;
    }

    /**
     * From a map of attributes.
     *
     * <p>A `null` map is accepted and gives the default font —Dialog, plain, 12— instead of
     * throwing, which is what this note used to promise.
     */
    public Font(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        this.name = "Dialog";
        this.style = 0;
        this.size = 12;
        this.pointSize = 12;
        if (attributes != null) {
            this.applyAttributes(attributes);
        }
    }

    /** Copy; for the subclasses. */
    protected Font(Font font) {
        this.name = font.name;
        this.style = font.style;
        this.size = font.size;
        this.pointSize = font.pointSize;
        this.transform = font.transform;
        if (font.attributes != null) {
            this.attributes = new HashMap<TextAttribute, Object>(font.attributes);
        }
    }

    /** Reads the attributes this class understands and keeps the rest stored as they came. */
    private void applyAttributes(Map<? extends AttributedCharacterIterator.Attribute, ?> attrs) {
        this.attributes = new HashMap<TextAttribute, Object>();
        java.util.Iterator<? extends AttributedCharacterIterator.Attribute> it =
                attrs.keySet().iterator();
        while (it.hasNext()) {
            AttributedCharacterIterator.Attribute key = it.next();
            Object value = attrs.get(key);
            if (!(key instanceof TextAttribute)) {
                continue;
            }
            TextAttribute ta = (TextAttribute) key;
            this.attributes.put(ta, value);
            if (ta == TextAttribute.FAMILY && value instanceof String) {
                this.name = (String) value;
            } else if (ta == TextAttribute.SIZE && value instanceof Number) {
                this.pointSize = ((Number) value).floatValue();
                this.size = (int) (this.pointSize + 0.5f);
            } else if (ta == TextAttribute.WEIGHT && value instanceof Number) {
                // The threshold is the JDK's: from 2.0 up it is bold. A value in between has no way
                // of being expressed in an int style, which has only one bit for it.
                if (((Number) value).floatValue() >= 2.0f) {
                    this.style = this.style | BOLD;
                }
            } else if (ta == TextAttribute.POSTURE && value instanceof Number) {
                if (((Number) value).floatValue() >= 0.2f) {
                    this.style = this.style | ITALIC;
                }
            } else if (ta == TextAttribute.TRANSFORM && value instanceof AffineTransform) {
                AffineTransform at = (AffineTransform) value;
                if (!at.isIdentity()) {
                    this.transform = new AffineTransform(at);
                }
            }
        }
    }

    /**
     * A font from a map of attributes.
     *
     * @throws NullPointerException if the map is `null`
     */
    public static Font getFont(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        Object value = attributes.get(TextAttribute.FONT);
        if (value instanceof Font) {
            return (Font) value;
        }
        return new Font(attributes);
    }

    /**
     * The font that system property names.
     *
     * @throws NullPointerException if the name is `null`
     */
    public static Font getFont(String nm) {
        return getFont(nm, null);
    }

    /**
     * The font that system property names, or `font` if the property is not there.
     *
     * @throws NullPointerException if the name is `null`
     */
    public static Font getFont(String nm, Font font) {
        String str = System.getProperty(nm);
        if (str == null) {
            return font;
        }
        return decode(str);
    }

    /**
     * Reads a font description written as text.
     *
     * <p>The format is `family-STYLE-size`, and it is also accepted with spaces. Whatever is not
     * understood is taken as part of the family name and not as an error: `decode` never fails, so
     * that a misspelled property degrades into a reasonable font instead of breaking the start-up.
     */
    public static Font decode(String str) {
        String fontName = str;
        String styleName = "";
        int fontSize = 12;
        int fontStyle = Font.PLAIN;
        if (str == null) {
            return new Font(DIALOG, fontStyle, fontSize);
        }
        int lastHyphen = str.lastIndexOf('-');
        int lastSpace = str.lastIndexOf(' ');
        char sepChar = lastHyphen > lastSpace ? '-' : ' ';
        int sizeIndex = str.lastIndexOf(sepChar);
        int styleIndex = str.lastIndexOf(sepChar, sizeIndex - 1);
        int strlen = str.length();
        if (sizeIndex > 0 && sizeIndex + 1 < strlen) {
            try {
                fontSize = Integer.valueOf(str.substring(sizeIndex + 1)).intValue();
                if (fontSize <= 0) {
                    fontSize = 12;
                }
            } catch (NumberFormatException e) {
                // It was not a size. If the style had not been found yet, this was the style.
                styleIndex = sizeIndex;
                sizeIndex = strlen;
                if (str.charAt(sizeIndex - 1) == sepChar) {
                    sizeIndex = sizeIndex - 1;
                }
            }
        }
        if (styleIndex >= 0 && styleIndex + 1 < strlen) {
            styleName = str.substring(styleIndex + 1, sizeIndex);
            styleName = styleName.toLowerCase(Locale.ENGLISH);
            if (styleName.equals("bolditalic")) {
                fontStyle = Font.BOLD | Font.ITALIC;
            } else if (styleName.equals("italic")) {
                fontStyle = Font.ITALIC;
            } else if (styleName.equals("bold")) {
                fontStyle = Font.BOLD;
            } else if (styleName.equals("plain")) {
                fontStyle = Font.PLAIN;
            } else {
                // It was none of the known styles: it is part of the name.
                styleIndex = sizeIndex;
                if (str.charAt(styleIndex - 1) == sepChar) {
                    styleIndex = styleIndex - 1;
                }
            }
            fontName = str.substring(0, styleIndex);
        } else {
            int fontEnd = strlen;
            if (styleIndex > 0) {
                fontEnd = styleIndex;
            } else if (sizeIndex > 0) {
                fontEnd = sizeIndex;
            }
            if (fontEnd > 0 && str.charAt(fontEnd - 1) == sepChar) {
                fontEnd = fontEnd - 1;
            }
            fontName = str.substring(0, fontEnd);
        }
        return new Font(fontName, fontStyle, fontSize);
    }

    /**
     * Whether that text needs typographic layout and cannot be drawn character by character.
     *
     * <p>It is a property **of the text**, not of the font: it depends on which scripts show up.
     * The combining diacritics, Hebrew, Arabic, the Indic scripts, Thai, Tibetan, Burmese, Khmer,
     * the direction controls and the surrogates need layout; Latin, Greek, Cyrillic and Armenian do
     * not.
     */
    public static boolean textRequiresLayout(char[] chars, int start, int limit) {
        for (int i = start; i < limit; i++) {
            if (chars[i] < MIN_LAYOUT_CHARCODE) {
                continue;
            }
            if (isComplex(chars[i]) || (chars[i] >= '\uD800' && chars[i] <= '\uDFFF')) {
                return true;
            }
        }
        return false;
    }

    /** Whether that code point belongs to a script that needs layout. */
    private static boolean isComplex(int code) {
        if (code < MIN_LAYOUT_CHARCODE || code > MAX_LAYOUT_CHARCODE) {
            return false;
        }
        if (code <= 0x036F) {
            return true;
        }
        if (code < 0x0590) {
            return false;
        }
        if (code <= 0x06FF) {
            return true;
        }
        if (code < 0x0900) {
            return false;
        }
        if (code <= 0x0E7F) {
            return true;
        }
        if (code < 0x0F00) {
            return false;
        }
        if (code <= 0x0FFF) {
            return true;
        }
        if (code < 0x10A0) {
            return true;
        }
        if (code < 0x1780) {
            return false;
        }
        if (code <= 0x17FF) {
            return true;
        }
        if (code < 0x200C) {
            return false;
        }
        if (code <= 0x200D) {
            return true;
        }
        if (code >= 0x202A && code <= 0x202E) {
            return true;
        }
        return code >= 0x206A && code <= 0x206F;
    }

    /** The transform of its own; the identity if it has none. */
    public AffineTransform getTransform() {
        if (this.transform == null) {
            return new AffineTransform();
        }
        return new AffineTransform(this.transform);
    }

    /** The family. */
    public String getFamily() {
        return this.name;
    }

    /** The family, in the given locale. */
    public String getFamily(Locale l) {
        if (l == null) {
            throw new NullPointerException("null locale doesn't mean default");
        }
        return this.name;
    }

    /**
     * The PostScript name of the font.
     *
     * @throws UnsupportedOperationException always: the PostScript name is inside the font file and
     *     this library has no font engine to read it
     */
    public String getPSName() {
        throw new UnsupportedOperationException("getPSName needs to read the font file; "
                + "this library has no font engine");
    }

    /** The logical name it was asked for with. */
    public String getName() {
        return this.name;
    }

    /** The name of the concrete face. */
    public String getFontName() {
        return this.name;
    }

    /** The name of the concrete face, in the given locale. */
    public String getFontName(Locale l) {
        if (l == null) {
            throw new NullPointerException("null locale doesn't mean default");
        }
        return this.name;
    }

    /** The combination of {@link #BOLD} and {@link #ITALIC}. */
    public int getStyle() {
        return this.style;
    }

    /** The size, rounded to an integer. */
    public int getSize() {
        return this.size;
    }

    /** The real size. */
    public float getSize2D() {
        return this.pointSize;
    }

    /** Whether it is neither bold nor italic. */
    public boolean isPlain() {
        return this.style == 0;
    }

    /** Whether it is bold. */
    public boolean isBold() {
        return (this.style & BOLD) != 0;
    }

    /** Whether it is italic. */
    public boolean isItalic() {
        return (this.style & ITALIC) != 0;
    }

    /** Whether it has a transform other than the identity. */
    public boolean isTransformed() {
        return this.transform != null;
    }

    /**
     * Whether it has attributes that force laying the text out instead of drawing it character by
     * character.
     *
     * <p>They are the ones that change the position or the drawing of the glyphs relative to each
     * other: superscript, underline, strikethrough, colour swapping, character replacement and
     * tracking.
     */
    public boolean hasLayoutAttributes() {
        if (this.attributes == null) {
            return false;
        }
        return this.attributes.containsKey(TextAttribute.SUPERSCRIPT)
                || this.attributes.containsKey(TextAttribute.UNDERLINE)
                || this.attributes.containsKey(TextAttribute.STRIKETHROUGH)
                || this.attributes.containsKey(TextAttribute.SWAP_COLORS)
                || this.attributes.containsKey(TextAttribute.CHAR_REPLACEMENT)
                || this.attributes.containsKey(TextAttribute.TRACKING);
    }

    public int hashCode() {
        if (this.hash == 0) {
            int h = this.name.hashCode() ^ this.style ^ this.size;
            if (this.transform != null) {
                h = h ^ this.transform.hashCode();
            }
            this.hash = h;
        }
        return this.hash;
    }

    /** Equality by name, style, size and transform. */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Font font = (Font) obj;
        if (this.size != font.size || this.style != font.style
                || this.pointSize != font.pointSize) {
            return false;
        }
        if (!this.name.equals(font.name)) {
            return false;
        }
        if (this.transform == null) {
            return font.transform == null;
        }
        return this.transform.equals(font.transform);
    }

    public String toString() {
        String styleName;
        if (this.isBold() && this.isItalic()) {
            styleName = "bolditalic";
        } else if (this.isBold()) {
            styleName = "bold";
        } else if (this.isItalic()) {
            styleName = "italic";
        } else {
            styleName = "plain";
        }
        return this.getClass().getName() + "[family=" + this.getFamily() + ",name=" + this.name
                + ",style=" + styleName + ",size=" + this.size + "]";
    }

    /**
     * How many glyphs the font has.
     *
     * @throws UnsupportedOperationException always: the count is inside the font file and this
     *     library has no font engine to read it
     */
    public int getNumGlyphs() {
        throw new UnsupportedOperationException("getNumGlyphs needs to read the font "
                + "file; this library has no font engine");
    }

    /**
     * The code of the glyph drawn when a character is not there.
     *
     * @throws UnsupportedOperationException always, for the same reason as {@link #getNumGlyphs}
     */
    public int getMissingGlyphCode() {
        throw new UnsupportedOperationException("getMissingGlyphCode needs to read "
                + "the font file; this library has no font engine");
    }

    /**
     * Which baseline that character sits on.
     *
     * <p>This one can be answered without the font: the baseline is a property of the **script**
     * and not of the typeface. The ideographic scripts are centred, the Indic ones hang from a top
     * bar, and the rest sit on the Roman one.
     *
     * <p>The casts to `byte` are explicit because our javac does not yet fold a `static final`
     * constant in assignment context (finding #489); the real javac accepts them without a cast.
     */
    public byte getBaselineFor(char c) {
        if (c < 0x0900) {
            return (byte) ROMAN_BASELINE;
        }
        // Devanagari, Bengali, Gurmukhi, Gujarati, Oriya, Tamil, Telugu, Kannada, Malayalam.
        if (c <= 0x0D7F) {
            return (byte) HANGING_BASELINE;
        }
        // Tibetano.
        if (c >= 0x0F00 && c <= 0x0FFF) {
            return (byte) HANGING_BASELINE;
        }
        // Han, hiragana, katakana, hangul and the full-width punctuation.
        if (c >= 0x2E80 && c <= 0xD7AF) {
            return (byte) CENTER_BASELINE;
        }
        if (c >= 0xF900 && c <= 0xFAFF) {
            return (byte) CENTER_BASELINE;
        }
        if (c >= 0xFF00 && c <= 0xFF60) {
            return (byte) CENTER_BASELINE;
        }
        return (byte) ROMAN_BASELINE;
    }

    /**
     * The attributes of this font.
     *
     * <p>If it was built from a map, that one is returned; if it was built by name and style, one
     * is built with what the description says.
     */
    public Map<TextAttribute, ?> getAttributes() {
        Map<TextAttribute, Object> out = new HashMap<TextAttribute, Object>();
        if (this.attributes != null) {
            out.putAll(this.attributes);
            return out;
        }
        out.put(TextAttribute.FAMILY, this.name);
        out.put(TextAttribute.SIZE, Float.valueOf(this.pointSize));
        if (this.isBold()) {
            out.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        } else {
            out.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        }
        if (this.isItalic()) {
            out.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        } else {
            out.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);
        }
        if (this.transform != null) {
            out.put(TextAttribute.TRANSFORM, new AffineTransform(this.transform));
        }
        return out;
    }

    /** The attributes a font can have. */
    public AttributedCharacterIterator.Attribute[] getAvailableAttributes() {
        AttributedCharacterIterator.Attribute[] attributes = {
            TextAttribute.FAMILY,
            TextAttribute.WEIGHT,
            TextAttribute.WIDTH,
            TextAttribute.POSTURE,
            TextAttribute.SIZE,
            TextAttribute.TRANSFORM,
            TextAttribute.SUPERSCRIPT,
            TextAttribute.CHAR_REPLACEMENT,
        };
        return attributes;
    }

    /** The same one with another style and another size. */
    public Font deriveFont(int style, float size) {
        Font f = new Font(this);
        if ((style & ~0x03) == 0) {
            f.style = style;
        } else {
            f.style = 0;
        }
        f.pointSize = size;
        f.size = (int) (size + 0.5f);
        f.hash = 0;
        return f;
    }

    /** The same one with another style and another transform. */
    public Font deriveFont(int style, AffineTransform trans) {
        if (trans == null) {
            throw new IllegalArgumentException("transform must not be null");
        }
        Font f = new Font(this);
        if ((style & ~0x03) == 0) {
            f.style = style;
        } else {
            f.style = 0;
        }
        if (trans.isIdentity()) {
            f.transform = null;
        } else {
            f.transform = new AffineTransform(trans);
        }
        f.hash = 0;
        return f;
    }

    /** The same one with another size. */
    public Font deriveFont(float size) {
        Font f = new Font(this);
        f.pointSize = size;
        f.size = (int) (size + 0.5f);
        f.hash = 0;
        return f;
    }

    /**
     * The same one with another transform.
     *
     * @throws IllegalArgumentException if the transform is `null`
     */
    public Font deriveFont(AffineTransform trans) {
        if (trans == null) {
            throw new IllegalArgumentException("transform must not be null");
        }
        Font f = new Font(this);
        if (trans.isIdentity()) {
            f.transform = null;
        } else {
            f.transform = new AffineTransform(trans);
        }
        f.hash = 0;
        return f;
    }

    /** The same one with another style. */
    public Font deriveFont(int style) {
        Font f = new Font(this);
        if ((style & ~0x03) == 0) {
            f.style = style;
        } else {
            f.style = 0;
        }
        f.hash = 0;
        return f;
    }

    /**
     * The same one with those attributes on top.
     *
     * <p>A `null` map is accepted and changes nothing, against what this note used to promise.
     */
    public Font deriveFont(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        Font f = new Font(this);
        if (attributes != null) {
            Map<TextAttribute, Object> merged = new HashMap<TextAttribute, Object>();
            if (f.attributes != null) {
                merged.putAll(f.attributes);
            }
            f.attributes = merged;
            f.applyOver(attributes);
        }
        f.hash = 0;
        return f;
    }

    /** Applies attributes over the ones already there, without erasing those not mentioned. */
    private void applyOver(Map<? extends AttributedCharacterIterator.Attribute, ?> attrs) {
        Map<TextAttribute, Object> previous = this.attributes;
        this.applyAttributes(attrs);
        Map<TextAttribute, Object> fresh = this.attributes;
        if (previous != null) {
            Map<TextAttribute, Object> merged = new HashMap<TextAttribute, Object>(previous);
            merged.putAll(fresh);
            this.attributes = merged;
        }
    }

    /**
     * Whether the font can draw that character.
     *
     * @throws UnsupportedOperationException always: which characters a font covers is in its
     *     character map, inside the file
     */
    public boolean canDisplay(char c) {
        throw new UnsupportedOperationException("canDisplay needs the character table of the font "
                + "file; this library has no font engine");
    }

    /**
     * Whether the font can draw that code point.
     *
     * @throws UnsupportedOperationException always, for the same reason as {@link #canDisplay}
     */
    public boolean canDisplay(int codePoint) {
        throw new UnsupportedOperationException("canDisplay needs the character table of the font "
                + "file; this library has no font engine");
    }

    /**
     * How far into that string the font can draw.
     *
     * @throws UnsupportedOperationException always, for the same reason as {@link #canDisplay}
     */
    public int canDisplayUpTo(String str) {
        throw new UnsupportedOperationException("canDisplayUpTo needs the character table of "
                + "the font file; this library has no font engine");
    }

    /**
     * How far into that stretch the font can draw.
     *
     * @throws UnsupportedOperationException always, for the same reason as {@link #canDisplay}
     */
    public int canDisplayUpTo(char[] text, int start, int limit) {
        throw new UnsupportedOperationException("canDisplayUpTo needs the character table of "
                + "the font file; this library has no font engine");
    }

    /**
     * How far into that iterator the font can draw.
     *
     * @throws UnsupportedOperationException always, for the same reason as {@link #canDisplay}
     */
    public int canDisplayUpTo(CharacterIterator iter, int start, int limit) {
        throw new UnsupportedOperationException("canDisplayUpTo needs the character table of "
                + "the font file; this library has no font engine");
    }

    /**
     * The slant angle of the italic.
     *
     * @throws UnsupportedOperationException always: the angle is declared inside the font file
     */
    public float getItalicAngle() {
        throw new UnsupportedOperationException("getItalicAngle needs to read the font "
                + "file; this library has no font engine");
    }

    /**
     * Whether all the characters share the same line measures.
     *
     * @throws UnsupportedOperationException always: it depends on the metrics of the font
     */
    public boolean hasUniformLineMetrics() {
        throw new UnsupportedOperationException("hasUniformLineMetrics needs the metrics of "
                + "the font file; this library has no font engine");
    }

    /** The message of the measures that need the glyphs. */
    private static UnsupportedOperationException noMetrics(String method) {
        return new UnsupportedOperationException(method + " needs to measure the glyphs of "
                + "the font file; this library has no font engine");
    }

    /**
     * The vertical measures of that string.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public LineMetrics getLineMetrics(String str, FontRenderContext frc) {
        throw noMetrics("getLineMetrics");
    }

    /**
     * The vertical measures of a stretch of that string.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public LineMetrics getLineMetrics(String str, int beginIndex, int limit,
            FontRenderContext frc) {
        throw noMetrics("getLineMetrics");
    }

    /**
     * The vertical measures of a stretch of characters.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit,
            FontRenderContext frc) {
        throw noMetrics("getLineMetrics");
    }

    /**
     * The vertical measures of a stretch of an iterator.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit,
            FontRenderContext frc) {
        throw noMetrics("getLineMetrics");
    }

    /**
     * The rectangle that string takes up.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public Rectangle2D getStringBounds(String str, FontRenderContext frc) {
        throw noMetrics("getStringBounds");
    }

    /**
     * The rectangle a stretch of that string takes up.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public Rectangle2D getStringBounds(String str, int beginIndex, int limit,
            FontRenderContext frc) {
        throw noMetrics("getStringBounds");
    }

    /**
     * The rectangle a stretch of characters takes up.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit,
            FontRenderContext frc) {
        throw noMetrics("getStringBounds");
    }

    /**
     * The rectangle a stretch of an iterator takes up.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit,
            FontRenderContext frc) {
        throw noMetrics("getStringBounds");
    }

    /**
     * The rectangle of the biggest character of the font.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public Rectangle2D getMaxCharBounds(FontRenderContext frc) {
        throw noMetrics("getMaxCharBounds");
    }

    /** The message of the operations that need the outlines of the glyphs. */
    private static UnsupportedOperationException noGlyphs(String method) {
        return new UnsupportedOperationException(method + " needs the glyph outlines; "
                + "this library has no font engine");
    }

    /**
     * The glyphs of that string, one per character.
     *
     * @throws UnsupportedOperationException always: the outlines are needed
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, String str) {
        throw noGlyphs("createGlyphVector");
    }

    /**
     * The glyphs of those characters.
     *
     * @throws UnsupportedOperationException always: the outlines are needed
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, char[] chars) {
        throw noGlyphs("createGlyphVector");
    }

    /**
     * The glyphs of that iterator.
     *
     * @throws UnsupportedOperationException always: the outlines are needed
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, CharacterIterator ci) {
        throw noGlyphs("createGlyphVector");
    }

    /**
     * The glyphs of those codes.
     *
     * @throws UnsupportedOperationException always: the outlines are needed
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, int[] glyphCodes) {
        throw noGlyphs("createGlyphVector");
    }

    /**
     * The glyphs of that text, laid out with reordering and ligatures.
     *
     * @throws UnsupportedOperationException always: the outlines and the layout tables of the font
     *     are needed
     */
    public GlyphVector layoutGlyphVector(FontRenderContext frc, char[] text, int start, int limit,
            int flags) {
        throw noGlyphs("layoutGlyphVector");
    }

    /** The message of the reading of font files. */
    private static UnsupportedOperationException noReader() {
        return new UnsupportedOperationException("creating a font from a file needs a "
                + "TrueType and Type 1 reader; this library has no font engine");
    }

    /**
     * Reads a font from a stream.
     *
     * @throws UnsupportedOperationException always: a reader of font files is needed
     */
    public static Font createFont(int fontFormat, InputStream fontStream)
            throws FontFormatException, IOException {
        throw noReader();
    }

    /**
     * Reads a font from a file.
     *
     * @throws UnsupportedOperationException always: a reader of font files is needed
     */
    public static Font createFont(int fontFormat, File fontFile)
            throws FontFormatException, IOException {
        throw noReader();
    }

    /**
     * Reads every font in a stream.
     *
     * @throws UnsupportedOperationException always: a reader of font files is needed
     */
    public static Font[] createFonts(InputStream fontStream)
            throws FontFormatException, IOException {
        throw noReader();
    }

    /**
     * Reads every font in a file.
     *
     * @throws UnsupportedOperationException always: a reader of font files is needed
     */
    public static Font[] createFonts(File fontFile) throws FontFormatException, IOException {
        throw noReader();
    }
}
