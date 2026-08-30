package java.lang;

/**
 * KajiLibrary's java.lang.CharacterData -- the package-private base of the Unicode property tables
 * the JDK's {@link Character} reads (one concrete subclass per code-point block:
 * {@code CharacterDataLatin1}, {@code CharacterData00}, ...).
 *
 * <p>KajiLibrary's {@code Character} carries its own tables and does not go through this class, so
 * it exists only for the shape. The concrete subclasses (the actual tables) are not here, so the
 * {@link #of(int)} factory that would pick one throws {@link UnsupportedOperationException}.
 */
abstract class CharacterData {

    CharacterData() {
    }

    abstract int getProperties(int ch);

    abstract int getType(int ch);

    abstract boolean isDigit(int ch);

    abstract boolean isLowerCase(int ch);

    abstract boolean isUpperCase(int ch);

    abstract boolean isWhitespace(int ch);

    abstract boolean isMirrored(int ch);

    abstract boolean isJavaIdentifierStart(int ch);

    abstract boolean isJavaIdentifierPart(int ch);

    abstract boolean isUnicodeIdentifierStart(int ch);

    abstract boolean isUnicodeIdentifierPart(int ch);

    abstract boolean isIdentifierIgnorable(int ch);

    abstract boolean isEmoji(int ch);

    abstract boolean isEmojiPresentation(int ch);

    abstract boolean isEmojiModifier(int ch);

    abstract boolean isEmojiModifierBase(int ch);

    abstract boolean isEmojiComponent(int ch);

    abstract boolean isExtendedPictographic(int ch);

    abstract int toLowerCase(int ch);

    abstract int toUpperCase(int ch);

    abstract int toTitleCase(int ch);

    abstract int digit(int ch, int radix);

    abstract int getNumericValue(int ch);

    abstract byte getDirectionality(int ch);

    // Los cuatro con cuerpo por defecto, idénticos al base del JDK.

    int toUpperCaseEx(int ch) {
        return toUpperCase(ch);
    }

    char[] toUpperCaseCharArray(int ch) {
        return null;
    }

    boolean isOtherAlphabetic(int ch) {
        return false;
    }

    boolean isIdeographic(int ch) {
        return false;
    }

    /**
     * The table for a code point's block. The JDK dispatches to a concrete subclass; those tables
     * are not in KajiLibrary, so there is nothing to return.
     */
    static final CharacterData of(int ch) {
        throw new UnsupportedOperationException("las tablas de CharacterData no están en KajiLibrary");
    }
}
