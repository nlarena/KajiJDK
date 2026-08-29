/**
 * Exercises the Unicode layer of java.lang.Character: the general category table and the
 * properties that hang off it. Every method returns the number of things that came out wrong, so
 * 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p><strong>How it checks.</strong> A handful of examples cannot validate a table of four
 * thousand ranges -- a range copied one code point short would sit between any two examples
 * anyone thought to write. So the checks are SWEEPS: the whole BMP plus a fixed sample of the
 * supplementary planes, reduced to one number, compared against the number the reference
 * produces over exactly the same set. That number is what catches a boundary being off by one,
 * and it is the same technique the {@code sqrt} and {@code fma} tables are validated with.
 */
public class CharUnicodeTest {

    // El barrido: el BMP entero, donde vive casi todo, mas una muestra de los planos
    // suplementarios lo bastante densa como para que un rango mal copiado caiga adentro.
    static final int SAMPLE_STEP = 997;

    static long hashOfType(boolean directionality) {
        long h = 1;
        int cp = 0;
        while (cp <= 0xffff) {
            if (directionality) {
                h = h * 31 + Character.getDirectionality(cp);
            } else {
                h = h * 31 + Character.getType(cp);
            }
            cp = cp + 1;
        }
        cp = 0x10000;
        while (cp <= 0x10ffff) {
            if (directionality) {
                h = h * 31 + Character.getDirectionality(cp);
            } else {
                h = h * 31 + Character.getType(cp);
            }
            cp = cp + CharUnicodeTest.SAMPLE_STEP;
        }
        return h;
    }

    // `which` elige la propiedad; devolver un int y no un predicado es a proposito, porque una
    // referencia a metodo por propiedad seria veinte clases sinteticas mas en el .class y esta
    // prueba tiene que compilar igual en las dos cadenas de herramientas.
    static boolean test(int which, int cp) {
        if (which == 0) {
            return Character.isDefined(cp);
        }
        if (which == 1) {
            return Character.isMirrored(cp);
        }
        if (which == 2) {
            return Character.isAlphabetic(cp);
        }
        if (which == 3) {
            return Character.isIdeographic(cp);
        }
        if (which == 4) {
            return Character.isJavaIdentifierStart(cp);
        }
        if (which == 5) {
            return Character.isJavaIdentifierPart(cp);
        }
        if (which == 6) {
            return Character.isUnicodeIdentifierStart(cp);
        }
        if (which == 7) {
            return Character.isUnicodeIdentifierPart(cp);
        }
        if (which == 8) {
            return Character.isIdentifierIgnorable(cp);
        }
        if (which == 9) {
            return Character.isEmoji(cp);
        }
        if (which == 10) {
            return Character.isEmojiComponent(cp);
        }
        if (which == 11) {
            return Character.isEmojiModifier(cp);
        }
        if (which == 12) {
            return Character.isEmojiModifierBase(cp);
        }
        if (which == 13) {
            return Character.isEmojiPresentation(cp);
        }
        if (which == 14) {
            return Character.isExtendedPictographic(cp);
        }
        if (which == 15) {
            return Character.isTitleCase(cp);
        }
        if (which == 16) {
            return Character.isLetter(cp);
        }
        return Character.isDigit(cp);
    }

    static int count(int which) {
        int n = 0;
        int cp = 0;
        while (cp <= 0xffff) {
            if (CharUnicodeTest.test(which, cp)) {
                n = n + 1;
            }
            cp = cp + 1;
        }
        cp = 0x10000;
        while (cp <= 0x10ffff) {
            if (CharUnicodeTest.test(which, cp)) {
                n = n + 1;
            }
            cp = cp + CharUnicodeTest.SAMPLE_STEP;
        }
        return n;
    }

    /** The two tables that answer with a value rather than a yes or no. */
    public static int tablas() {
        int bad = 0;
        if (CharUnicodeTest.hashOfType(false) != 4242836368789322347L) {
            bad = bad + 1;
        }
        if (CharUnicodeTest.hashOfType(true) != -661231361126141774L) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The eighteen properties, each as a count over the same sweep. */
    public static int propiedades() {
        int bad = 0;
        int[] expected = new int[18];
        expected[0] = 64331;   // isDefined
        expected[1] = 549;     // isMirrored
        expected[2] = 50012;   // isAlphabetic
        expected[3] = 28148;   // isIdeographic
        expected[4] = 49194;   // isJavaIdentifierStart
        expected[5] = 50991;   // isJavaIdentifierPart
        expected[6] = 49133;   // isUnicodeIdentifierStart
        expected[7] = 50952;   // isUnicodeIdentifierPart
        expected[8] = 99;      // isIdentifierIgnorable
        expected[9] = 184;     // isEmoji
        expected[10] = 15;     // isEmojiComponent
        expected[11] = 0;      // isEmojiModifier -- ninguno cae en la muestra, y es correcto
        expected[12] = 6;      // isEmojiModifierBase
        expected[13] = 62;     // isEmojiPresentation
        expected[14] = 345;    // isExtendedPictographic
        expected[15] = 31;     // isTitleCase
        expected[16] = 49062;  // isLetter
        expected[17] = 370;    // isDigit
        int which = 0;
        while (which < 18) {
            if (CharUnicodeTest.count(which) != expected[which]) {
                bad = bad + 1;
            }
            which = which + 1;
        }
        return bad;
    }

    /**
     * A few named characters, so that a failure says WHICH property broke.
     *
     * <p>The sweeps above are the real check; this group exists because a count that is off by
     * one names nothing, and these do.
     */
    public static int ejemplos() {
        int bad = 0;
        if (Character.getType('A') != Character.UPPERCASE_LETTER
                || Character.getType('a') != Character.LOWERCASE_LETTER) {
            bad = bad + 1;
        }
        if (Character.getType('7') != Character.DECIMAL_DIGIT_NUMBER
                || Character.getType(' ') != Character.SPACE_SEPARATOR) {
            bad = bad + 1;
        }
        if (Character.getType('$') != Character.CURRENCY_SYMBOL
                || Character.getType('_') != Character.CONNECTOR_PUNCTUATION) {
            bad = bad + 1;
        }
        if (Character.getType('+') != Character.MATH_SYMBOL
                || Character.getType('(') != Character.START_PUNCTUATION) {
            bad = bad + 1;
        }
        if (Character.getType((char) 0) != Character.CONTROL
                || Character.getType((char) 0xd800) != Character.SURROGATE) {
            bad = bad + 1;
        }
        // A supplementary code point: the table has to reach past the BMP.
        if (Character.getType(0x1d11e) != Character.OTHER_SYMBOL) {
            bad = bad + 1;
        }
        if (Character.getType(0x10ffff) != Character.UNASSIGNED) {
            bad = bad + 1;
        }
        // Directionality: Latin reads one way and Hebrew the other.
        if (Character.getDirectionality('A') != Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
            bad = bad + 1;
        }
        if (Character.getDirectionality((char) 0x05d0) != Character.DIRECTIONALITY_RIGHT_TO_LEFT) {
            bad = bad + 1;
        }
        if (Character.getDirectionality('7')
                != Character.DIRECTIONALITY_EUROPEAN_NUMBER) {
            bad = bad + 1;
        }
        if (Character.getDirectionality((char) 0xd800) != Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
            bad = bad + 1;
        }
        // Java's identifier rule takes the currency symbols and the connectors; Unicode's does
        // not, and that difference is why `$x` compiles.
        if (!Character.isJavaIdentifierStart('$') || !Character.isJavaIdentifierStart('_')) {
            bad = bad + 1;
        }
        if (Character.isUnicodeIdentifierStart('$')) {
            bad = bad + 1;
        }
        if (!Character.isJavaIdentifierPart('7') || Character.isJavaIdentifierStart('7')) {
            bad = bad + 1;
        }
        // A zero-width joiner is legal inside an identifier and contributes nothing.
        if (!Character.isIdentifierIgnorable(0x200d)) {
            bad = bad + 1;
        }
        if (Character.isIdentifierIgnorable('a') || Character.isIdentifierIgnorable(' ')) {
            bad = bad + 1;
        }
        // A parenthesis mirrors; a letter does not.
        if (!Character.isMirrored('(') || Character.isMirrored('a')) {
            bad = bad + 1;
        }
        // Alphabetic is wider than letter: a Devanagari vowel sign is a combining mark.
        if (!Character.isAlphabetic(0x093e) || Character.isLetter(0x093e)) {
            bad = bad + 1;
        }
        if (!Character.isIdeographic(0x4e00) || Character.isIdeographic('a')) {
            bad = bad + 1;
        }
        // The emoji properties are six different questions.
        if (!Character.isEmoji(0x1f600) || Character.isEmoji('a')) {
            bad = bad + 1;
        }
        if (!Character.isEmojiPresentation(0x1f600)) {
            bad = bad + 1;
        }
        if (!Character.isEmojiModifier(0x1f3fb) || Character.isEmojiModifier(0x1f600)) {
            bad = bad + 1;
        }
        if (!Character.isEmojiModifierBase(0x1f44d)) {
            bad = bad + 1;
        }
        // Emoji_Component is not "part of an emoji but not one": a digit is BOTH, because a
        // keycap sequence is built from it. What it separates is the skin tones, which are
        // components, from the faces they attach to, which are not.
        if (!Character.isEmojiComponent('7') || !Character.isEmoji('7')) {
            bad = bad + 1;
        }
        if (!Character.isEmojiComponent(0x1f3fb) || Character.isEmojiComponent(0x1f600)) {
            bad = bad + 1;
        }
        if (!Character.isExtendedPictographic(0x1f600)) {
            bad = bad + 1;
        }
        if (!Character.isDefined('a') || Character.isDefined(0x10ffff)) {
            bad = bad + 1;
        }
        // The two deprecated spellings still answer what they always did.
        if (!Character.isJavaLetter('a') || !Character.isJavaLetterOrDigit('7')) {
            bad = bad + 1;
        }
        if (!Character.isSpace(' ') || !Character.isSpace((char) 10)
                || Character.isSpace((char) 0x00a0)) {
            bad = bad + 1;
        }
        return bad;
    }

    public static int todo() {
        return CharUnicodeTest.tablas() + CharUnicodeTest.propiedades()
                + CharUnicodeTest.ejemplos();
    }

    public static void main(String[] args) {
        System.out.println("tablas       " + CharUnicodeTest.tablas());
        System.out.println("propiedades  " + CharUnicodeTest.propiedades());
        System.out.println("ejemplos     " + CharUnicodeTest.ejemplos());
        System.out.println("TOTAL        " + CharUnicodeTest.todo());
    }
}
