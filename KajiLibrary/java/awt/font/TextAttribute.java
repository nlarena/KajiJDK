package java.awt.font;

import java.io.InvalidObjectException;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

/**
 * Las claves con las que se le pone estilo a un texto atributo por atributo.
 *
 * <p>Un {@link java.awt.Font} describe el estilo de un texto entero. Estos atributos describen el de
 * **un tramo**: se cuelgan de un `AttributedCharacterIterator` y valen desde tal carácter hasta tal
 * otro, que es como se representa un párrafo con una palabra en negrita.
 *
 * <p>Los valores no son enumeraciones sino números con significado. El grosor es un múltiplo del
 * regular, así que {@link #WEIGHT_BOLD} vale 2.0 porque la negrita es el doble de gruesa, y nada
 * impide pedir 1.6; la inclinación es una tangente, y {@link #POSTURE_OBLIQUE} vale 0,20 porque ése
 * es el ángulo de la cursiva de siempre. Las constantes son los valores usuales, no los únicos
 * posibles.
 *
 * <p>Cada instancia es única: son las mismas de siempre y se comparan por identidad. Por eso hay
 * {@link #readResolve}, que devuelve la instancia canónica cuando una llega deserializada.
 */
public final class TextAttribute extends AttributedCharacterIterator.Attribute {

    private static final long serialVersionUID = 7744112784117861702L;

    private static final Map<String, TextAttribute> instanceMap =
            new HashMap<String, TextAttribute>(29);

    /**
     * Uno nuevo con ese nombre.
     *
     * <p>Es protegido porque los atributos que existen son los de esta clase; una subclase que
     * agregue los suyos tiene que ser de este mismo paquete.
     */
    protected TextAttribute(String name) {
        super(name);
        if (this.getClass() == TextAttribute.class) {
            instanceMap.put(name, this);
        }
    }

    /**
     * La instancia canónica que corresponde a este nombre.
     *
     * <p>Sin esto, un atributo deserializado sería un objeto distinto del que está en las constantes
     * y las comparaciones por identidad fallarían en silencio.
     *
     * @throws InvalidObjectException si la subclase no lo redefinió, o si el nombre no es de ninguno
     *     de los atributos conocidos
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

    /** La familia tipográfica, por nombre. */
    public static final TextAttribute FAMILY = new TextAttribute("family");

    /** El grosor del trazo, como múltiplo del regular. */
    public static final TextAttribute WEIGHT = new TextAttribute("weight");

    /** El ancho de los glifos, como múltiplo del regular. */
    public static final TextAttribute WIDTH = new TextAttribute("width");

    /** La inclinación; 0 es derecha y 0,20 la cursiva de siempre. */
    public static final TextAttribute POSTURE = new TextAttribute("posture");

    /** El cuerpo, en puntos. */
    public static final TextAttribute SIZE = new TextAttribute("size");

    /** Una transformación afín aplicada a los glifos. */
    public static final TextAttribute TRANSFORM = new TextAttribute("transform");

    /** Volado o subíndice. */
    public static final TextAttribute SUPERSCRIPT = new TextAttribute("superscript");

    /** Una fuente ya armada, que reemplaza a todos los demás atributos. */
    public static final TextAttribute FONT = new TextAttribute("font");

    /** Un dibujo que ocupa el lugar del carácter. */
    public static final TextAttribute CHAR_REPLACEMENT = new TextAttribute("char_replacement");

    /** Con qué se pinta el texto. */
    public static final TextAttribute FOREGROUND = new TextAttribute("foreground");

    /** Con qué se pinta el fondo del texto. */
    public static final TextAttribute BACKGROUND = new TextAttribute("background");

    /** El subrayado. */
    public static final TextAttribute UNDERLINE = new TextAttribute("underline");

    /** El tachado. */
    public static final TextAttribute STRIKETHROUGH = new TextAttribute("strikethrough");

    /** La dirección base del párrafo. */
    public static final TextAttribute RUN_DIRECTION = new TextAttribute("run_direction");

    /** El nivel de anidamiento bidireccional. */
    public static final TextAttribute BIDI_EMBEDDING = new TextAttribute("bidi_embedding");

    /** Qué parte del sobrante absorbe este tramo al justificar. */
    public static final TextAttribute JUSTIFICATION = new TextAttribute("justification");

    /** El resaltado del texto que todavía está componiendo el método de entrada. */
    public static final TextAttribute INPUT_METHOD_HIGHLIGHT = new TextAttribute("input method highlight");

    /** El subrayado del texto que todavía está componiendo el método de entrada. */
    public static final TextAttribute INPUT_METHOD_UNDERLINE = new TextAttribute("input method underline");

    /** Intercambia el color del texto con el del fondo. */
    public static final TextAttribute SWAP_COLORS = new TextAttribute("swap_colors");

    /** Cómo se dibujan los dígitos según el idioma. */
    public static final TextAttribute NUMERIC_SHAPING = new TextAttribute("numeric_shaping");

    /** El ajuste fino de espacio entre pares de letras. */
    public static final TextAttribute KERNING = new TextAttribute("kerning");

    /** Si se usan las ligaduras de la fuente. */
    public static final TextAttribute LIGATURES = new TextAttribute("ligatures");

    /** Espacio agregado o quitado entre todas las letras. */
    public static final TextAttribute TRACKING = new TextAttribute("tracking");

    /** La mitad del grosor regular. */
    public static final Float WEIGHT_EXTRA_LIGHT = Float.valueOf(0.5f);

    /** Fina. */
    public static final Float WEIGHT_LIGHT = Float.valueOf(0.75f);

    /** Entre fina y regular. */
    public static final Float WEIGHT_DEMILIGHT = Float.valueOf(0.875f);

    /** El grosor normal. */
    public static final Float WEIGHT_REGULAR = Float.valueOf(1.0f);

    /** Apenas más gruesa que la regular. */
    public static final Float WEIGHT_SEMIBOLD = Float.valueOf(1.25f);

    /** Entre regular y negrita. */
    public static final Float WEIGHT_MEDIUM = Float.valueOf(1.5f);

    /** Casi negrita. */
    public static final Float WEIGHT_DEMIBOLD = Float.valueOf(1.75f);

    /** El doble del grosor regular: la negrita de siempre. */
    public static final Float WEIGHT_BOLD = Float.valueOf(2.0f);

    /** Más que negrita. */
    public static final Float WEIGHT_HEAVY = Float.valueOf(2.25f);

    /** Bastante más que negrita. */
    public static final Float WEIGHT_EXTRABOLD = Float.valueOf(2.5f);

    /** El grosor máximo previsto. */
    public static final Float WEIGHT_ULTRABOLD = Float.valueOf(2.75f);

    /** Estrecha. */
    public static final Float WIDTH_CONDENSED = Float.valueOf(0.75f);

    /** Apenas estrecha. */
    public static final Float WIDTH_SEMI_CONDENSED = Float.valueOf(0.875f);

    /** El ancho normal. */
    public static final Float WIDTH_REGULAR = Float.valueOf(1.0f);

    /** Apenas ancha. */
    public static final Float WIDTH_SEMI_EXTENDED = Float.valueOf(1.25f);

    /** Ancha. */
    public static final Float WIDTH_EXTENDED = Float.valueOf(1.5f);

    /** Derecha. */
    public static final Float POSTURE_REGULAR = Float.valueOf(0.0f);

    /** La inclinación de la cursiva de siempre. */
    public static final Float POSTURE_OBLIQUE = Float.valueOf(0.20f);

    /** Un nivel de volado. */
    public static final Integer SUPERSCRIPT_SUPER = Integer.valueOf(1);

    /** Un nivel de subíndice. */
    public static final Integer SUPERSCRIPT_SUB = Integer.valueOf(-1);

    /** El subrayado normal. */
    public static final Integer UNDERLINE_ON = Integer.valueOf(0);

    /** Tachado. */
    public static final Boolean STRIKETHROUGH_ON = Boolean.TRUE;

    /** Párrafo de izquierda a derecha. */
    public static final Boolean RUN_DIRECTION_LTR = Boolean.FALSE;

    /** Párrafo de derecha a izquierda. */
    public static final Boolean RUN_DIRECTION_RTL = Boolean.TRUE;

    /** Absorbe todo el sobrante que le toque. */
    public static final Float JUSTIFICATION_FULL = Float.valueOf(1.0f);

    /** No se estira. */
    public static final Float JUSTIFICATION_NONE = Float.valueOf(0.0f);

    /** Subrayado de un píxel, para métodos de entrada. */
    public static final Integer UNDERLINE_LOW_ONE_PIXEL = Integer.valueOf(1);

    /** Subrayado de dos píxeles. */
    public static final Integer UNDERLINE_LOW_TWO_PIXEL = Integer.valueOf(2);

    /** Subrayado punteado. */
    public static final Integer UNDERLINE_LOW_DOTTED = Integer.valueOf(3);

    /** Subrayado gris. */
    public static final Integer UNDERLINE_LOW_GRAY = Integer.valueOf(4);

    /** Subrayado de rayas. */
    public static final Integer UNDERLINE_LOW_DASHED = Integer.valueOf(5);

    /** Se intercambian los colores. */
    public static final Boolean SWAP_COLORS_ON = Boolean.TRUE;

    /** Se aplica el ajuste entre pares. */
    public static final Integer KERNING_ON = Integer.valueOf(1);

    /** Se usan las ligaduras. */
    public static final Integer LIGATURES_ON = Integer.valueOf(1);

    /** Letras apretadas. */
    public static final Float TRACKING_TIGHT = Float.valueOf(-0.04f);

    /** Letras separadas. */
    public static final Float TRACKING_LOOSE = Float.valueOf(0.04f);
}
