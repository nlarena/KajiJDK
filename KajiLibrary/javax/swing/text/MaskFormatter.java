package javax.swing.text;

import java.text.ParseException;

import javax.swing.JFormattedTextField;

/**
 * A formatter with a mask: the text has to fit a mould.
 *
 * <h2>The mould</h2>
 *
 * <p>The mask is a string where each special character says what may be typed at that position:
 *
 * <table border="1">
 * <caption>The mask characters</caption>
 * <tr><td><code>#</code></td><td>a digit</td></tr>
 * <tr><td><code>'</code></td><td>escape: the next one is literal</td></tr>
 * <tr><td><code>U</code></td><td>a letter, turned into upper case</td></tr>
 * <tr><td><code>L</code></td><td>a letter, turned into lower case</td></tr>
 * <tr><td><code>A</code></td><td>a letter or a digit</td></tr>
 * <tr><td><code>?</code></td><td>a letter</td></tr>
 * <tr><td><code>*</code></td><td>anything</td></tr>
 * <tr><td><code>H</code></td><td>a hexadecimal digit</td></tr>
 * </table>
 *
 * <p>Everything else is literal and always appears: in <code>###-####</code> the hyphen is there
 * from the start and the user neither types it nor can remove it.
 *
 * <h2>The literals and the value</h2>
 *
 * <p>That the hyphen is seen does not mean it is part of the value. With
 * {@link #setValueContainsLiteralCharacters} at false, the value of <code>123-4567</code> is
 * <code>1234567</code>. It is the difference between what the user sees and what the program
 * keeps, and it is worth deciding on purpose: keeping the literals forces taking them out
 * afterwards on every use.
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

    /** A formatter with no mask; it accepts any text until one is set on it. */
    public MaskFormatter() {
        setAllowsInvalid(false);
        containsLiteralChars = true;
        maskChars = new MaskCharacter[0];
        placeholder = ' ';
    }

    /** A formatter with that mask. */
    public MaskFormatter(String mask) throws ParseException {
        this();
        setMask(mask);
    }

    /**
     * The mask.
     *
     * @throws ParseException if the mask is not understood (for instance, it ends in an escape).
     */
    public void setMask(String mask) throws ParseException {
        this.mask = mask;
        updateInternalMask();
    }

    public String getMask() {
        return mask;
    }

    /** If it is set, only these characters are accepted, besides what the mask says. */
    public void setValidCharacters(String validCharacters) {
        this.validCharacters = validCharacters;
    }

    public String getValidCharacters() {
        return validCharacters;
    }

    /** If it is set, these characters are rejected even if the mask accepts them. */
    public void setInvalidCharacters(String invalidCharacters) {
        this.invalidCharacters = invalidCharacters;
    }

    public String getInvalidCharacters() {
        return invalidCharacters;
    }

    /**
     * The text shown where the user has not typed yet.
     *
     * <p>If it is shorter than the mask, what is missing is filled with
     * {@link #getPlaceholderCharacter}.
     */
    public void setPlaceholder(String placeholder) {
        this.placeholderString = placeholder;
    }

    public String getPlaceholder() {
        return placeholderString;
    }

    /** The filler character; by default, a space. */
    public void setPlaceholderCharacter(char placeholder) {
        this.placeholder = placeholder;
    }

    public char getPlaceholderCharacter() {
        return placeholder;
    }

    /** Whether the value includes the mask's literals; see the class note. */
    public void setValueContainsLiteralCharacters(boolean containsLiteralChars) {
        this.containsLiteralChars = containsLiteralChars;
    }

    public boolean getValueContainsLiteralCharacters() {
        return containsLiteralChars;
    }

    /**
     * That text's value.
     *
     * @throws ParseException if the text does not fit the mask.
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

    /** The first position that does not fit, or -1 if it all fits. */
    private int getInvalidOffset(String string, boolean completo) {
        int max = string.length();
        if (completo && max != maskChars.length) {
            // A different length: the text is incomplete or there is too much.
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

    /** It takes the mask's literal characters out of the string. */
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
     * The value's text, with the literals put in and the filler where something is missing.
     *
     * <p>It is not only formatting: it also validates. A character that does not fit in its
     * position is an error, not something that can be fixed up, because fixing it up would change
     * the value without warning.
     *
     * @throws ParseException if the value does not fit the mask.
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
        // If the value does not fit, the field is left with the empty mask and not with rubbish.
        if (ftf != null) {
            Object value = ftf.getValue();
            try {
                valueToString(value);
            } catch (ParseException pe) {
                setEditValid(false);
            }
        }
    }

    /** It builds the internal mask from the string. */
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
        // The cursor starts at the first place where typing is possible, not over a literal.
        for (int i = 0; i < maskChars.length; i++) {
            if (!maskChars[i].isLiteral()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * A position of the mask.
     *
     * <p>Either it is a literal, and then it always gives the same character, or it is a mould, and
     * then it says which characters it accepts and how it transforms them.
     */
    static class MaskCharacter {

        private final MaskFormatter fmt;
        private final char type;
        private final char literal;

        MaskCharacter(MaskFormatter fmt, char type, char literal) {
            this.fmt = fmt;
            this.type = type;
            this.literal = literal;
        }

        boolean isLiteral() {
            return type == 0;
        }

        /** The character that goes at that position. */
        char getChar(char aChar) {
            if (isLiteral()) {
                return literal;
            }
            if (type == UPPERCASE_KEY || type == HEX_KEY) {
                // Hexadecimal too: 'aF' and 'AF' are the same number, and showing the two styles
                // mixed
                                // in one same field reads worse than normalizing them.
                return Character.toUpperCase(aChar);
            }
            if (type == LOWERCASE_KEY) {
                return Character.toLowerCase(aChar);
            }
            return aChar;
        }

        /**
         * It writes into the result what corresponds to this position.
         *
         * <p>Four cases, in this order: the character serves and is used; it is a literal and is
         * put in by itself; there is a character but it does not serve, and that is an error; there
         * is no character and the filler goes in.
         *
         * <p>The first case asks that there still be characters left in the value. Without that
         * condition, a permissive mould such as <code>*</code> would accept the zero character at
         * the end of the value and write rubbish instead of the filler.
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

        /** Whether that character may be typed here. */
        boolean isValidCharacter(char aChar) {
            if (isLiteral()) {
                return (literal == aChar);
            }
            if (!fmt.isAllowed(aChar)) {
                return false;
            }
            switch (type) {
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

    /** Whether the character passes the allowed and forbidden lists. */
    boolean isAllowed(char aChar) {
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
