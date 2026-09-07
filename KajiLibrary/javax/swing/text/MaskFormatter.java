package javax.swing.text;

import java.text.ParseException;

import javax.swing.JFormattedTextField;

/**
 * Un formateador con mascara: el texto tiene que calzar en un molde.
 *
 * <h2>El molde</h2>
 *
 * <p>La mascara es una cadena donde cada caracter especial dice que se puede escribir en esa
 * posicion:
 *
 * <table border="1">
 * <caption>Los caracteres de la mascara</caption>
 * <tr><td><code>#</code></td><td>un digito</td></tr>
 * <tr><td><code>'</code></td><td>escape: el siguiente es literal</td></tr>
 * <tr><td><code>U</code></td><td>una letra, que pasa a mayuscula</td></tr>
 * <tr><td><code>L</code></td><td>una letra, que pasa a minuscula</td></tr>
 * <tr><td><code>A</code></td><td>una letra o un digito</td></tr>
 * <tr><td><code>?</code></td><td>una letra</td></tr>
 * <tr><td><code>*</code></td><td>cualquier cosa</td></tr>
 * <tr><td><code>H</code></td><td>un digito hexadecimal</td></tr>
 * </table>
 *
 * <p>Todo lo demas es literal y aparece siempre: en <code>###-####</code> el guion esta puesto
 * desde el principio y el usuario no lo escribe ni lo puede borrar.
 *
 * <h2>Los literales y el valor</h2>
 *
 * <p>Que el guion se vea no quiere decir que forme parte del valor. Con
 * {@link #setValueContainsLiteralCharacters} en falso, el valor de <code>123-4567</code> es
 * <code>1234567</code>. Es la diferencia entre lo que el usuario ve y lo que el programa guarda, y
 * conviene decidirla a proposito: guardar los literales obliga a sacarlos despues en cada uso.
 */
public class MaskFormatter extends DefaultFormatter {

    private static final char DIGIT_KEY = '#';
    private static final char LITERAL_KEY = '\'';
    private static final char UPPERCASE_KEY = 'U';
    private static final char LOWERCASE_KEY = 'L';
    private static final char ALPHA_NUMERIC_KEY = 'A';
    private static final char CHARACTER_KEY = '?';
    private static final char ANYTHING_KEY = '*';
    private static final char HEX_KEY = 'H';

    private String mask;
    private transient MaskCharacter[] maskChars;
    private String validCharacters;
    private String invalidCharacters;
    private String placeholderString;
    private char placeholder;
    private boolean containsLiteralChars;

    /** Un formateador sin mascara; acepta cualquier texto hasta que le pongan una. */
    public MaskFormatter() {
        setAllowsInvalid(false);
        containsLiteralChars = true;
        maskChars = new MaskCharacter[0];
        placeholder = ' ';
    }

    /** Un formateador con esa mascara. */
    public MaskFormatter(String mask) throws ParseException {
        this();
        setMask(mask);
    }

    /**
     * La mascara.
     *
     * @throws ParseException si la mascara no se entiende (por ejemplo, termina en un escape).
     */
    public void setMask(String mask) throws ParseException {
        this.mask = mask;
        updateInternalMask();
    }

    public String getMask() {
        return mask;
    }

    /** Si esta puesto, solo se aceptan estos caracteres, ademas de lo que diga la mascara. */
    public void setValidCharacters(String validCharacters) {
        this.validCharacters = validCharacters;
    }

    public String getValidCharacters() {
        return validCharacters;
    }

    /** Si esta puesto, estos caracteres se rechazan aunque la mascara los acepte. */
    public void setInvalidCharacters(String invalidCharacters) {
        this.invalidCharacters = invalidCharacters;
    }

    public String getInvalidCharacters() {
        return invalidCharacters;
    }

    /**
     * El texto que se muestra donde el usuario todavia no escribio.
     *
     * <p>Si es mas corto que la mascara, lo que falta se llena con
     * {@link #getPlaceholderCharacter}.
     */
    public void setPlaceholder(String placeholder) {
        this.placeholderString = placeholder;
    }

    public String getPlaceholder() {
        return placeholderString;
    }

    /** El caracter de relleno; por omision, un espacio. */
    public void setPlaceholderCharacter(char placeholder) {
        this.placeholder = placeholder;
    }

    public char getPlaceholderCharacter() {
        return placeholder;
    }

    /** Si el valor incluye los literales de la mascara; ver la nota de la clase. */
    public void setValueContainsLiteralCharacters(boolean containsLiteralChars) {
        this.containsLiteralChars = containsLiteralChars;
    }

    public boolean getValueContainsLiteralCharacters() {
        return containsLiteralChars;
    }

    /**
     * El valor de ese texto.
     *
     * @throws ParseException si el texto no calza en la mascara.
     */
    public Object stringToValue(String value) throws ParseException {
        return stringToValue(value, true);
    }

    private Object stringToValue(String value, boolean completo) throws ParseException {
        int errorOffset;
        if ((errorOffset = getInvalidOffset(value, completo)) == -1) {
            if (!getValueContainsLiteralCharacters()) {
                value = stripLiteralChars(value);
            }
            return super.stringToValue(value);
        }
        throw new ParseException("stringToValue passed invalid value", errorOffset);
    }

    /** La primera posicion que no calza, o -1 si calza toda. */
    private int getInvalidOffset(String string, boolean completo) {
        int max = string.length();
        if (completo && max != maskChars.length) {
            // Largo distinto: el texto esta incompleto o sobra.
            return max;
        }
        for (int i = 0; i < max; i++) {
            char aChar = string.charAt(i);
            if (i >= maskChars.length) {
                return i;
            }
            if (!maskChars[i].isValidCharacter(aChar)) {
                return i;
            }
        }
        return -1;
    }

    /** Saca de la cadena los caracteres literales de la mascara. */
    private String stripLiteralChars(String string) {
        StringBuilder sb = null;
        int last = 0;
        int max = string.length();
        for (int counter = 0; counter < max; counter++) {
            if (counter < maskChars.length && maskChars[counter].isLiteral()) {
                if (sb == null) {
                    sb = new StringBuilder();
                    if (counter > 0) {
                        sb.append(string, 0, counter);
                    }
                    last = counter + 1;
                } else if (last != counter) {
                    sb.append(string, last, counter);
                }
                last = counter + 1;
            }
        }
        if (sb == null) {
            return string;
        } else if (last != string.length()) {
            sb.append(string, last, string.length());
        }
        return sb.toString();
    }

    /**
     * El texto del valor, con los literales puestos y el relleno donde falta.
     *
     * <p>No es solo dar formato: tambien valida. Un caracter que no entra en su posicion es un
     * error, no algo que se pueda acomodar, porque acomodarlo cambiaria el valor sin avisar.
     *
     * @throws ParseException si el valor no entra en la mascara.
     */
    public String valueToString(Object value) throws ParseException {
        String sValue = (value == null) ? "" : value.toString();
        StringBuilder result = new StringBuilder();
        String placeholder = getPlaceholder();
        int[] valueCounter = {0};

        for (int counter = 0; counter < maskChars.length; counter++) {
            maskChars[counter].append(result, sValue, valueCounter, placeholder);
        }
        return result.toString();
    }

    public void install(JFormattedTextField ftf) {
        super.install(ftf);
        // Si el valor no calza, el campo queda con la mascara vacia y no con basura.
        if (ftf != null) {
            Object value = ftf.getValue();
            try {
                valueToString(value);
            } catch (ParseException pe) {
                setEditValid(false);
            }
        }
    }

    /** Arma la mascara interna a partir de la cadena. */
    private void updateInternalMask() throws ParseException {
        String mask = getMask();
        java.util.List<MaskCharacter> fixed = new java.util.ArrayList<MaskCharacter>();

        if (mask != null) {
            int maxCounter = mask.length();
            for (int counter = 0; counter < maxCounter; counter++) {
                char maskChar = mask.charAt(counter);
                switch (maskChar) {
                    case MaskFormatter.DIGIT_KEY:
                        fixed.add(new MaskCharacter(this, DIGIT_KEY, (char) 0));
                        break;
                    case MaskFormatter.LITERAL_KEY:
                        if (++counter < maxCounter) {
                            maskChar = mask.charAt(counter);
                            fixed.add(new MaskCharacter(this, (char) 0, maskChar));
                        } else {
                            throw new ParseException("Invalid character in mask", counter);
                        }
                        break;
                    case MaskFormatter.UPPERCASE_KEY:
                    case MaskFormatter.LOWERCASE_KEY:
                    case MaskFormatter.ALPHA_NUMERIC_KEY:
                    case MaskFormatter.CHARACTER_KEY:
                    case MaskFormatter.ANYTHING_KEY:
                    case MaskFormatter.HEX_KEY:
                        fixed.add(new MaskCharacter(this, maskChar, (char) 0));
                        break;
                    default:
                        fixed.add(new MaskCharacter(this, (char) 0, maskChar));
                        break;
                }
            }
        }
        maskChars = fixed.toArray(new MaskCharacter[fixed.size()]);
    }

    int getInitialVisualPosition() {
        // El cursor arranca en el primer lugar donde se puede escribir, no sobre un literal.
        for (int i = 0; i < maskChars.length; i++) {
            if (!maskChars[i].isLiteral()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Una posicion de la mascara.
     *
     * <p>O es un literal, y entonces siempre da el mismo caracter, o es un molde, y entonces dice
     * que caracteres acepta y como los transforma.
     */
    static class MaskCharacter {

        private final MaskFormatter fmt;
        private final char tipo;
        private final char literal;

        MaskCharacter(MaskFormatter fmt, char tipo, char literal) {
            this.fmt = fmt;
            this.tipo = tipo;
            this.literal = literal;
        }

        boolean isLiteral() {
            return tipo == 0;
        }

        /** El caracter que va en esa posicion. */
        char getChar(char aChar) {
            if (isLiteral()) {
                return literal;
            }
            if (tipo == UPPERCASE_KEY || tipo == HEX_KEY) {
                // El hexadecimal tambien: 'aF' y 'AF' son el mismo numero, y mostrar los dos
                // estilos mezclados en un mismo campo se lee peor que normalizarlos.
                return Character.toUpperCase(aChar);
            }
            if (tipo == LOWERCASE_KEY) {
                return Character.toLowerCase(aChar);
            }
            return aChar;
        }

        /**
         * Escribe en el resultado lo que corresponde a esta posicion.
         *
         * <p>Cuatro casos, en este orden: el caracter sirve y se usa; es un literal y se pone solo;
         * hay caracter pero no sirve, y eso es un error; no hay caracter y se rellena.
         *
         * <p>El primer caso pide que todavia queden caracteres en el valor. Sin esa condicion, un
         * molde permisivo como <code>*</code> aceptaria el caracter cero del final del valor y
         * escribiria basura en lugar del relleno.
         */
        void append(StringBuilder buff, String formatting, int[] index, String placeholder)
                throws ParseException {
            boolean inString = index[0] < formatting.length();
            char aChar = inString ? formatting.charAt(index[0]) : 0;

            if (inString && isValidCharacter(aChar)) {
                buff.append(getChar(aChar));
                index[0] = index[0] + 1;
            } else if (isLiteral()) {
                buff.append(literal);
                if (fmt.getValueContainsLiteralCharacters() && inString) {
                    if (aChar != literal) {
                        throw new ParseException("Invalid character: " + aChar, index[0]);
                    }
                    index[0] = index[0] + 1;
                }
            } else if (inString) {
                throw new ParseException("Invalid character: " + aChar, index[0]);
            } else if (placeholder != null && index[0] < placeholder.length()) {
                buff.append(placeholder.charAt(index[0]));
                index[0] = index[0] + 1;
            } else {
                buff.append(fmt.getPlaceholderCharacter());
                index[0] = index[0] + 1;
            }
        }

        /** Si ese caracter se puede escribir aca. */
        boolean isValidCharacter(char aChar) {
            if (isLiteral()) {
                return (literal == aChar);
            }
            if (!fmt.esPermitido(aChar)) {
                return false;
            }
            switch (tipo) {
                case MaskFormatter.DIGIT_KEY:
                    return Character.isDigit(aChar);
                case MaskFormatter.UPPERCASE_KEY:
                case MaskFormatter.LOWERCASE_KEY:
                case MaskFormatter.CHARACTER_KEY:
                    return Character.isLetter(aChar);
                case MaskFormatter.ALPHA_NUMERIC_KEY:
                    return Character.isLetterOrDigit(aChar);
                case MaskFormatter.HEX_KEY:
                    return (Character.isDigit(aChar)
                            || (aChar >= 'a' && aChar <= 'f')
                            || (aChar >= 'A' && aChar <= 'F'));
                case MaskFormatter.ANYTHING_KEY:
                    return true;
                default:
                    return false;
            }
        }
    }

    /** Si el caracter pasa las listas de permitidos y prohibidos. */
    boolean esPermitido(char aChar) {
        String validCharacters = getValidCharacters();
        String invalidCharacters = getInvalidCharacters();
        if (validCharacters != null && validCharacters.indexOf(aChar) == -1) {
            return false;
        }
        if (invalidCharacters != null && invalidCharacters.indexOf(aChar) != -1) {
            return false;
        }
        return true;
    }
}
