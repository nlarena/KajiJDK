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
 * Una tipografía: qué familia, qué estilo, qué cuerpo.
 *
 * <p>Un objeto de esta clase es una **descripción**, no un archivo de fuente. Dice "Serif, negrita,
 * 12 puntos"; los contornos de las letras están en otro lado. Esa distinción, que en el JDK es
 * invisible porque siempre hay un motor tipográfico detrás, acá es la línea que divide la clase en
 * dos mitades.
 *
 * <p><strong>Lo que sale de la descripción funciona.</strong> El nombre, la familia, el estilo, el
 * cuerpo, la transformación, los atributos, todos los {@code deriveFont}, {@link #decode}, la
 * igualdad, y {@link #textRequiresLayout}, que depende del texto y no de la fuente.
 *
 * <p><strong>Lo que necesita los contornos, no.</strong> {@link #getStringBounds}, los
 * {@code createGlyphVector}, {@link #canDisplay}, {@link #getNumGlyphs}, {@link #getItalicAngle},
 * {@link #getLineMetrics} y {@link #createFont} tiran `UnsupportedOperationException`: sin un motor
 * tipográfico que lea archivos de fuente no hay glifos que medir. Contestar cualquier otra cosa
 * —cero, un rectángulo vacío, un ancho estimado— sería inventar un número que después alguien usa
 * para maquetar. Un miembro que falta es un subconjunto legal; uno que miente, no.
 *
 * <p>El cuerpo está dos veces, como `int` y como `float`. No es redundancia: {@link #getSize}
 * redondea y existe desde 1.0, {@link #getSize2D} es el valor real. Una fuente de 11,5 puntos dice
 * 12 y 11.5, y son las dos respuestas correctas a preguntas distintas.
 */
public class Font implements Serializable {

    private static final long serialVersionUID = -4206021311591459213L;

    /** La familia lógica del diálogo del sistema. */
    public static final String DIALOG = "Dialog";

    /** La familia lógica de la entrada de texto. */
    public static final String DIALOG_INPUT = "DialogInput";

    /** La familia lógica sin remates. */
    public static final String SANS_SERIF = "SansSerif";

    /** La familia lógica con remates. */
    public static final String SERIF = "Serif";

    /** La familia lógica de ancho fijo. */
    public static final String MONOSPACED = "Monospaced";

    /** Ni negrita ni cursiva. */
    public static final int PLAIN = 0;

    /** Negrita. */
    public static final int BOLD = 1;

    /** Cursiva. */
    public static final int ITALIC = 2;

    /** La línea de base sobre la que se apoyan las escrituras latina, cirílica y griega. */
    public static final int ROMAN_BASELINE = 0;

    /** La línea de base sobre la que se centran las escrituras ideográficas. */
    public static final int CENTER_BASELINE = 1;

    /** La línea de base de la que cuelgan las escrituras índicas. */
    public static final int HANGING_BASELINE = 2;

    /** Formato de fuente TrueType u OpenType. */
    public static final int TRUETYPE_FONT = 0;

    /** Formato de fuente Type 1. */
    public static final int TYPE1_FONT = 1;

    /** El texto se arma de izquierda a derecha. */
    public static final int LAYOUT_LEFT_TO_RIGHT = 0;

    /** El texto se arma de derecha a izquierda. */
    public static final int LAYOUT_RIGHT_TO_LEFT = 1;

    /** Lo que está antes del tramo no cuenta como contexto. */
    public static final int LAYOUT_NO_START_CONTEXT = 2;

    /** Lo que está después del tramo no cuenta como contexto. */
    public static final int LAYOUT_NO_LIMIT_CONTEXT = 4;

    /** El nombre lógico de la fuente. */
    protected String name;

    /** La combinación de {@link #BOLD} e {@link #ITALIC}. */
    protected int style;

    /** El cuerpo redondeado a entero. */
    protected int size;

    /** El cuerpo real. */
    protected float pointSize;

    /** El hash, calculado una sola vez. */
    transient int hash;

    /** La transformación propia, o `null` si es la identidad. */
    private transient AffineTransform transform;

    /** Los atributos con los que se armó, o `null` si se armó por nombre y estilo. */
    private transient Map<TextAttribute, Object> attributes;

    /** El primer punto de código a partir del cual puede hacer falta armar el texto. */
    private static final int MIN_LAYOUT_CHARCODE = 0x0300;

    /** El último. */
    private static final int MAX_LAYOUT_CHARCODE = 0x206F;

    /**
     * Con nombre, estilo y cuerpo.
     *
     * <p>Un nombre `null` da la familia del diálogo; un estilo que no sea una combinación de
     * {@link #BOLD} e {@link #ITALIC} se toma como {@link #PLAIN}, sin tirar, igual que en el JDK.
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
     * A partir de un mapa de atributos.
     *
     * @throws NullPointerException si el mapa es `null`
     */
    public Font(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        this.name = "Dialog";
        this.style = 0;
        this.size = 12;
        this.pointSize = 12;
        if (attributes != null) {
            this.aplicar(attributes);
        }
    }

    /** Copia; para las subclases. */
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

    /** Lee los atributos que esta clase entiende y deja el resto guardado tal cual. */
    private void aplicar(Map<? extends AttributedCharacterIterator.Attribute, ?> attrs) {
        this.attributes = new HashMap<TextAttribute, Object>();
        java.util.Iterator<? extends AttributedCharacterIterator.Attribute> it =
                attrs.keySet().iterator();
        while (it.hasNext()) {
            AttributedCharacterIterator.Attribute clave = it.next();
            Object valor = attrs.get(clave);
            if (!(clave instanceof TextAttribute)) {
                continue;
            }
            TextAttribute ta = (TextAttribute) clave;
            this.attributes.put(ta, valor);
            if (ta == TextAttribute.FAMILY && valor instanceof String) {
                this.name = (String) valor;
            } else if (ta == TextAttribute.SIZE && valor instanceof Number) {
                this.pointSize = ((Number) valor).floatValue();
                this.size = (int) (this.pointSize + 0.5f);
            } else if (ta == TextAttribute.WEIGHT && valor instanceof Number) {
                // El umbral es el del JDK: de 2.0 para arriba es negrita. Un valor intermedio no
                // tiene forma de expresarse en el estilo de un int, que sólo tiene un bit.
                if (((Number) valor).floatValue() >= 2.0f) {
                    this.style = this.style | BOLD;
                }
            } else if (ta == TextAttribute.POSTURE && valor instanceof Number) {
                if (((Number) valor).floatValue() >= 0.2f) {
                    this.style = this.style | ITALIC;
                }
            } else if (ta == TextAttribute.TRANSFORM && valor instanceof AffineTransform) {
                AffineTransform at = (AffineTransform) valor;
                if (!at.isIdentity()) {
                    this.transform = new AffineTransform(at);
                }
            }
        }
    }

    /**
     * Una fuente a partir de un mapa de atributos.
     *
     * @throws NullPointerException si el mapa es `null`
     */
    public static Font getFont(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        Object valor = attributes.get(TextAttribute.FONT);
        if (valor instanceof Font) {
            return (Font) valor;
        }
        return new Font(attributes);
    }

    /**
     * La fuente que nombra esa propiedad del sistema.
     *
     * @throws NullPointerException si el nombre es `null`
     */
    public static Font getFont(String nm) {
        return getFont(nm, null);
    }

    /**
     * La fuente que nombra esa propiedad del sistema, o `font` si la propiedad no está.
     *
     * @throws NullPointerException si el nombre es `null`
     */
    public static Font getFont(String nm, Font font) {
        String str = System.getProperty(nm);
        if (str == null) {
            return font;
        }
        return decode(str);
    }

    /**
     * Lee una descripción de fuente en texto.
     *
     * <p>El formato es `familia-ESTILO-cuerpo`, y también se admite con espacios. Lo que no se
     * entienda se toma como parte del nombre de la familia y no como un error: `decode` no falla
     * nunca, para que una propiedad mal escrita degrade en una fuente razonable en vez de romper el
     * arranque.
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
                // No era un cuerpo. Si todavia no se habia encontrado el estilo, esto era el estilo.
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
                // No era ninguno de los estilos conocidos: es parte del nombre.
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
     * Si ese texto necesita armado tipográfico y no se puede dibujar carácter por carácter.
     *
     * <p>Es una propiedad **del texto**, no de la fuente: depende de qué escrituras aparecen. Los
     * diacríticos combinantes, el hebreo, el árabe, las escrituras índicas, el tailandés, el
     * tibetano, el birmano, el jemer, los controles de dirección y los sustitutos necesitan armado;
     * el latín, el griego, el cirílico y el armenio, no.
     */
    public static boolean textRequiresLayout(char[] chars, int start, int limit) {
        for (int i = start; i < limit; i++) {
            if (chars[i] < MIN_LAYOUT_CHARCODE) {
                continue;
            }
            if (esComplejo(chars[i]) || (chars[i] >= '\uD800' && chars[i] <= '\uDFFF')) {
                return true;
            }
        }
        return false;
    }

    /** Si ese punto de código pertenece a una escritura que necesita armado. */
    private static boolean esComplejo(int code) {
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

    /** La transformación propia; la identidad si no tiene. */
    public AffineTransform getTransform() {
        if (this.transform == null) {
            return new AffineTransform();
        }
        return new AffineTransform(this.transform);
    }

    /** La familia. */
    public String getFamily() {
        return this.name;
    }

    /** La familia, en el idioma dado. */
    public String getFamily(Locale l) {
        if (l == null) {
            throw new NullPointerException("null locale doesn't mean default");
        }
        return this.name;
    }

    /**
     * El nombre PostScript de la fuente.
     *
     * @throws UnsupportedOperationException siempre: el nombre PostScript está adentro del archivo
     *     de la fuente y esta biblioteca no trae un motor tipográfico que lo lea
     */
    public String getPSName() {
        throw new UnsupportedOperationException("getPSName requiere leer el archivo de la fuente; "
                + "esta biblioteca no trae motor tipográfico");
    }

    /** El nombre lógico con el que se pidió. */
    public String getName() {
        return this.name;
    }

    /** El nombre de la cara concreta. */
    public String getFontName() {
        return this.name;
    }

    /** El nombre de la cara concreta, en el idioma dado. */
    public String getFontName(Locale l) {
        if (l == null) {
            throw new NullPointerException("null locale doesn't mean default");
        }
        return this.name;
    }

    /** La combinación de {@link #BOLD} e {@link #ITALIC}. */
    public int getStyle() {
        return this.style;
    }

    /** El cuerpo, redondeado a entero. */
    public int getSize() {
        return this.size;
    }

    /** El cuerpo real. */
    public float getSize2D() {
        return this.pointSize;
    }

    /** Si no es ni negrita ni cursiva. */
    public boolean isPlain() {
        return this.style == 0;
    }

    /** Si es negrita. */
    public boolean isBold() {
        return (this.style & BOLD) != 0;
    }

    /** Si es cursiva. */
    public boolean isItalic() {
        return (this.style & ITALIC) != 0;
    }

    /** Si tiene una transformación que no sea la identidad. */
    public boolean isTransformed() {
        return this.transform != null;
    }

    /**
     * Si tiene atributos que obligan a armar el texto en vez de dibujarlo carácter por carácter.
     *
     * <p>Son los que cambian la posición o el dibujo de los glifos entre sí: el volado, el
     * subrayado, el tachado, el intercambio de colores, el reemplazo de carácter y el espaciado.
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

    /** Igualdad por nombre, estilo, cuerpo y transformación. */
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
        String estilo;
        if (this.isBold() && this.isItalic()) {
            estilo = "bolditalic";
        } else if (this.isBold()) {
            estilo = "bold";
        } else if (this.isItalic()) {
            estilo = "italic";
        } else {
            estilo = "plain";
        }
        return this.getClass().getName() + "[family=" + this.getFamily() + ",name=" + this.name
                + ",style=" + estilo + ",size=" + this.size + "]";
    }

    /**
     * Cuántos glifos tiene la fuente.
     *
     * @throws UnsupportedOperationException siempre: la cuenta está adentro del archivo de la
     *     fuente y esta biblioteca no trae un motor tipográfico que lo lea
     */
    public int getNumGlyphs() {
        throw new UnsupportedOperationException("getNumGlyphs requiere leer el archivo de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * El código del glifo que se dibuja cuando un carácter no está.
     *
     * @throws UnsupportedOperationException siempre, por el mismo motivo que {@link #getNumGlyphs}
     */
    public int getMissingGlyphCode() {
        throw new UnsupportedOperationException("getMissingGlyphCode requiere leer el archivo de "
                + "la fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Sobre qué línea de base se apoya ese carácter.
     *
     * <p>Ésta sí se puede contestar sin la fuente: la línea de base es una propiedad de la
     * **escritura** y no de la tipografía. Las escrituras ideográficas se centran, las índicas
     * cuelgan de una barra superior, y el resto se apoya en la romana.
     *
     * <p>Las conversiones a `byte` son explícitas porque nuestro javac todavía no pliega una
     * constante `static final` en contexto de asignación (hallazgo #489); el javac real las acepta
     * sin conversión.
     */
    public byte getBaselineFor(char c) {
        if (c < 0x0900) {
            return (byte) ROMAN_BASELINE;
        }
        // Devanagari, bengalí, gurmukhi, guyaratí, oriya, tamil, telugu, canarés, malayalam.
        if (c <= 0x0D7F) {
            return (byte) HANGING_BASELINE;
        }
        // Tibetano.
        if (c >= 0x0F00 && c <= 0x0FFF) {
            return (byte) HANGING_BASELINE;
        }
        // Han, hiragana, katakana, hangul y la puntuación de ancho completo.
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
     * Los atributos de esta fuente.
     *
     * <p>Si se armó a partir de un mapa, se devuelve ése; si se armó por nombre y estilo, se arma
     * uno con lo que la descripción dice.
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

    /** Los atributos que una fuente puede tener. */
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

    /** La misma con otro estilo y otro cuerpo. */
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

    /** La misma con otro estilo y otra transformación. */
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

    /** La misma con otro cuerpo. */
    public Font deriveFont(float size) {
        Font f = new Font(this);
        f.pointSize = size;
        f.size = (int) (size + 0.5f);
        f.hash = 0;
        return f;
    }

    /**
     * La misma con otra transformación.
     *
     * @throws IllegalArgumentException si la transformación es `null`
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

    /** La misma con otro estilo. */
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
     * La misma con esos atributos encima.
     *
     * @throws NullPointerException si el mapa es `null`
     */
    public Font deriveFont(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        Font f = new Font(this);
        if (attributes != null) {
            Map<TextAttribute, Object> juntos = new HashMap<TextAttribute, Object>();
            if (f.attributes != null) {
                juntos.putAll(f.attributes);
            }
            f.attributes = juntos;
            f.aplicarEncima(attributes);
        }
        f.hash = 0;
        return f;
    }

    /** Aplica atributos sobre los que ya hay, sin borrar los que no se mencionan. */
    private void aplicarEncima(Map<? extends AttributedCharacterIterator.Attribute, ?> attrs) {
        Map<TextAttribute, Object> previos = this.attributes;
        this.aplicar(attrs);
        Map<TextAttribute, Object> nuevos = this.attributes;
        if (previos != null) {
            Map<TextAttribute, Object> juntos = new HashMap<TextAttribute, Object>(previos);
            juntos.putAll(nuevos);
            this.attributes = juntos;
        }
    }

    /**
     * Si la fuente puede dibujar ese carácter.
     *
     * @throws UnsupportedOperationException siempre: qué caracteres cubre una fuente está en su
     *     tabla de correspondencias, adentro del archivo
     */
    public boolean canDisplay(char c) {
        throw new UnsupportedOperationException("canDisplay requiere la tabla de caracteres de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Si la fuente puede dibujar ese punto de código.
     *
     * @throws UnsupportedOperationException siempre, por el mismo motivo que {@link #canDisplay}
     */
    public boolean canDisplay(int codePoint) {
        throw new UnsupportedOperationException("canDisplay requiere la tabla de caracteres de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Hasta dónde de esa cadena puede dibujar la fuente.
     *
     * @throws UnsupportedOperationException siempre, por el mismo motivo que {@link #canDisplay}
     */
    public int canDisplayUpTo(String str) {
        throw new UnsupportedOperationException("canDisplayUpTo requiere la tabla de caracteres de "
                + "la fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Hasta dónde de ese tramo puede dibujar la fuente.
     *
     * @throws UnsupportedOperationException siempre, por el mismo motivo que {@link #canDisplay}
     */
    public int canDisplayUpTo(char[] text, int start, int limit) {
        throw new UnsupportedOperationException("canDisplayUpTo requiere la tabla de caracteres de "
                + "la fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Hasta dónde de ese iterador puede dibujar la fuente.
     *
     * @throws UnsupportedOperationException siempre, por el mismo motivo que {@link #canDisplay}
     */
    public int canDisplayUpTo(CharacterIterator iter, int start, int limit) {
        throw new UnsupportedOperationException("canDisplayUpTo requiere la tabla de caracteres de "
                + "la fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * El ángulo de inclinación de la cursiva.
     *
     * @throws UnsupportedOperationException siempre: el ángulo está declarado adentro del archivo de
     *     la fuente
     */
    public float getItalicAngle() {
        throw new UnsupportedOperationException("getItalicAngle requiere leer el archivo de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Si todos los caracteres comparten las mismas medidas de renglón.
     *
     * @throws UnsupportedOperationException siempre: depende de las métricas de la fuente
     */
    public boolean hasUniformLineMetrics() {
        throw new UnsupportedOperationException("hasUniformLineMetrics requiere las métricas de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /** El mensaje de las medidas que necesitan los glifos. */
    private static UnsupportedOperationException sinMetrica(String metodo) {
        return new UnsupportedOperationException(metodo + " requiere medir los glifos de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Las medidas verticales de esa cadena.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public LineMetrics getLineMetrics(String str, FontRenderContext frc) {
        throw sinMetrica("getLineMetrics");
    }

    /**
     * Las medidas verticales de un tramo de esa cadena.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public LineMetrics getLineMetrics(String str, int beginIndex, int limit,
            FontRenderContext frc) {
        throw sinMetrica("getLineMetrics");
    }

    /**
     * Las medidas verticales de un tramo de caracteres.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit,
            FontRenderContext frc) {
        throw sinMetrica("getLineMetrics");
    }

    /**
     * Las medidas verticales de un tramo de un iterador.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit,
            FontRenderContext frc) {
        throw sinMetrica("getLineMetrics");
    }

    /**
     * El rectángulo que ocupa esa cadena.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public Rectangle2D getStringBounds(String str, FontRenderContext frc) {
        throw sinMetrica("getStringBounds");
    }

    /**
     * El rectángulo que ocupa un tramo de esa cadena.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public Rectangle2D getStringBounds(String str, int beginIndex, int limit,
            FontRenderContext frc) {
        throw sinMetrica("getStringBounds");
    }

    /**
     * El rectángulo que ocupa un tramo de caracteres.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit,
            FontRenderContext frc) {
        throw sinMetrica("getStringBounds");
    }

    /**
     * El rectángulo que ocupa un tramo de un iterador.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit,
            FontRenderContext frc) {
        throw sinMetrica("getStringBounds");
    }

    /**
     * El rectángulo del carácter más grande de la fuente.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public Rectangle2D getMaxCharBounds(FontRenderContext frc) {
        throw sinMetrica("getMaxCharBounds");
    }

    /** El mensaje de las operaciones que necesitan los contornos de los glifos. */
    private static UnsupportedOperationException sinGlifos(String metodo) {
        return new UnsupportedOperationException(metodo + " requiere los contornos de los glifos; "
                + "esta biblioteca no trae motor tipográfico");
    }

    /**
     * Los glifos de esa cadena, uno por carácter.
     *
     * @throws UnsupportedOperationException siempre: hacen falta los contornos
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, String str) {
        throw sinGlifos("createGlyphVector");
    }

    /**
     * Los glifos de esos caracteres.
     *
     * @throws UnsupportedOperationException siempre: hacen falta los contornos
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, char[] chars) {
        throw sinGlifos("createGlyphVector");
    }

    /**
     * Los glifos de ese iterador.
     *
     * @throws UnsupportedOperationException siempre: hacen falta los contornos
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, CharacterIterator ci) {
        throw sinGlifos("createGlyphVector");
    }

    /**
     * Los glifos de esos códigos.
     *
     * @throws UnsupportedOperationException siempre: hacen falta los contornos
     */
    public GlyphVector createGlyphVector(FontRenderContext frc, int[] glyphCodes) {
        throw sinGlifos("createGlyphVector");
    }

    /**
     * Los glifos de ese texto, armados con reordenamiento y ligaduras.
     *
     * @throws UnsupportedOperationException siempre: hacen falta los contornos y las tablas de
     *     armado de la fuente
     */
    public GlyphVector layoutGlyphVector(FontRenderContext frc, char[] text, int start, int limit,
            int flags) {
        throw sinGlifos("layoutGlyphVector");
    }

    /** El mensaje de la lectura de archivos de fuente. */
    private static UnsupportedOperationException sinLector() {
        return new UnsupportedOperationException("crear una fuente desde un archivo requiere un "
                + "lector de TrueType y Type 1; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Lee una fuente de un flujo.
     *
     * @throws UnsupportedOperationException siempre: hace falta un lector de archivos de fuente
     */
    public static Font createFont(int fontFormat, InputStream fontStream)
            throws FontFormatException, IOException {
        throw sinLector();
    }

    /**
     * Lee una fuente de un archivo.
     *
     * @throws UnsupportedOperationException siempre: hace falta un lector de archivos de fuente
     */
    public static Font createFont(int fontFormat, File fontFile)
            throws FontFormatException, IOException {
        throw sinLector();
    }

    /**
     * Lee todas las fuentes de un flujo.
     *
     * @throws UnsupportedOperationException siempre: hace falta un lector de archivos de fuente
     */
    public static Font[] createFonts(InputStream fontStream)
            throws FontFormatException, IOException {
        throw sinLector();
    }

    /**
     * Lee todas las fuentes de un archivo.
     *
     * @throws UnsupportedOperationException siempre: hace falta un lector de archivos de fuente
     */
    public static Font[] createFonts(File fontFile) throws FontFormatException, IOException {
        throw sinLector();
    }
}
