// Por import y nombre simple: una llamada estatica calificada no resuelve (finding #274).
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;

/**
 * Exercises java.lang.Short, Byte, Boolean and CharSequence, the wrapper caches the language
 * requires, and the bootstrap descriptors the three {@code describeConstable} methods are built
 * on. Every method returns the number of things that came out wrong, so 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 */
public class WrapMoreTest {

    static int eq(String got, String want) {
        if (got == null) {
            if (want == null) {
                return 0;
            }
            return 1;
        }
        if (got.equals(want)) {
            return 0;
        }
        return 1;
    }

    /** Short: parsing, formatting, and the range check that is the only thing it adds to int. */
    public static int cortos() {
        int bad = 0;
        if (Short.MIN_VALUE != -32768 || Short.MAX_VALUE != 32767) {
            bad = bad + 1;
        }
        if (Short.SIZE != 16 || Short.BYTES != 2) {
            bad = bad + 1;
        }
        if (Short.TYPE == null || Short.TYPE == Short.class) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.eq(Short.TYPE.getName(), "short");

        if (Short.parseShort("123") != 123 || Short.parseShort("-32768") != -32768) {
            bad = bad + 1;
        }
        if (Short.parseShort("7fff", 16) != 32767 || Short.parseShort("-11", 2) != -3) {
            bad = bad + 1;
        }
        if (Short.valueOf("42").shortValue() != 42) {
            bad = bad + 1;
        }
        if (Short.valueOf("ff", 16).shortValue() != 255) {
            bad = bad + 1;
        }
        if (Short.decode("0x7f").shortValue() != 127 || Short.decode("#10").shortValue() != 16) {
            bad = bad + 1;
        }
        if (Short.decode("-010").shortValue() != -8 || Short.decode("99").shortValue() != 99) {
            bad = bad + 1;
        }
        // The range check: a perfectly good int that is not a short.
        bad = bad + WrapMoreTest.expectNumberFormat(1);
        bad = bad + WrapMoreTest.expectNumberFormat(2);
        bad = bad + WrapMoreTest.expectNumberFormat(3);

        bad = bad + WrapMoreTest.eq(Short.toString((short) -300), "-300");
        bad = bad + WrapMoreTest.eq(Short.valueOf((short) 7).toString(), "7");
        if (Short.hashCode((short) -5) != -5
                || Short.valueOf((short) 9).hashCode() != 9) {
            bad = bad + 1;
        }
        if (!Short.valueOf((short) 5).equals(Short.valueOf((short) 5))) {
            bad = bad + 1;
        }
        if (Short.valueOf((short) 5).equals(Integer.valueOf(5))
                || Short.valueOf((short) 5).equals(null)) {
            bad = bad + 1;
        }
        if (Short.compare((short) -1, (short) 1) >= 0
                || Short.compare((short) 3, (short) 3) != 0) {
            bad = bad + 1;
        }
        // Read as unsigned, -1 is the LARGEST short there is.
        if (Short.compareUnsigned((short) -1, (short) 1) <= 0) {
            bad = bad + 1;
        }
        if (Short.toUnsignedInt((short) -1) != 65535
                || Short.toUnsignedLong((short) -1) != 65535L) {
            bad = bad + 1;
        }
        if (Short.reverseBytes((short) 0x1234) != 0x3412) {
            bad = bad + 1;
        }
        if (Short.reverseBytes((short) -1) != -1) {
            bad = bad + 1;
        }
        Short seven = Short.valueOf((short) 7);
        if (seven.byteValue() != 7 || seven.shortValue() != 7 || seven.intValue() != 7
                || seven.longValue() != 7L) {
            bad = bad + 1;
        }
        if (seven.floatValue() != 7.0f || seven.doubleValue() != 7.0d) {
            bad = bad + 1;
        }
        // byteValue narrows, and losing the high byte is the specified behaviour.
        if (Short.valueOf((short) 300).byteValue() != 44) {
            bad = bad + 1;
        }
        if (Short.valueOf((short) 1).compareTo(Short.valueOf((short) 2)) >= 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Byte: the same shape, plus a cache that covers the whole type. */
    public static int bytes() {
        int bad = 0;
        if (Byte.MIN_VALUE != -128 || Byte.MAX_VALUE != 127) {
            bad = bad + 1;
        }
        if (Byte.SIZE != 8 || Byte.BYTES != 1) {
            bad = bad + 1;
        }
        if (Byte.TYPE == null || Byte.TYPE == Byte.class) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.eq(Byte.TYPE.getName(), "byte");

        if (Byte.parseByte("100") != 100 || Byte.parseByte("-128") != -128) {
            bad = bad + 1;
        }
        if (Byte.parseByte("7f", 16) != 127) {
            bad = bad + 1;
        }
        if (Byte.valueOf("42").byteValue() != 42 || Byte.valueOf("11", 2).byteValue() != 3) {
            bad = bad + 1;
        }
        if (Byte.decode("0x7f").byteValue() != 127 || Byte.decode("-0x80").byteValue() != -128) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.expectNumberFormat(4);
        bad = bad + WrapMoreTest.expectNumberFormat(5);

        bad = bad + WrapMoreTest.eq(Byte.toString((byte) -128), "-128");
        bad = bad + WrapMoreTest.eq(Byte.valueOf((byte) 7).toString(), "7");
        if (Byte.hashCode((byte) -5) != -5 || Byte.valueOf((byte) 9).hashCode() != 9) {
            bad = bad + 1;
        }
        if (!Byte.valueOf((byte) 5).equals(Byte.valueOf((byte) 5))) {
            bad = bad + 1;
        }
        if (Byte.valueOf((byte) 5).equals(Short.valueOf((short) 5))) {
            bad = bad + 1;
        }
        if (Byte.compare((byte) -1, (byte) 1) >= 0 || Byte.compare((byte) 3, (byte) 3) != 0) {
            bad = bad + 1;
        }
        if (Byte.compareUnsigned((byte) -1, (byte) 1) <= 0) {
            bad = bad + 1;
        }
        if (Byte.toUnsignedInt((byte) -1) != 255 || Byte.toUnsignedLong((byte) -1) != 255L) {
            bad = bad + 1;
        }
        Byte seven = Byte.valueOf((byte) 7);
        if (seven.byteValue() != 7 || seven.shortValue() != 7 || seven.intValue() != 7
                || seven.longValue() != 7L) {
            bad = bad + 1;
        }
        if (seven.floatValue() != 7.0f || seven.doubleValue() != 7.0d) {
            bad = bad + 1;
        }
        if (Byte.valueOf((byte) 1).compareTo(Byte.valueOf((byte) 2)) >= 0) {
            bad = bad + 1;
        }
        return bad;
    }

    static int expectNumberFormat(int which) {
        try {
            WrapMoreTest.numberFormatCase(which);
        } catch (NumberFormatException ex) {
            return 0;
        }
        return 1;
    }

    static int numberFormatCase(int which) {
        if (which == 1) {
            return Short.parseShort("40000");
        }
        if (which == 2) {
            return Short.parseShort("-32769");
        }
        if (which == 3) {
            return Short.decode("0xffff").shortValue();
        }
        if (which == 4) {
            return Byte.parseByte("128");
        }
        return Byte.decode("0x100").byteValue();
    }

    /** Boolean: the parser that never throws, and the operations that never short-circuit. */
    public static int booleanos() {
        int bad = 0;
        if (Boolean.TYPE == null || Boolean.TYPE == Boolean.class) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.eq(Boolean.TYPE.getName(), "boolean");
        if (!Boolean.TRUE.booleanValue() || Boolean.FALSE.booleanValue()) {
            bad = bad + 1;
        }
        // Anything that is not "true" is false, and nothing throws -- null included.
        if (!Boolean.parseBoolean("true") || !Boolean.parseBoolean("TRUE")
                || !Boolean.parseBoolean("TrUe")) {
            bad = bad + 1;
        }
        if (Boolean.parseBoolean("false") || Boolean.parseBoolean("yes")
                || Boolean.parseBoolean("") || Boolean.parseBoolean(null)) {
            bad = bad + 1;
        }
        if (Boolean.valueOf("true") != Boolean.TRUE || Boolean.valueOf("x") != Boolean.FALSE) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.eq(Boolean.toString(true), "true");
        bad = bad + WrapMoreTest.eq(Boolean.toString(false), "false");
        bad = bad + WrapMoreTest.eq(Boolean.TRUE.toString(), "true");
        if (Boolean.hashCode(true) != 1231 || Boolean.hashCode(false) != 1237) {
            bad = bad + 1;
        }
        if (Boolean.TRUE.hashCode() != 1231 || Boolean.FALSE.hashCode() != 1237) {
            bad = bad + 1;
        }
        if (!Boolean.TRUE.equals(Boolean.valueOf(true)) || Boolean.TRUE.equals("true")) {
            bad = bad + 1;
        }
        if (Boolean.compare(false, true) >= 0 || Boolean.compare(true, true) != 0
                || Boolean.compare(true, false) <= 0) {
            bad = bad + 1;
        }
        if (Boolean.TRUE.compareTo(Boolean.FALSE) <= 0) {
            bad = bad + 1;
        }
        if (!Boolean.logicalAnd(true, true) || Boolean.logicalAnd(true, false)) {
            bad = bad + 1;
        }
        if (!Boolean.logicalOr(false, true) || Boolean.logicalOr(false, false)) {
            bad = bad + 1;
        }
        if (!Boolean.logicalXor(true, false) || Boolean.logicalXor(true, true)) {
            bad = bad + 1;
        }
        // A property nobody set is false rather than an error.
        if (Boolean.getBoolean("kaji.no.existe.esta.propiedad")) {
            bad = bad + 1;
        }
        if (Boolean.getBoolean(null) || Boolean.getBoolean("")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The five defaults CharSequence derives from length and charAt. */
    public static int secuencia() {
        int bad = 0;
        CharSequence abc = "abcdef";
        CharSequence vacia = "";
        if (abc.isEmpty() || !vacia.isEmpty()) {
            bad = bad + 1;
        }
        char[] out = new char[8];
        abc.getChars(1, 4, out, 2);
        if (out[0] != 0 || out[2] != 'b' || out[3] != 'c' || out[4] != 'd' || out[5] != 0) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.expectIndex(1);
        bad = bad + WrapMoreTest.expectIndex(2);

        int[] chars = abc.chars().toArray();
        if (chars.length != 6 || chars[0] != 'a' || chars[5] != 'f') {
            bad = bad + 1;
        }
        int[] points = abc.codePoints().toArray();
        if (points.length != 6 || points[0] != 'a') {
            bad = bad + 1;
        }
        // A surrogate pair is two chars and ONE code point, which is the whole reason both
        // methods exist.
        StringBuilder pair = new StringBuilder();
        pair.append('a').appendCodePoint(0x1d11e).append('b');
        CharSequence mixed = pair;
        if (mixed.chars().toArray().length != 4) {
            bad = bad + 1;
        }
        int[] mixedPoints = mixed.codePoints().toArray();
        if (mixedPoints.length != 3 || mixedPoints[1] != 0x1d11e) {
            bad = bad + 1;
        }
        // A lone high surrogate stays one code point of its own.
        StringBuilder lone = new StringBuilder();
        lone.append((char) 0xd834).append('b');
        CharSequence loneSeq = lone;
        if (loneSeq.codePoints().toArray().length != 2) {
            bad = bad + 1;
        }

        // compare is static because the two sides can be different implementations.
        CharSequence builder = new StringBuilder("abcdef");
        if (CharSequence.compare(abc, builder) != 0) {
            bad = bad + 1;
        }
        if (CharSequence.compare("abc", "abd") >= 0 || CharSequence.compare("abd", "abc") <= 0) {
            bad = bad + 1;
        }
        if (CharSequence.compare("ab", "abc") >= 0 || CharSequence.compare("", "") != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    static int expectIndex(int which) {
        try {
            CharSequence abc = "abcdef";
            char[] small = new char[2];
            if (which == 1) {
                abc.getChars(0, 4, small, 0);
            } else {
                abc.getChars(4, 1, small, 0);
            }
        } catch (IndexOutOfBoundsException ex) {
            return 0;
        }
        return 1;
    }

    /**
     * The identity the language promises.
     *
     * <p>JLS 5.1.7 requires boxing a value in the shared range to yield the SAME reference, so
     * these {@code ==} comparisons are not a test of an optimisation -- they test a rule. And the
     * one that must be FALSE is as important as the ones that must be true: a cache that covered
     * every value would make code that compares boxed integers with {@code ==} appear to work,
     * and it would break the moment it met a real JDK.
     */
    public static int identidad() {
        int bad = 0;
        if (Integer.valueOf(100) != Integer.valueOf(100)) {
            bad = bad + 1;
        }
        if (Integer.valueOf(-128) != Integer.valueOf(-128)
                || Integer.valueOf(127) != Integer.valueOf(127)) {
            bad = bad + 1;
        }
        if (Integer.valueOf(200) == Integer.valueOf(200)
                || Integer.valueOf(128) == Integer.valueOf(128)) {
            bad = bad + 1;
        }
        if (Integer.valueOf(-129) == Integer.valueOf(-129)) {
            bad = bad + 1;
        }
        // Autoboxing goes through valueOf, so it inherits the promise.
        Integer a = 100;
        Integer b = 100;
        if (a != b) {
            bad = bad + 1;
        }
        Integer big = 1000;
        Integer alsoBig = 1000;
        if (big == alsoBig) {
            bad = bad + 1;
        }
        if (Long.valueOf(100L) != Long.valueOf(100L)
                || Long.valueOf(1000L) == Long.valueOf(1000L)) {
            bad = bad + 1;
        }
        if (Short.valueOf((short) 100) != Short.valueOf((short) 100)) {
            bad = bad + 1;
        }
        if (Short.valueOf((short) 1000) == Short.valueOf((short) 1000)) {
            bad = bad + 1;
        }
        // Every byte is in range, so every Byte is shared.
        if (Byte.valueOf((byte) 100) != Byte.valueOf((byte) 100)
                || Byte.valueOf((byte) -128) != Byte.valueOf((byte) -128)) {
            bad = bad + 1;
        }
        // Character caches 0..127 and nothing below zero exists.
        if (Character.valueOf('k') != Character.valueOf('k')) {
            bad = bad + 1;
        }
        if (Character.valueOf((char) 200) == Character.valueOf((char) 200)) {
            bad = bad + 1;
        }
        if (Boolean.valueOf(true) != Boolean.TRUE || Boolean.valueOf(false) != Boolean.FALSE) {
            bad = bad + 1;
        }
        // Double and Float have no cache at all -- there is no range to share.
        if (Double.valueOf(1.0d) == Double.valueOf(1.0d)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The bootstrap descriptors, and the constants the wrappers describe themselves with. */
    public static int constables() {
        int bad = 0;
        // A bootstrap's descriptor always begins with the three arguments the VM passes.
        DirectMethodHandleDesc bsm = ConstantDescs.BSM_EXPLICIT_CAST;
        bad = bad + WrapMoreTest.eq(bsm.methodName(), "explicitCast");
        ClassDesc[] params = bsm.invocationType().parameterArray();
        if (params.length != 4) {
            bad = bad + 1;
        } else {
            bad = bad + WrapMoreTest.eq(params[0].descriptorString(),
                    "Ljava/lang/invoke/MethodHandles$Lookup;");
            bad = bad + WrapMoreTest.eq(params[1].descriptorString(), "Ljava/lang/String;");
            bad = bad + WrapMoreTest.eq(params[2].descriptorString(), "Ljava/lang/Class;");
            bad = bad + WrapMoreTest.eq(params[3].descriptorString(), "Ljava/lang/Object;");
        }
        // A call site bootstrap differs in the third: a signature, not a type.
        DirectMethodHandleDesc indy = ConstantDescs.ofCallsiteBootstrap(
                ConstantDescs.CD_Object, "linkme", ConstantDescs.CD_Object);
        ClassDesc[] indyParams = indy.invocationType().parameterArray();
        if (indyParams.length != 3) {
            bad = bad + 1;
        } else {
            bad = bad + WrapMoreTest.eq(indyParams[2].descriptorString(),
                    "Ljava/lang/invoke/MethodType;");
        }
        if (ConstantDescs.NULL == null) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.eq(ConstantDescs.TRUE.constantName(), "TRUE");
        bad = bad + WrapMoreTest.eq(ConstantDescs.FALSE.constantName(), "FALSE");

        // A short and a byte describe themselves as a CAST of an int, because a class file has
        // no constant of either type.
        DynamicConstantDesc<Short> asShort = Short.valueOf((short) 7).describeConstable().get();
        bad = bad + WrapMoreTest.eq(asShort.constantType().descriptorString(), "S");
        DynamicConstantDesc<Byte> asByte = Byte.valueOf((byte) 7).describeConstable().get();
        bad = bad + WrapMoreTest.eq(asByte.constantType().descriptorString(), "B");
        DynamicConstantDesc<Boolean> asBoolean = Boolean.TRUE.describeConstable().get();
        bad = bad + WrapMoreTest.eq(asBoolean.constantType().descriptorString(),
                "Ljava/lang/Boolean;");
        if (!Short.valueOf((short) 7).describeConstable().isPresent()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The two numbering schemes, and the pieces of Character that need no Unicode table. */
    public static int caracteres() {
        int bad = 0;
        if (Character.TYPE == null || Character.TYPE == Character.class) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.eq(Character.TYPE.getName(), "char");
        // The general categories, in the standard's own order.
        if (Character.UNASSIGNED != 0 || Character.UPPERCASE_LETTER != 1
                || Character.LOWERCASE_LETTER != 2 || Character.TITLECASE_LETTER != 3) {
            bad = bad + 1;
        }
        if (Character.DECIMAL_DIGIT_NUMBER != 9 || Character.SPACE_SEPARATOR != 12
                || Character.CONTROL != 15 || Character.SURROGATE != 19) {
            bad = bad + 1;
        }
        if (Character.MATH_SYMBOL != 25 || Character.FINAL_QUOTE_PUNCTUATION != 30) {
            bad = bad + 1;
        }
        // The bidirectional classes, where UNDEFINED is -1 because 0 already means something.
        if (Character.DIRECTIONALITY_UNDEFINED != -1
                || Character.DIRECTIONALITY_LEFT_TO_RIGHT != 0
                || Character.DIRECTIONALITY_RIGHT_TO_LEFT != 1) {
            bad = bad + 1;
        }
        if (Character.DIRECTIONALITY_WHITESPACE != 12
                || Character.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE != 22) {
            bad = bad + 1;
        }

        if (Character.reverseBytes((char) 0x1234) != (char) 0x3412) {
            bad = bad + 1;
        }
        if (Character.reverseBytes((char) 0xffff) != (char) 0xffff) {
            bad = bad + 1;
        }
        // Both control ranges, and the C1 one is the one that gets forgotten.
        if (!Character.isISOControl((char) 10) || !Character.isISOControl((char) 0)
                || !Character.isISOControl((char) 0x7f) || !Character.isISOControl((char) 0x9f)) {
            bad = bad + 1;
        }
        if (Character.isISOControl('a') || Character.isISOControl((char) 0x20)
                || Character.isISOControl((char) 0xa0)) {
            bad = bad + 1;
        }
        if (!Character.isISOControl(0x1f) || Character.isISOControl(0x1d11e)) {
            bad = bad + 1;
        }
        // A titlecase letter is a THIRD case: Dz is neither DZ nor dz.
        if (!Character.isTitleCase((char) 0x01f2) || !Character.isTitleCase((char) 0x01c5)) {
            bad = bad + 1;
        }
        if (Character.isTitleCase('A') || Character.isTitleCase('a')
                || Character.isTitleCase('1')) {
            bad = bad + 1;
        }
        // The definitive check, and the one that caught this method being derived from the case
        // mappings instead of read from a table: there are exactly 31 titlecase characters, and
        // "its own titlecase but not its own uppercase" finds 50 of them plus 42 impostors.
        int titles = 0;
        int cp = 0;
        while (cp <= 0xffff) {
            if (Character.isTitleCase(cp)) {
                titles = titles + 1;
            }
            cp = cp + 1;
        }
        if (titles != 31) {
            bad = bad + 1;
        }
        if (!Character.valueOf('k').describeConstable().isPresent()) {
            bad = bad + 1;
        }
        bad = bad + WrapMoreTest.eq(
                Character.valueOf('k').describeConstable().get().constantType()
                        .descriptorString(), "C");
        return bad;
    }

    public static int todo() {
        return WrapMoreTest.cortos() + WrapMoreTest.bytes() + WrapMoreTest.booleanos()
                + WrapMoreTest.secuencia() + WrapMoreTest.identidad()
                + WrapMoreTest.constables() + WrapMoreTest.caracteres();
    }

    public static void main(String[] args) {
        System.out.println("cortos      " + WrapMoreTest.cortos());
        System.out.println("bytes       " + WrapMoreTest.bytes());
        System.out.println("booleanos   " + WrapMoreTest.booleanos());
        System.out.println("secuencia   " + WrapMoreTest.secuencia());
        System.out.println("identidad   " + WrapMoreTest.identidad());
        System.out.println("constables  " + WrapMoreTest.constables());
        System.out.println("caracteres  " + WrapMoreTest.caracteres());
        System.out.println("TOTAL       " + WrapMoreTest.todo());
    }
}
