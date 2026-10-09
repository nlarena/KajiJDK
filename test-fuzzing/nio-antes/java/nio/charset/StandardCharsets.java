package java.nio.charset;

/**
 * The charsets every Java platform is required to have.
 *
 * <p>Constants rather than {@code Charset.forName} calls, and the difference is not only
 * convenience: a name looked up at run time can fail at run time, while these cannot fail at
 * all. Any code that reaches for {@code forName("UTF-8")} and catches the exception is writing
 * a handler for something that cannot happen.
 */
public final class StandardCharsets {

    private StandardCharsets() {
    }

    /** Seven-bit ASCII, also known as ISO646-US and as the Basic Latin block of Unicode. */
    public static final Charset US_ASCII = new SingleByteCharset("US-ASCII",
            StandardCharsets.aliases("646,ANSI_X3.4-1968,ANSI_X3.4-1986,ASCII,IBM367,ISO646-US,"
                    + "ISO_646.irv:1991,ascii7,cp367,csASCII,iso-ir-6,iso_646.irv:1983,us"),
            RankedCharset.RANK_ASCII, 0x7f);

    /** ISO Latin Alphabet No. 1, whose 256 characters are the first 256 of Unicode. */
    public static final Charset ISO_8859_1 = new SingleByteCharset("ISO-8859-1",
            StandardCharsets.aliases("819,8859_1,IBM-819,IBM819,ISO8859-1,ISO8859_1,ISO_8859-1,"
                    + "ISO_8859-1:1987,ISO_8859_1,cp819,csISOLatin1,iso-ir-100,l1,latin1"),
            RankedCharset.RANK_LATIN1, 0xff);

    /** Eight-bit UCS Transformation Format, and the default charset of the platform. */
    public static final Charset UTF_8 = new Utf8Charset();

    /** Sixteen-bit UCS Transformation Format, big-endian byte order. */
    public static final Charset UTF_16BE = new Utf16Charset("UTF-16BE",
            StandardCharsets.aliases("ISO-10646-UCS-2,UTF_16BE,UnicodeBigUnmarked,X-UTF-16BE"),
            Utf16Charset.MODE_BIG);

    /** Sixteen-bit UCS Transformation Format, little-endian byte order. */
    public static final Charset UTF_16LE = new Utf16Charset("UTF-16LE",
            StandardCharsets.aliases("UTF_16LE,UnicodeLittleUnmarked,X-UTF-16LE"),
            Utf16Charset.MODE_LITTLE);

    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by a mark.
     *
     * <p>Decoding reads the mark and falls back to big-endian without one; encoding always
     * writes a big-endian mark.
     */
    public static final Charset UTF_16 = new Utf16Charset("UTF-16",
            StandardCharsets.aliases("UTF_16,UnicodeBig,unicode,utf16"),
            Utf16Charset.MODE_MARK);

    /** Thirty-two-bit UCS Transformation Format, big-endian byte order. */
    public static final Charset UTF_32BE = new Utf32Charset("UTF-32BE",
            StandardCharsets.aliases("UTF_32BE,X-UTF-32BE"), Utf32Charset.MODE_BIG);

    /** Thirty-two-bit UCS Transformation Format, little-endian byte order. */
    public static final Charset UTF_32LE = new Utf32Charset("UTF-32LE",
            StandardCharsets.aliases("UTF_32LE,X-UTF-32LE"), Utf32Charset.MODE_LITTLE);

    /**
     * Thirty-two-bit UCS Transformation Format, byte order identified by a mark.
     *
     * <p>Decoding reads the mark and falls back to big-endian without one; encoding writes no
     * mark at all, so its output is byte-for-byte {@link #UTF_32BE}. The asymmetry with {@link
     * #UTF_16} is real and is the reference behaviour.
     */
    public static final Charset UTF_32 = new Utf32Charset("UTF-32",
            StandardCharsets.aliases("UTF32,UTF_32"), Utf32Charset.MODE_MARK);

    // Splits a comma-separated list. Written by hand rather than with String.split because the
    // alias lists are long enough that an array literal per charset would bury the names, and
    // because a regex here would pull java.util.regex into the initialisation of a class that
    // java.lang.String itself depends on.
    private static String[] aliases(String list) {
        int count = 1;
        int i = 0;
        while (i < list.length()) {
            if (list.charAt(i) == ',') {
                count = count + 1;
            }
            i = i + 1;
        }
        String[] out = new String[count];
        int put = 0;
        int from = 0;
        int at = 0;
        while (at < list.length()) {
            if (list.charAt(at) == ',') {
                out[put] = list.substring(from, at);
                put = put + 1;
                from = at + 1;
            }
            at = at + 1;
        }
        out[put] = list.substring(from, list.length());
        return out;
    }
}
