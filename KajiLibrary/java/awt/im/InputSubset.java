package java.awt.im;

/**
 * Los conjuntos de caracteres que se le pueden pedir a un método de entrada.
 *
 * <p>Sirve para acotar lo que el usuario puede escribir en un campo: en uno que sólo admite números
 * no tiene sentido que el método de entrada ofrezca candidatos en kanji.
 *
 * <p>Los tres subconjuntos han son el mismo bloque de Unicode mirado desde tres idiomas. Un mismo
 * carácter puede pertenecer a los tres, y la distinción no está en qué caracteres son sino en cuáles
 * conviene ofrecer primero según en qué idioma se esté escribiendo.
 *
 * <p>Extiende {@code Character.Subset}, que se compara **por identidad**: dos subconjuntos con el
 * mismo nombre son objetos distintos y no son iguales. Por eso las constantes de acá son las que hay
 * que usar y no unas propias con el mismo nombre.
 */
public final class InputSubset extends Character.Subset {

    /** Con el nombre dado; privado porque los subconjuntos que existen son los de acá. */
    private InputSubset(String name) {
        super(name);
    }

    /** Letras latinas. */
    public static final InputSubset LATIN = new InputSubset("LATIN");

    /** Dígitos latinos. */
    public static final InputSubset LATIN_DIGITS = new InputSubset("LATIN_DIGITS");

    /** Caracteres han tradicionales. */
    public static final InputSubset TRADITIONAL_HANZI = new InputSubset("TRADITIONAL_HANZI");

    /** Caracteres han simplificados. */
    public static final InputSubset SIMPLIFIED_HANZI = new InputSubset("SIMPLIFIED_HANZI");

    /** Los han que se usan en japonés. */
    public static final InputSubset KANJI = new InputSubset("KANJI");

    /** Los han que se usan en coreano. */
    public static final InputSubset HANJA = new InputSubset("HANJA");

    /** Katakana de ancho mitad. */
    public static final InputSubset HALFWIDTH_KATAKANA = new InputSubset("HALFWIDTH_KATAKANA");

    /** Letras latinas de ancho completo. */
    public static final InputSubset FULLWIDTH_LATIN = new InputSubset("FULLWIDTH_LATIN");

    /** Dígitos de ancho completo. */
    public static final InputSubset FULLWIDTH_DIGITS = new InputSubset("FULLWIDTH_DIGITS");
}
