package java.util;

// KajiLibrary's java.util.HexFormat — bytes to hexadecimal text and back.
//
// Hex is the trivial encoding: a byte is 8 bits, a hex digit is 4, so every byte is exactly two
// digits and the conversion is two table lookups. What HexFormat adds is not the arithmetic but
// the *presentation*, because "hex" in the wild is a dozen different formats: `a1b2c3`,
// `A1:B2:C3`, `0xa1, 0xb2, 0xc3`. A HexFormat is an immutable description of one of those —
// a delimiter between bytes, a prefix and suffix around each byte, and upper or lower case —
// and the same object both formats and parses, so a round trip cannot drift.
//
// It is built by a `with...` chain from `of()`, and every `with` returns a NEW instance rather
// than mutating: `HexFormat.of().withUpperCase().withDelimiter(":")`. That is what makes it safe
// to keep one in a static field and share it.
//
// KajiLibrary implements the byte[]/String surface. The `<A extends Appendable>` overloads
// (formatHex(A, byte[]), toHexDigits(A, byte)) are omitted: they call methods on a receiver
// whose static type is a type variable, which compiler finding #111 miscompiles into silence.
// The char[] parseHex overload and the (fromIndex, toIndex) forms of the static digit parsers
// are omitted as redundant with the CharSequence ones we do provide.
public final class HexFormat {

    // Nibble value 0..15 -> digit character, in this format's case. A `final char[]` instance
    // field, not a `static final` table: finding #110 makes a static field read from another
    // class a trap, so KajiLibrary keeps constants out of static state.
    private final char[] digits;

    // Between bytes.
    private final String delimiter;

    // Around each byte.
    private final String prefix;

    private final String suffix;

    private HexFormat(String delimiter, String prefix, String suffix, boolean upper) {
        this.delimiter = delimiter;
        this.prefix = prefix;
        this.suffix = suffix;
        this.digits = buildDigits(upper);
    }

    // '0'..'9' then 'a'..'f' (or 'A'..'F'). Contiguous ASCII runs again, so arithmetic beats a
    // 16-element literal.
    private static char[] buildDigits(boolean upper) {
        char[] t = new char[16];
        for (int i = 0; i < 10; i++) {
            t[i] = (char) ('0' + i);
        }
        char base = 'a';
        if (upper) {
            base = 'A';
        }
        for (int i = 0; i < 6; i++) {
            t[10 + i] = (char) (base + i);
        }
        return t;
    }

    // The plain format: lowercase, nothing between or around the bytes. `a1b2c3`.
    public static HexFormat of() {
        return new HexFormat("", "", "", false);
    }

    // Lowercase, `delimiter` between bytes. `HexFormat.ofDelimiter(":")` gives `a1:b2:c3`.
    public static HexFormat ofDelimiter(String delimiter) {
        return new HexFormat(delimiter, "", "", false);
    }

    public HexFormat withDelimiter(String delimiter) {
        return new HexFormat(delimiter, this.prefix, this.suffix, isUpperCase());
    }

    public HexFormat withPrefix(String prefix) {
        return new HexFormat(this.delimiter, prefix, this.suffix, isUpperCase());
    }

    public HexFormat withSuffix(String suffix) {
        return new HexFormat(this.delimiter, this.prefix, suffix, isUpperCase());
    }

    public HexFormat withUpperCase() {
        return new HexFormat(this.delimiter, this.prefix, this.suffix, true);
    }

    public HexFormat withLowerCase() {
        return new HexFormat(this.delimiter, this.prefix, this.suffix, false);
    }

    public String delimiter() {
        return this.delimiter;
    }

    public String prefix() {
        return this.prefix;
    }

    public String suffix() {
        return this.suffix;
    }

    // The case is not stored separately — it is readable off the digit table, since only an
    // uppercase table has 'A' at index 10.
    public boolean isUpperCase() {
        boolean upper = false;
        if (this.digits[10] == 'A') {
            upper = true;
        }
        return upper;
    }

    // --- formatting ---

    // The digit for the low 4 bits of `value` (the second of a byte's two digits).
    public char toLowHexDigit(int value) {
        return this.digits[value & 0xf];
    }

    // The digit for bits 4..7 of `value` (the first of a byte's two digits).
    public char toHighHexDigit(int value) {
        return this.digits[(value >> 4) & 0xf];
    }

    public String formatHex(byte[] bytes) {
        return formatHex(bytes, 0, bytes.length);
    }

    /**
     * Formatea a un `Appendable` en vez de a un `String`.
     *
     * <p>Existe para no construir la cadena intermedia cuando el destino ya es un buffer: formatear
     * un megabyte a un `StringBuilder` con la version de `String` haria una copia entera de mas.
     *
     * <p>Devuelve **el mismo** `out` que recibio, para poder encadenarlo.
     */
    public <A extends Appendable> A formatHex(A out, byte[] bytes) {
        return formatHex(out, bytes, 0, bytes.length);
    }

    /** Idem, sobre un tramo. */
    public <A extends Appendable> A formatHex(A out, byte[] bytes, int fromIndex, int toIndex) {
        if (out == null) {
            throw new NullPointerException();
        }
        checkRange(fromIndex, toIndex, bytes.length);
        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                out.append(this.delimiter);
            }
            out.append(this.prefix);
            out.append(toHighHexDigit(bytes[i]));
            out.append(toLowHexDigit(bytes[i]));
            out.append(this.suffix);
        }
        return out;
    }

    /**
     * Los dos digitos de `value` a un `Appendable`.
     *
     * <p>Ojo: **no** lleva prefijo, sufijo ni delimitador. `toHexDigits` es la conversion cruda y
     * `formatHex` la que aplica el formato -- la diferencia esta en los dos nombres y es facil de
     * pasar por alto.
     */
    public <A extends Appendable> A toHexDigits(A out, byte value) {
        if (out == null) {
            throw new NullPointerException();
        }
        out.append(toHighHexDigit(value));
        out.append(toLowHexDigit(value));
        return out;
    }

    public String formatHex(byte[] bytes, int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex, bytes.length);
        StringBuilder sb = new StringBuilder();
        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(this.delimiter);
            }
            sb.append(this.prefix);
            sb.append(toHighHexDigit(bytes[i]));
            sb.append(toLowHexDigit(bytes[i]));
            sb.append(this.suffix);
        }
        return sb.toString();
    }

    // --- parsing ---

    public byte[] parseHex(CharSequence string) {
        return parseHex(string, 0, string.length());
    }

    /**
     * Parsea desde un `char[]`.
     *
     * <p>Se copia el tramo a un `String` y se delega. Copiar parece un desperdicio y es lo correcto:
     * el arreglo es **mutable** y de quien llama, asi que leerlo perezosamente dejaria al parser
     * expuesto a que se lo cambien en el medio. El JDK hace lo mismo por la misma razon.
     */
    public byte[] parseHex(char[] chars, int fromIndex, int toIndex) {
        if (chars == null) {
            throw new NullPointerException();
        }
        checkRange(fromIndex, toIndex, chars.length);
        return parseHex(String.valueOf(chars, fromIndex, toIndex - fromIndex));
    }

    // The inverse of formatHex, and it insists on the exact shape this format produces: the
    // delimiter must appear between every pair of bytes (and nowhere else), the prefix and
    // suffix must be present on each. Anything else is an error rather than a best-effort read,
    // which is the point — a parser that quietly skipped junk would let corrupt input through.
    public byte[] parseHex(CharSequence string, int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex, string.length());
        byte[] result;
        if (fromIndex == toIndex) {
            // Empty input is zero bytes for EVERY format, checked before the shape rules below:
            // with a delimiter the arithmetic would otherwise report a missing value, since an
            // empty string is indistinguishable from "one byte's worth of characters, absent".
            result = new byte[0];
        } else if (this.delimiter.isEmpty() && this.prefix.isEmpty() && this.suffix.isEmpty()) {
            result = parseNoDelimiter(string, fromIndex, toIndex);
        } else {
            // One byte occupies prefix + 2 digits + suffix, and all but the last are followed by
            // a delimiter. Adding one delimiter's width to the total makes every byte cost the
            // same `stride`, so the count is a single division — and a non-zero remainder is
            // already proof the input is malformed.
            int stride = this.prefix.length() + 2 + this.suffix.length() + this.delimiter.length();
            int span = toIndex - fromIndex + this.delimiter.length();
            if (span % stride != 0) {
                throw new IllegalArgumentException("extra or missing digits");
            }
            int count = span / stride;
            result = new byte[count];
            int offset = fromIndex;
            for (int i = 0; i < count; i++) {
                offset = offset + checkLiteral(string, offset, this.prefix);
                result[i] = (byte) hexPair(string, offset);
                offset = offset + 2;
                offset = offset + checkLiteral(string, offset, this.suffix);
                if (i < count - 1) {
                    offset = offset + checkLiteral(string, offset, this.delimiter);
                }
            }
        }
        return result;
    }

    private byte[] parseNoDelimiter(CharSequence string, int fromIndex, int toIndex) {
        int len = toIndex - fromIndex;
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("string length not even");
        }
        byte[] result = new byte[len / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) hexPair(string, fromIndex + i * 2);
        }
        return result;
    }

    // The byte value of the two digits at `offset`.
    private static int hexPair(CharSequence string, int offset) {
        int hi = fromHexDigit(string.charAt(offset));
        int lo = fromHexDigit(string.charAt(offset + 1));
        return (hi << 4) | lo;
    }

    // Assert that `literal` sits at `offset`; returns its length so the caller can advance.
    private static int checkLiteral(CharSequence string, int offset, String literal) {
        int n = literal.length();
        if (offset + n > string.length()) {
            throw new IllegalArgumentException("found not the expected literal");
        }
        for (int i = 0; i < n; i++) {
            if (string.charAt(offset + i) != literal.charAt(i)) {
                throw new IllegalArgumentException("found not the expected literal");
            }
        }
        return n;
    }

    private static void checkRange(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range out of bounds");
        }
    }

    // --- fixed-width digit conversions ---

    // The low `digits` nibbles of `value`, most significant first. Every other toHexDigits
    // overload is this one with the width its type implies, which is what makes them
    // zero-padded: `toHexDigits((byte) 5)` is "05", not "5".
    public String toHexDigits(long value, int digits) {
        if (digits < 0 || digits > 16) {
            throw new IllegalArgumentException("number of digits out of range");
        }
        char[] chars = new char[digits];
        for (int i = 0; i < digits; i++) {
            int shift = (digits - 1 - i) * 4;
            int nibble = (int) ((value >>> shift) & 0xfL);
            chars[i] = this.digits[nibble];
        }
        return String.valueOf(chars, 0, digits);
    }

    public String toHexDigits(byte value) {
        return toHexDigits((long) (value & 0xff), 2);
    }

    public String toHexDigits(char value) {
        return toHexDigits((long) value, 4);
    }

    public String toHexDigits(short value) {
        return toHexDigits((long) (value & 0xffff), 4);
    }

    public String toHexDigits(int value) {
        // The mask is what stops a negative int from sign-extending into the top 8 digits.
        return toHexDigits(((long) value) & 0xffffffffL, 8);
    }

    public String toHexDigits(long value) {
        return toHexDigits(value, 16);
    }

    // --- static digit helpers (case-insensitive: reading hex, unlike writing it, never cares) ---

    public static boolean isHexDigit(int ch) {
        boolean hex = false;
        if (ch >= '0' && ch <= '9') {
            hex = true;
        } else if (ch >= 'A' && ch <= 'F') {
            hex = true;
        } else if (ch >= 'a' && ch <= 'f') {
            hex = true;
        }
        return hex;
    }

    public static int fromHexDigit(int ch) {
        int v;
        if (ch >= '0' && ch <= '9') {
            v = ch - '0';
        } else if (ch >= 'A' && ch <= 'F') {
            v = ch - 'A' + 10;
        } else if (ch >= 'a' && ch <= 'f') {
            v = ch - 'a' + 10;
        } else {
            throw new NumberFormatException("not a hexadecimal digit");
        }
        return v;
    }

    // Up to 8 digits as an int. Note there is no sign and no overflow check: "ffffffff" is -1,
    // exactly as the bits say.
    public static int fromHexDigits(CharSequence string) {
        int length = string.length();
        if (length > 8) {
            throw new IllegalArgumentException("string length greater than 8");
        }
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 4) + fromHexDigit(string.charAt(i));
        }
        return value;
    }

    /**
     * Los digitos de `[fromIndex, toIndex)` como `int`.
     *
     * @throws IllegalArgumentException si el tramo tiene mas de 8 digitos, o alguno no es hexadecimal
     * @throws IndexOutOfBoundsException si el tramo no cae dentro de `string`
     */
    public static int fromHexDigits(CharSequence string, int fromIndex, int toIndex) {
        if (string == null) {
            throw new NullPointerException();
        }
        if (fromIndex < 0 || toIndex > string.length() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + string.length());
        }
        return fromHexDigits(string.subSequence(fromIndex, toIndex));
    }

    /** Idem, hasta 16 digitos, como `long`. */
    public static long fromHexDigitsToLong(CharSequence string, int fromIndex, int toIndex) {
        if (string == null) {
            throw new NullPointerException();
        }
        if (fromIndex < 0 || toIndex > string.length() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + string.length());
        }
        return fromHexDigitsToLong(string.subSequence(fromIndex, toIndex));
    }

    // Up to 16 digits as a long. Same story one width up.
    public static long fromHexDigitsToLong(CharSequence string) {
        int length = string.length();
        if (length > 16) {
            throw new IllegalArgumentException("string length greater than 16");
        }
        long value = 0L;
        for (int i = 0; i < length; i++) {
            value = (value << 4) + (long) fromHexDigit(string.charAt(i));
        }
        return value;
    }

    // --- identity ---

    public boolean equals(Object o) {
        boolean eq = false;
        if (o == this) {
            eq = true;
        } else if (o instanceof HexFormat) {
            HexFormat other = (HexFormat) o;
            if (this.digits[10] == other.digits[10]
                    && this.delimiter.equals(other.delimiter)
                    && this.prefix.equals(other.prefix)
                    && this.suffix.equals(other.suffix)) {
                eq = true;
            }
        }
        return eq;
    }

    public int hashCode() {
        int h = this.delimiter.hashCode();
        h = h * 31 + this.prefix.hashCode();
        h = h * 31 + this.suffix.hashCode();
        h = h * 31 + this.digits[10];
        return h;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("uppercase: ");
        sb.append(isUpperCase());
        sb.append(", delimiter: \"");
        sb.append(this.delimiter);
        sb.append("\", prefix: \"");
        sb.append(this.prefix);
        sb.append("\", suffix: \"");
        sb.append(this.suffix);
        sb.append("\"");
        return sb.toString();
    }
}
