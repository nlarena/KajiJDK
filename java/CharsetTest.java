import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.SortedMap;

/**
 * Exercises java.nio.charset. Every method returns the number of things that came out wrong, so
 * 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts, so
 * every expectation here was checked against the reference implementation before it was asked of
 * ours. The byte patterns and the malformed-input LENGTHS in particular are not guesses: they
 * were read off the JDK and only then written down.
 *
 * <p>Kept to pure ASCII on purpose (finding #259): the characters above U+007F that the tests
 * need are built from their code points rather than written as literals.
 */
public class CharsetTest {

    // ---- helpers ----

    /** A one-character string, built rather than written, for any code point. */
    static String chr(int cp) {
        if (cp > 0xffff) {
            char[] pair = new char[2];
            pair[0] = Character.highSurrogate(cp);
            pair[1] = Character.lowSurrogate(cp);
            return String.valueOf(pair, 0, 2);
        }
        char[] one = new char[1];
        one[0] = (char) cp;
        return String.valueOf(one, 0, 1);
    }

    /** A byte array from unsigned values, so the tests can be written in hex. */
    static byte[] raw(int[] values) {
        byte[] out = new byte[values.length];
        int i = 0;
        while (i < values.length) {
            out[i] = (byte) values[i];
            i = i + 1;
        }
        return out;
    }

    /** Whether what is left in {@code bb} is exactly these unsigned byte values. */
    static boolean bytesAre(ByteBuffer bb, int[] want) {
        if (bb.remaining() != want.length) {
            return false;
        }
        int i = 0;
        while (i < want.length) {
            int got = bb.get(bb.position() + i) & 0xff;
            if (got != want[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Encodes with REPLACE, the way the convenience methods do. */
    static ByteBuffer enc(Charset cs, String text) {
        return cs.encode(text);
    }

    /** Decodes with REPLACE, the way the convenience methods do. */
    static String dec(Charset cs, int[] bytes) {
        ByteBuffer in = ByteBuffer.wrap(CharsetTest.raw(bytes));
        CharBuffer out = cs.decode(in);
        return out.toString();
    }

    /** Decodes with REPORT and hands back the result, for the malformed-input tests. */
    static CoderResult report(Charset cs, int[] bytes) {
        CharsetDecoder decoder = cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(CharsetTest.raw(bytes));
        CharBuffer out = CharBuffer.allocate(32);
        return decoder.decode(in, out, true);
    }

    /** Whether the result is malformed input of exactly this length. */
    static boolean malformed(CoderResult cr, int length) {
        if (!cr.isError() || !cr.isMalformed()) {
            return false;
        }
        return cr.length() == length;
    }

    // ---- groups ----

    /** Names, aliases, lookup and the relations between charsets. */
    public static int nombres() {
        int bad = 0;
        if (Charset.forName("UTF-8") != StandardCharsets.UTF_8) {
            bad = bad + 1;
        }
        // Lookup ignores case, and finds aliases as well as canonical names.
        if (Charset.forName("utf8") != StandardCharsets.UTF_8) {
            bad = bad + 1;
        }
        if (Charset.forName("LATIN1") != StandardCharsets.ISO_8859_1) {
            bad = bad + 1;
        }
        if (Charset.forName("UnicodeBigUnmarked") != StandardCharsets.UTF_16BE) {
            bad = bad + 1;
        }
        // ...but the canonical spelling is what comes back.
        if (!Charset.forName("utf8").name().equals("UTF-8")) {
            bad = bad + 1;
        }
        if (!Charset.isSupported("ISO-8859-1")) {
            bad = bad + 1;
        }
        if (Charset.isSupported("no-such-charset")) {
            bad = bad + 1;
        }
        if (!Charset.defaultCharset().name().equals("UTF-8")) {
            bad = bad + 1;
        }
        if (!StandardCharsets.UTF_8.toString().equals("UTF-8")) {
            bad = bad + 1;
        }
        if (!StandardCharsets.UTF_8.displayName().equals("UTF-8")) {
            bad = bad + 1;
        }
        if (!StandardCharsets.UTF_8.isRegistered()) {
            bad = bad + 1;
        }
        if (!StandardCharsets.UTF_8.canEncode()) {
            bad = bad + 1;
        }
        if (!StandardCharsets.UTF_8.aliases().contains("UTF8")) {
            bad = bad + 1;
        }
        if (StandardCharsets.UTF_8.aliases().contains("UTF-8")) {
            bad = bad + 1;
        }
        if (StandardCharsets.UTF_8.hashCode() != "UTF-8".hashCode()) {
            bad = bad + 1;
        }
        if (!StandardCharsets.UTF_8.equals(Charset.forName("UTF8"))) {
            bad = bad + 1;
        }
        if (StandardCharsets.UTF_8.equals(StandardCharsets.UTF_16)) {
            bad = bad + 1;
        }
        // Ordering is by name, ignoring case: "UTF-8" sorts after "UTF-16".
        if (StandardCharsets.UTF_8.compareTo(StandardCharsets.UTF_16) <= 0) {
            bad = bad + 1;
        }
        if (StandardCharsets.UTF_8.compareTo(StandardCharsets.UTF_8) != 0) {
            bad = bad + 1;
        }
        // Containment runs one way: UTF-8 covers ASCII, not the other way round.
        if (!StandardCharsets.UTF_8.contains(StandardCharsets.US_ASCII)) {
            bad = bad + 1;
        }
        if (StandardCharsets.US_ASCII.contains(StandardCharsets.UTF_8)) {
            bad = bad + 1;
        }
        if (!StandardCharsets.ISO_8859_1.contains(StandardCharsets.US_ASCII)) {
            bad = bad + 1;
        }
        if (StandardCharsets.US_ASCII.contains(StandardCharsets.ISO_8859_1)) {
            bad = bad + 1;
        }
        if (!StandardCharsets.UTF_16.contains(StandardCharsets.UTF_32)) {
            bad = bad + 1;
        }
        if (!StandardCharsets.US_ASCII.contains(StandardCharsets.US_ASCII)) {
            bad = bad + 1;
        }
        // The forgiving lookup: unknown AND malformed names both yield the fallback.
        if (Charset.forName("no-such-charset", StandardCharsets.UTF_8)
                != StandardCharsets.UTF_8) {
            bad = bad + 1;
        }
        if (Charset.forName("!!illegal!!", StandardCharsets.UTF_8) != StandardCharsets.UTF_8) {
            bad = bad + 1;
        }
        if (Charset.forName("US-ASCII", StandardCharsets.UTF_8) != StandardCharsets.US_ASCII) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The two ways a name can be refused, which are different exceptions on purpose. */
    public static int nombresMalos() {
        int bad = 0;
        boolean threw = false;
        try {
            Charset.forName("-leading-punctuation");
            threw = false;
        } catch (IllegalCharsetNameException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        threw = false;
        try {
            Charset.forName("has space");
            threw = false;
        } catch (IllegalCharsetNameException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        // Well-formed but unknown is the OTHER exception.
        threw = false;
        try {
            Charset.forName("no-such-charset");
            threw = false;
        } catch (UnsupportedCharsetException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        // Punctuation is legal anywhere but first, and digits are legal everywhere.
        if (!Charset.isSupported("iso_646.irv:1991")) {
            bad = bad + 1;
        }
        if (!Charset.isSupported("646")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** UTF-8 round trips, at each of the four sequence lengths. */
    public static int utf8() {
        int bad = 0;
        int[] wantA = new int[1];
        wantA[0] = 0x41;
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.UTF_8, "A"), wantA)) {
            bad = bad + 1;
        }
        // U+00E9, two bytes.
        int[] wantE9 = new int[2];
        wantE9[0] = 0xc3;
        wantE9[1] = 0xa9;
        if (!CharsetTest.bytesAre(
                CharsetTest.enc(StandardCharsets.UTF_8, CharsetTest.chr(0xe9)), wantE9)) {
            bad = bad + 1;
        }
        // U+20AC EURO SIGN, three bytes.
        int[] wantEuro = new int[3];
        wantEuro[0] = 0xe2;
        wantEuro[1] = 0x82;
        wantEuro[2] = 0xac;
        if (!CharsetTest.bytesAre(
                CharsetTest.enc(StandardCharsets.UTF_8, CharsetTest.chr(0x20ac)), wantEuro)) {
            bad = bad + 1;
        }
        // U+1D160, four bytes, and two chars on the Java side.
        int[] wantNote = new int[4];
        wantNote[0] = 0xf0;
        wantNote[1] = 0x9d;
        wantNote[2] = 0x85;
        wantNote[3] = 0xa0;
        if (!CharsetTest.bytesAre(
                CharsetTest.enc(StandardCharsets.UTF_8, CharsetTest.chr(0x1d160)), wantNote)) {
            bad = bad + 1;
        }
        // ...and back again.
        if (!CharsetTest.dec(StandardCharsets.UTF_8, wantEuro).equals(CharsetTest.chr(0x20ac))) {
            bad = bad + 1;
        }
        if (!CharsetTest.dec(StandardCharsets.UTF_8, wantNote).equals(CharsetTest.chr(0x1d160))) {
            bad = bad + 1;
        }
        if (CharsetTest.dec(StandardCharsets.UTF_8, wantNote).length() != 2) {
            bad = bad + 1;
        }
        // The boundaries of each length, which is where an off-by-one would hide.
        if (!CharsetTest.roundTrips(0x7f) || !CharsetTest.roundTrips(0x80)) {
            bad = bad + 1;
        }
        if (!CharsetTest.roundTrips(0x7ff) || !CharsetTest.roundTrips(0x800)) {
            bad = bad + 1;
        }
        if (!CharsetTest.roundTrips(0xffff) || !CharsetTest.roundTrips(0x10000)) {
            bad = bad + 1;
        }
        if (!CharsetTest.roundTrips(0x10ffff)) {
            bad = bad + 1;
        }
        // Empty input is not a special case, but it is where buffer sizing goes wrong.
        int[] none = new int[0];
        if (!CharsetTest.dec(StandardCharsets.UTF_8, none).equals("")) {
            bad = bad + 1;
        }
        if (CharsetTest.enc(StandardCharsets.UTF_8, "").remaining() != 0) {
            bad = bad + 1;
        }
        // A string long enough to outgrow the first output buffer several times over.
        String unit = "abc" + CharsetTest.chr(0x20ac) + CharsetTest.chr(0x1d160);
        String big = unit;
        int k = 0;
        while (k < 7) {
            big = big + big;
            k = k + 1;
        }
        ByteBuffer encoded = CharsetTest.enc(StandardCharsets.UTF_8, big);
        CharBuffer back = StandardCharsets.UTF_8.decode(encoded);
        if (!back.toString().equals(big)) {
            bad = bad + 1;
        }
        return bad;
    }

    static boolean roundTrips(int cp) {
        String text = CharsetTest.chr(cp);
        ByteBuffer bytes = CharsetTest.enc(StandardCharsets.UTF_8, text);
        CharBuffer back = StandardCharsets.UTF_8.decode(bytes);
        return back.toString().equals(text);
    }

    /**
     * Broken UTF-8, and the exact length each break is reported with.
     *
     * <p>The lengths matter: they are how much input the caller skips before resynchronising, so
     * getting one wrong turns a single bad byte into a cascade. Every value here was read off the
     * JDK.
     */
    public static int utf8Malo() {
        int bad = 0;
        // A continuation byte with nothing leading it.
        int[] loose = new int[1];
        loose[0] = 0x80;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, loose), 1)) {
            bad = bad + 1;
        }
        // Overlong two-byte form of a character that fits in one.
        int[] overlong2 = new int[2];
        overlong2[0] = 0xc0;
        overlong2[1] = 0x80;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, overlong2), 1)) {
            bad = bad + 1;
        }
        // Overlong three-byte form: caught at the first trailing byte, so the length is 1.
        int[] overlong3 = new int[3];
        overlong3[0] = 0xe0;
        overlong3[1] = 0x80;
        overlong3[2] = 0x80;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, overlong3), 1)) {
            bad = bad + 1;
        }
        // A SURROGATE spelled out in UTF-8. Length 3 and not 1: the bytes are well-formed, so all
        // three are consumed before the character they spell turns out to be illegal.
        int[] surrogate = new int[3];
        surrogate[0] = 0xed;
        surrogate[1] = 0xa0;
        surrogate[2] = 0x80;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, surrogate), 3)) {
            bad = bad + 1;
        }
        // ...but U+D7FF, just below the surrogate block, is a perfectly good character.
        int[] justBelow = new int[3];
        justBelow[0] = 0xed;
        justBelow[1] = 0x9f;
        justBelow[2] = 0xbf;
        if (!CharsetTest.report(StandardCharsets.UTF_8, justBelow).isUnderflow()) {
            bad = bad + 1;
        }
        // Above U+10FFFF: rejected at the leading byte.
        int[] tooHigh = new int[4];
        tooHigh[0] = 0xf5;
        tooHigh[1] = 0x80;
        tooHigh[2] = 0x80;
        tooHigh[3] = 0x80;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, tooHigh), 1)) {
            bad = bad + 1;
        }
        // ...and at the first trailing byte, for the lead that can go either way.
        int[] justOver = new int[4];
        justOver[0] = 0xf4;
        justOver[1] = 0x90;
        justOver[2] = 0x80;
        justOver[3] = 0x80;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, justOver), 1)) {
            bad = bad + 1;
        }
        // U+10FFFF itself is fine.
        int[] highest = new int[4];
        highest[0] = 0xf4;
        highest[1] = 0x8f;
        highest[2] = 0xbf;
        highest[3] = 0xbf;
        if (!CharsetTest.report(StandardCharsets.UTF_8, highest).isUnderflow()) {
            bad = bad + 1;
        }
        // Truncated: the length is what is left over.
        int[] cut = new int[2];
        cut[0] = 0xe2;
        cut[1] = 0x82;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, cut), 2)) {
            bad = bad + 1;
        }
        // A bad SECOND trailing byte reports 2, because the first one was good.
        int[] badSecond = new int[3];
        badSecond[0] = 0xe2;
        badSecond[1] = 0x82;
        badSecond[2] = 0x41;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, badSecond), 2)) {
            bad = bad + 1;
        }
        // A bad FIRST trailing byte reports 1.
        int[] badFirst = new int[3];
        badFirst[0] = 0xe2;
        badFirst[1] = 0x28;
        badFirst[2] = 0xa1;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_8, badFirst), 1)) {
            bad = bad + 1;
        }
        // With REPLACE instead of REPORT, the same input decodes to U+FFFD and carries on.
        int[] mixed = new int[3];
        mixed[0] = 0x41;
        mixed[1] = 0x80;
        mixed[2] = 0x42;
        String replaced = CharsetTest.dec(StandardCharsets.UTF_8, mixed);
        if (replaced.length() != 3) {
            bad = bad + 1;
        }
        if (replaced.charAt(0) != 'A' || replaced.charAt(2) != 'B') {
            bad = bad + 1;
        }
        if (replaced.charAt(1) != (char) 0xfffd) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The two eight-bit charsets, including what they do with what they cannot hold. */
    public static int ochoBits() {
        int bad = 0;
        int[] wantE9 = new int[1];
        wantE9[0] = 0xe9;
        if (!CharsetTest.bytesAre(
                CharsetTest.enc(StandardCharsets.ISO_8859_1, CharsetTest.chr(0xe9)), wantE9)) {
            bad = bad + 1;
        }
        if (!CharsetTest.dec(StandardCharsets.ISO_8859_1, wantE9).equals(CharsetTest.chr(0xe9))) {
            bad = bad + 1;
        }
        // ISO-8859-1 has no unused byte value: every one of the 256 decodes.
        int i = 0;
        while (i < 256) {
            int[] one = new int[1];
            one[0] = i;
            String got = CharsetTest.dec(StandardCharsets.ISO_8859_1, one);
            if (got.length() != 1 || got.charAt(0) != (char) i) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        // US-ASCII refuses the high half, and refuses it as MALFORMED -- those bytes are not
        // characters it failed to place, they are bytes it says cannot occur.
        int[] high = new int[1];
        high[0] = 0xe9;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.US_ASCII, high), 1)) {
            bad = bad + 1;
        }
        // Encoding what does not fit gives a question mark, one per CHARACTER.
        String tricky = "A" + CharsetTest.chr(0xe9) + CharsetTest.chr(0x1d160);
        int[] wantAscii = new int[3];
        wantAscii[0] = 0x41;
        wantAscii[1] = 0x3f;
        wantAscii[2] = 0x3f;
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.US_ASCII, tricky), wantAscii)) {
            bad = bad + 1;
        }
        // The same string in Latin-1 keeps the e-acute and loses only the note.
        int[] wantLatin = new int[3];
        wantLatin[0] = 0x41;
        wantLatin[1] = 0xe9;
        wantLatin[2] = 0x3f;
        if (!CharsetTest.bytesAre(
                CharsetTest.enc(StandardCharsets.ISO_8859_1, tricky), wantLatin)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** UTF-16 and UTF-32, in all three byte orders each. */
    public static int anchos() {
        int bad = 0;
        String text = "A" + CharsetTest.chr(0x1d160);
        int[] be16 = new int[6];
        be16[0] = 0x00;
        be16[1] = 0x41;
        be16[2] = 0xd8;
        be16[3] = 0x34;
        be16[4] = 0xdd;
        be16[5] = 0x60;
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.UTF_16BE, text), be16)) {
            bad = bad + 1;
        }
        int[] le16 = new int[6];
        le16[0] = 0x41;
        le16[1] = 0x00;
        le16[2] = 0x34;
        le16[3] = 0xd8;
        le16[4] = 0x60;
        le16[5] = 0xdd;
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.UTF_16LE, text), le16)) {
            bad = bad + 1;
        }
        // "UTF-16" writes a big-endian mark and then big-endian text.
        int[] mark16 = new int[8];
        mark16[0] = 0xfe;
        mark16[1] = 0xff;
        mark16[2] = 0x00;
        mark16[3] = 0x41;
        mark16[4] = 0xd8;
        mark16[5] = 0x34;
        mark16[6] = 0xdd;
        mark16[7] = 0x60;
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.UTF_16, text), mark16)) {
            bad = bad + 1;
        }
        // Reading back: the mark is consumed and chooses the order.
        if (!CharsetTest.dec(StandardCharsets.UTF_16, mark16).equals(text)) {
            bad = bad + 1;
        }
        int[] markLe = new int[4];
        markLe[0] = 0xff;
        markLe[1] = 0xfe;
        markLe[2] = 0x41;
        markLe[3] = 0x00;
        if (!CharsetTest.dec(StandardCharsets.UTF_16, markLe).equals("A")) {
            bad = bad + 1;
        }
        // With no mark at all, "UTF-16" reads big-endian.
        int[] noMark = new int[2];
        noMark[0] = 0x00;
        noMark[1] = 0x41;
        if (!CharsetTest.dec(StandardCharsets.UTF_16, noMark).equals("A")) {
            bad = bad + 1;
        }
        // A mark inside UTF-16BE is not a mark, it is the character U+FEFF.
        int[] bomThenA = new int[4];
        bomThenA[0] = 0xfe;
        bomThenA[1] = 0xff;
        bomThenA[2] = 0x00;
        bomThenA[3] = 0x41;
        if (CharsetTest.dec(StandardCharsets.UTF_16BE, bomThenA).length() != 2) {
            bad = bad + 1;
        }
        // A low surrogate on its own, and an odd trailing byte.
        int[] loneLow = new int[2];
        loneLow[0] = 0xdc;
        loneLow[1] = 0x00;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_16BE, loneLow), 2)) {
            bad = bad + 1;
        }
        int[] odd = new int[3];
        odd[0] = 0x00;
        odd[1] = 0x41;
        odd[2] = 0x00;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_16BE, odd), 1)) {
            bad = bad + 1;
        }
        // UTF-32, where the supplementary character is one four-byte unit and not a pair.
        int[] be32 = new int[8];
        be32[0] = 0x00;
        be32[1] = 0x00;
        be32[2] = 0x00;
        be32[3] = 0x41;
        be32[4] = 0x00;
        be32[5] = 0x01;
        be32[6] = 0xd1;
        be32[7] = 0x60;
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.UTF_32BE, text), be32)) {
            bad = bad + 1;
        }
        // "UTF-32" writes NO mark, so its output is byte-for-byte UTF-32BE. This asymmetry with
        // UTF-16 is the reference behaviour, not an oversight here.
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.UTF_32, text), be32)) {
            bad = bad + 1;
        }
        int[] le32 = new int[8];
        le32[0] = 0x41;
        le32[1] = 0x00;
        le32[2] = 0x00;
        le32[3] = 0x00;
        le32[4] = 0x60;
        le32[5] = 0xd1;
        le32[6] = 0x01;
        le32[7] = 0x00;
        if (!CharsetTest.bytesAre(CharsetTest.enc(StandardCharsets.UTF_32LE, text), le32)) {
            bad = bad + 1;
        }
        if (!CharsetTest.dec(StandardCharsets.UTF_32BE, be32).equals(text)) {
            bad = bad + 1;
        }
        if (!CharsetTest.dec(StandardCharsets.UTF_32LE, le32).equals(text)) {
            bad = bad + 1;
        }
        // ...but it DOES read one.
        int[] mark32 = new int[8];
        mark32[0] = 0xff;
        mark32[1] = 0xfe;
        mark32[2] = 0x00;
        mark32[3] = 0x00;
        mark32[4] = 0x41;
        mark32[5] = 0x00;
        mark32[6] = 0x00;
        mark32[7] = 0x00;
        if (!CharsetTest.dec(StandardCharsets.UTF_32, mark32).equals("A")) {
            bad = bad + 1;
        }
        // Out of range is malformed; a SURROGATE code point is not, which is the opposite of
        // what UTF-8 does and was verified against the reference.
        int[] outOfRange = new int[4];
        outOfRange[0] = 0x00;
        outOfRange[1] = 0x11;
        outOfRange[2] = 0x00;
        outOfRange[3] = 0x00;
        if (!CharsetTest.malformed(CharsetTest.report(StandardCharsets.UTF_32BE, outOfRange), 4)) {
            bad = bad + 1;
        }
        int[] surrogate32 = new int[4];
        surrogate32[0] = 0x00;
        surrogate32[1] = 0x00;
        surrogate32[2] = 0xd8;
        surrogate32[3] = 0x00;
        if (!CharsetTest.report(StandardCharsets.UTF_32BE, surrogate32).isUnderflow()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The coder machinery itself: results, actions, replacements, state. */
    public static int codificadores() {
        int bad = 0;
        if (!CoderResult.UNDERFLOW.isUnderflow() || CoderResult.UNDERFLOW.isError()) {
            bad = bad + 1;
        }
        if (!CoderResult.OVERFLOW.isOverflow() || CoderResult.OVERFLOW.isError()) {
            bad = bad + 1;
        }
        CoderResult mal = CoderResult.malformedForLength(3);
        if (!mal.isMalformed() || !mal.isError() || mal.length() != 3) {
            bad = bad + 1;
        }
        if (mal.isUnmappable() || mal.isUnderflow() || mal.isOverflow()) {
            bad = bad + 1;
        }
        CoderResult unm = CoderResult.unmappableForLength(2);
        if (!unm.isUnmappable() || !unm.isError() || unm.length() != 2) {
            bad = bad + 1;
        }
        if (!mal.toString().equals("MALFORMED[3]")) {
            bad = bad + 1;
        }
        if (!unm.toString().equals("UNMAPPABLE[2]")) {
            bad = bad + 1;
        }
        if (!CoderResult.UNDERFLOW.toString().equals("UNDERFLOW")) {
            bad = bad + 1;
        }
        // length() on a non-error is not zero, it is refused.
        boolean threw = false;
        try {
            CoderResult.UNDERFLOW.length();
            threw = false;
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        // throwException turns a result into the matching exception.
        threw = false;
        try {
            mal.throwException();
            threw = false;
        } catch (CharacterCodingException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        if (!CodingErrorAction.REPLACE.toString().equals("REPLACE")) {
            bad = bad + 1;
        }
        if (CodingErrorAction.REPLACE == CodingErrorAction.REPORT) {
            bad = bad + 1;
        }
        // Defaults, and the ratios each charset promises.
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        if (decoder.malformedInputAction() != CodingErrorAction.REPORT) {
            bad = bad + 1;
        }
        if (decoder.unmappableCharacterAction() != CodingErrorAction.REPORT) {
            bad = bad + 1;
        }
        if (decoder.charset() != StandardCharsets.UTF_8) {
            bad = bad + 1;
        }
        if (decoder.replacement().length() != 1) {
            bad = bad + 1;
        }
        if (decoder.replacement().charAt(0) != (char) 0xfffd) {
            bad = bad + 1;
        }
        if (decoder.isAutoDetecting()) {
            bad = bad + 1;
        }
        if (decoder.averageCharsPerByte() != 1.0f || decoder.maxCharsPerByte() != 1.0f) {
            bad = bad + 1;
        }
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        if (encoder.replacement().length != 1 || encoder.replacement()[0] != (byte) '?') {
            bad = bad + 1;
        }
        if (encoder.averageBytesPerChar() != 1.1f || encoder.maxBytesPerChar() != 3.0f) {
            bad = bad + 1;
        }
        if (encoder.charset() != StandardCharsets.UTF_8) {
            bad = bad + 1;
        }
        // UTF-16 replaces with a whole code unit, not a single byte.
        CharsetEncoder wide = StandardCharsets.UTF_16BE.newEncoder();
        if (wide.replacement().length != 2) {
            bad = bad + 1;
        }
        if (StandardCharsets.UTF_32BE.newEncoder().replacement().length != 4) {
            bad = bad + 1;
        }
        // IGNORE drops the bad input instead of substituting for it.
        CharsetDecoder ignoring = StandardCharsets.UTF_8.newDecoder();
        ignoring.onMalformedInput(CodingErrorAction.IGNORE);
        ignoring.onUnmappableCharacter(CodingErrorAction.IGNORE);
        if (ignoring.malformedInputAction() != CodingErrorAction.IGNORE) {
            bad = bad + 1;
        }
        int[] mixed = new int[3];
        mixed[0] = 0x41;
        mixed[1] = 0x80;
        mixed[2] = 0x42;
        ByteBuffer in = ByteBuffer.wrap(CharsetTest.raw(mixed));
        String dropped = CharsetTest.decodeWith(ignoring, in);
        if (!dropped.equals("AB")) {
            bad = bad + 1;
        }
        // A replacement of one's own.
        CharsetDecoder custom = StandardCharsets.UTF_8.newDecoder();
        custom.onMalformedInput(CodingErrorAction.REPLACE);
        custom.replaceWith("?");
        if (!custom.replacement().equals("?")) {
            bad = bad + 1;
        }
        ByteBuffer again = ByteBuffer.wrap(CharsetTest.raw(mixed));
        if (!CharsetTest.decodeWith(custom, again).equals("A?B")) {
            bad = bad + 1;
        }
        // An empty replacement is refused.
        threw = false;
        try {
            StandardCharsets.UTF_8.newDecoder().replaceWith("");
            threw = false;
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        // canEncode, including the surrogate cases.
        CharsetEncoder ascii = StandardCharsets.US_ASCII.newEncoder();
        if (!ascii.canEncode('A')) {
            bad = bad + 1;
        }
        if (ascii.canEncode((char) 0xe9)) {
            bad = bad + 1;
        }
        if (ascii.canEncode((char) 0xd800)) {
            bad = bad + 1;
        }
        CharsetEncoder utf8 = StandardCharsets.UTF_8.newEncoder();
        if (!utf8.canEncode((char) 0xe9)) {
            bad = bad + 1;
        }
        if (utf8.canEncode((char) 0xd800)) {
            bad = bad + 1;
        }
        if (!utf8.canEncode(CharsetTest.chr(0x1d160))) {
            bad = bad + 1;
        }
        if (!StandardCharsets.ISO_8859_1.newEncoder().canEncode((char) 0xff)) {
            bad = bad + 1;
        }
        // A lone surrogate is malformed input to the encoder, not an unmappable character.
        CharsetEncoder strict = StandardCharsets.UTF_8.newEncoder();
        strict.onMalformedInput(CodingErrorAction.REPORT);
        CharBuffer lone = CharBuffer.wrap(CharsetTest.chr(0xd800));
        CoderResult cr = strict.encode(lone, ByteBuffer.allocate(8), true);
        if (!CharsetTest.malformed(cr, 1)) {
            bad = bad + 1;
        }
        // OVERFLOW when the output has no room, and the input is left untouched.
        CharsetEncoder tight = StandardCharsets.UTF_8.newEncoder();
        CharBuffer source = CharBuffer.wrap(CharsetTest.chr(0x20ac));
        ByteBuffer tiny = ByteBuffer.allocate(2);
        CoderResult over = tight.encode(source, tiny, true);
        if (!over.isOverflow()) {
            bad = bad + 1;
        }
        if (source.position() != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    // Drives a decoder to completion over one buffer and returns the text.
    static String decodeWith(CharsetDecoder decoder, ByteBuffer in) {
        CharBuffer out = CharBuffer.allocate(64);
        decoder.reset();
        decoder.decode(in, out, true);
        decoder.flush(out);
        out.flip();
        return out.toString();
    }

    /** The registry: what is available and how it is ordered. */
    public static int disponibles() {
        int bad = 0;
        SortedMap<String, Charset> all = Charset.availableCharsets();
        if (all.size() < 9) {
            bad = bad + 1;
        }
        if (!all.containsKey("UTF-8")) {
            bad = bad + 1;
        }
        if (all.get("UTF-8") != StandardCharsets.UTF_8) {
            bad = bad + 1;
        }
        if (all.isEmpty()) {
            bad = bad + 1;
        }
        if (!all.containsValue(StandardCharsets.ISO_8859_1)) {
            bad = bad + 1;
        }
        // Sorted without regard to case, so the first key is the lowest name.
        if (all.firstKey().compareToIgnoreCase(all.lastKey()) > 0) {
            bad = bad + 1;
        }
        if (!all.keySet().contains("US-ASCII")) {
            bad = bad + 1;
        }
        // Unmodifiable.
        boolean threw = false;
        try {
            all.put("X-mine", StandardCharsets.UTF_8);
            threw = false;
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        if (!threw) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything, so one call answers "does it work". */
    public static int todo() {
        return CharsetTest.nombres() + CharsetTest.nombresMalos() + CharsetTest.utf8()
                + CharsetTest.utf8Malo() + CharsetTest.ochoBits() + CharsetTest.anchos()
                + CharsetTest.codificadores() + CharsetTest.disponibles();
    }

    public static void main(String[] args) {
        System.out.println("nombres         " + CharsetTest.nombres());
        System.out.println("nombresMalos    " + CharsetTest.nombresMalos());
        System.out.println("utf8            " + CharsetTest.utf8());
        System.out.println("utf8Malo        " + CharsetTest.utf8Malo());
        System.out.println("ochoBits        " + CharsetTest.ochoBits());
        System.out.println("anchos          " + CharsetTest.anchos());
        System.out.println("codificadores   " + CharsetTest.codificadores());
        System.out.println("disponibles     " + CharsetTest.disponibles());
        System.out.println("TOTAL           " + CharsetTest.todo());
    }
}
