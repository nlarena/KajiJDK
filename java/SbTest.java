/**
 * Exercises java.lang.StringBuilder and java.lang.StringBuffer. Every method returns the number
 * of things that came out wrong, so 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p>Note what the probe is standing on: string concatenation itself lowers to
 * {@code StringBuilder}, so this file cannot build a message about a failure without using the
 * thing under test. That is why every check counts rather than reports -- a count survives a
 * broken builder, a message does not.
 */
public class SbTest {

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

    /** Building, appending, and reading back. */
    public static int basico() {
        int bad = 0;
        StringBuilder sb = new StringBuilder();
        bad = bad + SbTest.eq(sb.toString(), "");
        if (sb.length() != 0) {
            bad = bad + 1;
        }
        sb.append('a').append("bc").append(true).append(42).append(-7L);
        bad = bad + SbTest.eq(sb.toString(), "abctrue42-7");
        if (sb.length() != 11) {
            bad = bad + 1;
        }
        if (sb.charAt(0) != 'a' || sb.charAt(10) != '7') {
            bad = bad + 1;
        }

        StringBuilder from = new StringBuilder("hola");
        bad = bad + SbTest.eq(from.toString(), "hola");
        CharSequence seq = "mundo";
        StringBuilder fromSeq = new StringBuilder(seq);
        bad = bad + SbTest.eq(fromSeq.toString(), "mundo");

        // The extremes of the two integer overloads, which count downward so that the value with
        // no positive counterpart still prints.
        StringBuilder nums = new StringBuilder();
        nums.append(Integer.MIN_VALUE).append(' ').append(Integer.MAX_VALUE);
        bad = bad + SbTest.eq(nums.toString(), "-2147483648 2147483647");
        StringBuilder longs = new StringBuilder();
        longs.append(Long.MIN_VALUE).append(' ').append(Long.MAX_VALUE);
        bad = bad + SbTest.eq(longs.toString(), "-9223372036854775808 9223372036854775807");
        StringBuilder zeros = new StringBuilder();
        zeros.append(0).append(0L);
        bad = bad + SbTest.eq(zeros.toString(), "00");

        // Every null argument spells itself out rather than throwing.
        StringBuilder nulls = new StringBuilder();
        String nullString = null;
        Object nullObject = null;
        CharSequence nullSeq = null;
        StringBuffer nullBuffer = null;
        nulls.append(nullString).append(nullObject).append(nullSeq).append(nullBuffer);
        bad = bad + SbTest.eq(nulls.toString(), "nullnullnullnull");

        // ...and a sliced null slices the word.
        StringBuilder sliced = new StringBuilder();
        sliced.append(nullSeq, 1, 3);
        bad = bad + SbTest.eq(sliced.toString(), "ul");

        // char[] in both forms.
        char[] chars = new char[5];
        chars[0] = 'p';
        chars[1] = 'e';
        chars[2] = 'r';
        chars[3] = 'r';
        chars[4] = 'o';
        StringBuilder arr = new StringBuilder();
        arr.append(chars).append('-').append(chars, 1, 3);
        bad = bad + SbTest.eq(arr.toString(), "perro-err");

        // The float and double overloads go through the same shortest-decimal as valueOf.
        StringBuilder fp = new StringBuilder();
        fp.append(0.5d).append(' ').append(0.5f);
        bad = bad + SbTest.eq(fp.toString(), "0.5 0.5");

        // An Object appends its toString.
        StringBuilder obj = new StringBuilder();
        Object one = Integer.valueOf(7);
        obj.append(one);
        bad = bad + SbTest.eq(obj.toString(), "7");

        // A CharSequence range.
        StringBuilder range = new StringBuilder();
        CharSequence abcdef = "abcdef";
        range.append(abcdef, 2, 5);
        bad = bad + SbTest.eq(range.toString(), "cde");

        // setCharAt and getChars.
        StringBuilder edit = new StringBuilder("abcde");
        edit.setCharAt(0, 'X');
        bad = bad + SbTest.eq(edit.toString(), "Xbcde");
        char[] out = new char[4];
        edit.getChars(1, 4, out, 1);
        if (out[0] != 0 || out[1] != 'b' || out[2] != 'c' || out[3] != 'd') {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * Room: how it grows, and the two methods that ask about it.
     *
     * <p>Capacity is observable, so it is testable, and the growth rule is worth pinning down:
     * doubling ALONE would leave a builder made with capacity zero at zero forever.
     */
    public static int capacidad() {
        int bad = 0;
        StringBuilder def = new StringBuilder();
        if (def.capacity() != 16) {
            bad = bad + 1;
        }
        StringBuilder sized = new StringBuilder(7);
        if (sized.capacity() != 7) {
            bad = bad + 1;
        }
        StringBuilder fromString = new StringBuilder("abc");
        if (fromString.capacity() != 19) {
            bad = bad + 1;
        }
        // Zero has to escape from zero.
        StringBuilder empty = new StringBuilder(0);
        empty.append('x');
        if (empty.capacity() != 2) {
            bad = bad + 1;
        }
        // One character past the default doubles and adds two.
        StringBuilder grown = new StringBuilder();
        int i = 0;
        while (i < 17) {
            grown.append('x');
            i = i + 1;
        }
        if (grown.capacity() != 34) {
            bad = bad + 1;
        }
        // A request larger than the doubling wins.
        StringBuilder big = new StringBuilder();
        big.ensureCapacity(1000);
        if (big.capacity() != 1000) {
            bad = bad + 1;
        }
        // A non-positive request does nothing.
        StringBuilder untouched = new StringBuilder();
        untouched.ensureCapacity(-5);
        untouched.ensureCapacity(0);
        if (untouched.capacity() != 16) {
            bad = bad + 1;
        }
        // trimToSize gives the slack back.
        StringBuilder trim = new StringBuilder();
        trim.append("abc");
        trim.trimToSize();
        if (trim.capacity() != 3) {
            bad = bad + 1;
        }
        bad = bad + SbTest.eq(trim.toString(), "abc");

        // setLength truncates...
        StringBuilder cut = new StringBuilder("abcdef");
        cut.setLength(3);
        bad = bad + SbTest.eq(cut.toString(), "abc");
        // ...and pads with NUL, which is a real character and not nothing.
        StringBuilder pad = new StringBuilder("ab");
        pad.setLength(5);
        if (pad.length() != 5) {
            bad = bad + 1;
        }
        if (pad.charAt(2) != 0 || pad.charAt(4) != 0) {
            bad = bad + 1;
        }
        if (pad.toString().length() != 5) {
            bad = bad + 1;
        }
        StringBuilder none = new StringBuilder("abc");
        none.setLength(0);
        bad = bad + SbTest.eq(none.toString(), "");
        return bad;
    }

    /** delete, deleteCharAt and replace. */
    public static int borrado() {
        int bad = 0;
        StringBuilder sb = new StringBuilder("abcdefg");
        sb.delete(2, 4);
        bad = bad + SbTest.eq(sb.toString(), "abefg");
        sb.deleteCharAt(0);
        bad = bad + SbTest.eq(sb.toString(), "befg");
        // An empty range is a no-op, not an error.
        sb.delete(2, 2);
        bad = bad + SbTest.eq(sb.toString(), "befg");
        // An end past the end means "to the end", which is what makes this the idiom for
        // truncation.
        StringBuilder tail = new StringBuilder("abcdef");
        tail.delete(3, Integer.MAX_VALUE);
        bad = bad + SbTest.eq(tail.toString(), "abc");
        StringBuilder all = new StringBuilder("abc");
        all.delete(0, 3);
        bad = bad + SbTest.eq(all.toString(), "");

        // replace with a shorter, a longer and an equal string: the tail has to land right in
        // all three.
        StringBuilder shorter = new StringBuilder("abcdef");
        shorter.replace(1, 4, "X");
        bad = bad + SbTest.eq(shorter.toString(), "aXef");
        StringBuilder longer = new StringBuilder("abcdef");
        longer.replace(1, 3, "XYZW");
        bad = bad + SbTest.eq(longer.toString(), "aXYZWdef");
        StringBuilder same = new StringBuilder("abcdef");
        same.replace(2, 4, "XY");
        bad = bad + SbTest.eq(same.toString(), "abXYef");
        StringBuilder atEnd = new StringBuilder("abc");
        atEnd.replace(3, 3, "de");
        bad = bad + SbTest.eq(atEnd.toString(), "abcde");
        StringBuilder clamped = new StringBuilder("abcdef");
        clamped.replace(2, 99, "Z");
        bad = bad + SbTest.eq(clamped.toString(), "abZ");
        StringBuilder emptied = new StringBuilder("abcdef");
        emptied.replace(0, 6, "");
        bad = bad + SbTest.eq(emptied.toString(), "");
        return bad;
    }

    /** The twelve inserts. */
    public static int insercion() {
        int bad = 0;
        StringBuilder sb = new StringBuilder("ac");
        sb.insert(1, 'b');
        bad = bad + SbTest.eq(sb.toString(), "abc");
        sb.insert(0, "XY");
        bad = bad + SbTest.eq(sb.toString(), "XYabc");
        sb.insert(5, "Z");
        bad = bad + SbTest.eq(sb.toString(), "XYabcZ");

        StringBuilder nums = new StringBuilder("[]");
        nums.insert(1, 42);
        bad = bad + SbTest.eq(nums.toString(), "[42]");
        nums.insert(1, -7L);
        bad = bad + SbTest.eq(nums.toString(), "[-742]");
        StringBuilder flags = new StringBuilder("()");
        flags.insert(1, true);
        bad = bad + SbTest.eq(flags.toString(), "(true)");
        flags.insert(1, false);
        bad = bad + SbTest.eq(flags.toString(), "(falsetrue)");

        StringBuilder fp = new StringBuilder("<>");
        fp.insert(1, 0.5d);
        bad = bad + SbTest.eq(fp.toString(), "<0.5>");
        StringBuilder fp2 = new StringBuilder("<>");
        fp2.insert(1, 0.5f);
        bad = bad + SbTest.eq(fp2.toString(), "<0.5>");

        char[] chars = new char[3];
        chars[0] = 'p';
        chars[1] = 'q';
        chars[2] = 'r';
        StringBuilder arr = new StringBuilder("--");
        arr.insert(1, chars);
        bad = bad + SbTest.eq(arr.toString(), "-pqr-");
        StringBuilder arr2 = new StringBuilder("--");
        arr2.insert(1, chars, 1, 2);
        bad = bad + SbTest.eq(arr2.toString(), "-qr-");

        StringBuilder objs = new StringBuilder("..");
        Object seven = Integer.valueOf(7);
        objs.insert(1, seven);
        bad = bad + SbTest.eq(objs.toString(), ".7.");
        Object nothing = null;
        objs.insert(0, nothing);
        bad = bad + SbTest.eq(objs.toString(), "null.7.");

        CharSequence seq = "abcdef";
        StringBuilder cs = new StringBuilder("<>");
        cs.insert(1, seq);
        bad = bad + SbTest.eq(cs.toString(), "<abcdef>");
        StringBuilder cs2 = new StringBuilder("<>");
        cs2.insert(1, seq, 2, 4);
        bad = bad + SbTest.eq(cs2.toString(), "<cd>");
        CharSequence nullSeq = null;
        StringBuilder cs3 = new StringBuilder("<>");
        cs3.insert(1, nullSeq);
        bad = bad + SbTest.eq(cs3.toString(), "<null>");

        String nullString = null;
        StringBuilder ns = new StringBuilder("<>");
        ns.insert(1, nullString);
        bad = bad + SbTest.eq(ns.toString(), "<null>");
        return bad;
    }

    /**
     * indexOf and lastIndexOf, where the empty needle is the whole difficulty.
     *
     * <p>An empty string occurs at every position, so the answer is whatever the search was told
     * to start from -- clamped. That is the rule that a hand-written search always gets wrong in
     * one direction or the other.
     */
    public static int busqueda() {
        int bad = 0;
        StringBuilder sb = new StringBuilder("abcabcabc");
        if (sb.indexOf("abc") != 0 || sb.indexOf("abc", 1) != 3 || sb.indexOf("abc", 4) != 6) {
            bad = bad + 1;
        }
        if (sb.indexOf("abc", 7) != -1) {
            bad = bad + 1;
        }
        if (sb.indexOf("zzz") != -1) {
            bad = bad + 1;
        }
        if (sb.lastIndexOf("abc") != 6 || sb.lastIndexOf("abc", 5) != 3) {
            bad = bad + 1;
        }
        if (sb.lastIndexOf("abc", 0) != 0 || sb.lastIndexOf("zzz") != -1) {
            bad = bad + 1;
        }
        if (sb.indexOf("c", 100) != -1) {
            bad = bad + 1;
        }
        if (sb.indexOf("abc", -5) != 0) {
            bad = bad + 1;
        }
        // The empty needle.
        StringBuilder abc = new StringBuilder("abc");
        if (abc.indexOf("") != 0 || abc.indexOf("", 2) != 2) {
            bad = bad + 1;
        }
        if (abc.indexOf("", 99) != 3 || abc.indexOf("", -1) != 0) {
            bad = bad + 1;
        }
        if (abc.lastIndexOf("") != 3 || abc.lastIndexOf("", 1) != 1) {
            bad = bad + 1;
        }
        if (abc.lastIndexOf("", -1) != -1) {
            bad = bad + 1;
        }
        StringBuilder none = new StringBuilder("");
        if (none.indexOf("") != 0 || none.lastIndexOf("") != 0) {
            bad = bad + 1;
        }
        if (none.indexOf("a") != -1 || none.lastIndexOf("a") != -1) {
            bad = bad + 1;
        }
        // A needle longer than the haystack.
        if (abc.indexOf("abcd") != -1 || abc.lastIndexOf("abcd") != -1) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Code points, where a supplementary character is two chars and one of them is not a char. */
    public static int puntos() {
        int bad = 0;
        // U+1D11E, the treble clef: surrogate pair D834 DD1E.
        int clef = 0x1d11e;
        StringBuilder sb = new StringBuilder();
        sb.append('a').appendCodePoint(clef).append('b');
        if (sb.length() != 4) {
            bad = bad + 1;
        }
        if (sb.charAt(1) != (char) 0xd834 || sb.charAt(2) != (char) 0xdd1e) {
            bad = bad + 1;
        }
        if (sb.codePointAt(1) != clef) {
            bad = bad + 1;
        }
        if (sb.codePointAt(0) != 'a' || sb.codePointAt(3) != 'b') {
            bad = bad + 1;
        }
        // Reading the low surrogate on its own gives the surrogate, not the character.
        if (sb.codePointAt(2) != 0xdd1e) {
            bad = bad + 1;
        }
        // Looking BACK from 3 lands on the low surrogate, sees the high one before it, and
        // answers the whole character -- while looking FORWARD from 2 answers the bare
        // surrogate. The two are not mirror images, and that asymmetry is the specification.
        if (sb.codePointBefore(3) != clef) {
            bad = bad + 1;
        }
        if (sb.codePointBefore(4) != 'b') {
            bad = bad + 1;
        }
        if (sb.codePointBefore(1) != 'a') {
            bad = bad + 1;
        }
        // Four chars, three code points.
        if (sb.codePointCount(0, 4) != 3) {
            bad = bad + 1;
        }
        if (sb.codePointCount(1, 3) != 1 || sb.codePointCount(0, 0) != 0) {
            bad = bad + 1;
        }
        // Splitting the pair counts each half.
        if (sb.codePointCount(2, 4) != 2) {
            bad = bad + 1;
        }
        if (sb.offsetByCodePoints(0, 1) != 1 || sb.offsetByCodePoints(0, 2) != 3) {
            bad = bad + 1;
        }
        if (sb.offsetByCodePoints(0, 3) != 4 || sb.offsetByCodePoints(4, -1) != 3) {
            bad = bad + 1;
        }
        if (sb.offsetByCodePoints(4, -2) != 1 || sb.offsetByCodePoints(4, -3) != 0) {
            bad = bad + 1;
        }
        if (sb.offsetByCodePoints(2, 0) != 2) {
            bad = bad + 1;
        }
        // A code point in the basic plane appends as one char.
        StringBuilder plain = new StringBuilder();
        plain.appendCodePoint(0x00f1);
        if (plain.length() != 1 || plain.charAt(0) != (char) 0x00f1) {
            bad = bad + 1;
        }
        return bad;
    }

    /** reverse, and the surrogate pairs it must not break. */
    public static int reverso() {
        int bad = 0;
        StringBuilder sb = new StringBuilder("abcde");
        sb.reverse();
        bad = bad + SbTest.eq(sb.toString(), "edcba");
        StringBuilder even = new StringBuilder("abcd");
        even.reverse();
        bad = bad + SbTest.eq(even.toString(), "dcba");
        StringBuilder one = new StringBuilder("a");
        one.reverse();
        bad = bad + SbTest.eq(one.toString(), "a");
        StringBuilder none = new StringBuilder("");
        none.reverse();
        bad = bad + SbTest.eq(none.toString(), "");
        // The pair moves as a unit and stays in order.
        StringBuilder pair = new StringBuilder();
        pair.append('a').appendCodePoint(0x1d11e).append('b');
        pair.reverse();
        if (pair.length() != 4) {
            bad = bad + 1;
        }
        if (pair.charAt(0) != 'b' || pair.charAt(3) != 'a') {
            bad = bad + 1;
        }
        if (pair.codePointAt(1) != 0x1d11e) {
            bad = bad + 1;
        }
        // A LONE surrogate is an ordinary character and just moves.
        StringBuilder lone = new StringBuilder();
        lone.append('a').append((char) 0xd834).append('b');
        lone.reverse();
        if (lone.charAt(0) != 'b' || lone.charAt(1) != (char) 0xd834
                || lone.charAt(2) != 'a') {
            bad = bad + 1;
        }
        return bad;
    }

    /** repeat, in both forms. */
    public static int repetir() {
        int bad = 0;
        StringBuilder sb = new StringBuilder();
        sb.repeat('x', 3);
        bad = bad + SbTest.eq(sb.toString(), "xxx");
        sb.repeat("ab", 2);
        bad = bad + SbTest.eq(sb.toString(), "xxxabab");
        StringBuilder zero = new StringBuilder("k");
        zero.repeat('x', 0);
        zero.repeat("ab", 0);
        bad = bad + SbTest.eq(zero.toString(), "k");
        // A supplementary code point repeats as two chars each time.
        StringBuilder supp = new StringBuilder();
        supp.repeat(0x1d11e, 2);
        if (supp.length() != 4) {
            bad = bad + 1;
        }
        if (supp.codePointAt(0) != 0x1d11e || supp.codePointAt(2) != 0x1d11e) {
            bad = bad + 1;
        }
        CharSequence nullSeq = null;
        StringBuilder nulls = new StringBuilder();
        nulls.repeat(nullSeq, 2);
        bad = bad + SbTest.eq(nulls.toString(), "nullnull");
        return bad;
    }

    /** substring, subSequence, chars and codePoints. */
    public static int lectura() {
        int bad = 0;
        StringBuilder sb = new StringBuilder("abcdef");
        bad = bad + SbTest.eq(sb.substring(2), "cdef");
        bad = bad + SbTest.eq(sb.substring(1, 3), "bc");
        bad = bad + SbTest.eq(sb.substring(3, 3), "");
        bad = bad + SbTest.eq(sb.substring(6), "");
        CharSequence sub = sb.subSequence(1, 4);
        bad = bad + SbTest.eq(sub.toString(), "bcd");
        // The slice does NOT track later changes: it is a copy.
        sb.setCharAt(1, 'Z');
        bad = bad + SbTest.eq(sub.toString(), "bcd");

        StringBuilder cs = new StringBuilder();
        cs.append('a').appendCodePoint(0x1d11e).append('b');
        int[] asChars = cs.chars().toArray();
        if (asChars.length != 4) {
            bad = bad + 1;
        }
        if (asChars[0] != 'a' || asChars[1] != 0xd834 || asChars[3] != 'b') {
            bad = bad + 1;
        }
        int[] asPoints = cs.codePoints().toArray();
        if (asPoints.length != 3) {
            bad = bad + 1;
        }
        if (asPoints[0] != 'a' || asPoints[1] != 0x1d11e || asPoints[2] != 'b') {
            bad = bad + 1;
        }
        return bad;
    }

    /** compareTo, which is by contents and is not equals. */
    public static int comparar() {
        int bad = 0;
        StringBuilder a = new StringBuilder("abc");
        StringBuilder b = new StringBuilder("abd");
        StringBuilder c = new StringBuilder("abc");
        StringBuilder shorter = new StringBuilder("ab");
        if (a.compareTo(b) >= 0 || b.compareTo(a) <= 0) {
            bad = bad + 1;
        }
        if (a.compareTo(c) != 0) {
            bad = bad + 1;
        }
        if (a.compareTo(shorter) <= 0 || shorter.compareTo(a) >= 0) {
            bad = bad + 1;
        }
        // Same contents, different objects: equals is NOT overridden, so it is identity.
        if (a.equals(c)) {
            bad = bad + 1;
        }
        if (!a.equals(a)) {
            bad = bad + 1;
        }
        StringBuffer x = new StringBuffer("abc");
        StringBuffer y = new StringBuffer("abd");
        if (x.compareTo(y) >= 0 || y.compareTo(x) <= 0) {
            bad = bad + 1;
        }
        StringBuffer z = new StringBuffer("abc");
        if (x.compareTo(z) != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * StringBuffer, which must answer exactly what StringBuilder answers.
     *
     * <p>The two are one implementation with a lock over it, so this is not really testing the
     * arithmetic again -- it is testing that every one of the fifty-nine methods forwards to the
     * one it is named after. A wrapper fails by landing on the wrong sibling, and that is what a
     * side-by-side sweep catches.
     */
    public static int buffer() {
        int bad = 0;
        StringBuffer sb = new StringBuffer();
        bad = bad + SbTest.eq(sb.toString(), "");
        sb.append('a').append("bc").append(true).append(42).append(-7L);
        bad = bad + SbTest.eq(sb.toString(), "abctrue42-7");
        if (sb.length() != 11 || sb.charAt(0) != 'a') {
            bad = bad + 1;
        }
        if (new StringBuffer().capacity() != 16) {
            bad = bad + 1;
        }
        if (new StringBuffer("abc").capacity() != 19) {
            bad = bad + 1;
        }
        if (new StringBuffer(7).capacity() != 7) {
            bad = bad + 1;
        }
        CharSequence seq = "mundo";
        bad = bad + SbTest.eq(new StringBuffer(seq).toString(), "mundo");

        StringBuffer edit = new StringBuffer("abcdefg");
        edit.delete(2, 4);
        bad = bad + SbTest.eq(edit.toString(), "abefg");
        edit.deleteCharAt(0);
        bad = bad + SbTest.eq(edit.toString(), "befg");
        edit.replace(1, 3, "XYZ");
        bad = bad + SbTest.eq(edit.toString(), "bXYZg");
        edit.insert(0, "--");
        bad = bad + SbTest.eq(edit.toString(), "--bXYZg");
        edit.insert(0, 42).insert(0, true).insert(0, 1.5d);
        bad = bad + SbTest.eq(edit.toString(), "1.5true42--bXYZg");
        edit.reverse();
        bad = bad + SbTest.eq(edit.toString(), "gZYXb--24eurt5.1");
        edit.setLength(3);
        bad = bad + SbTest.eq(edit.toString(), "gZY");
        edit.setCharAt(0, 'G');
        bad = bad + SbTest.eq(edit.toString(), "GZY");
        bad = bad + SbTest.eq(edit.substring(1), "ZY");
        bad = bad + SbTest.eq(edit.substring(0, 2), "GZ");
        bad = bad + SbTest.eq(edit.subSequence(1, 3).toString(), "ZY");

        StringBuffer find = new StringBuffer("abcabc");
        if (find.indexOf("abc") != 0 || find.indexOf("abc", 1) != 3) {
            bad = bad + 1;
        }
        if (find.lastIndexOf("abc") != 3 || find.lastIndexOf("abc", 2) != 0) {
            bad = bad + 1;
        }

        StringBuffer pts = new StringBuffer();
        pts.append('a').appendCodePoint(0x1d11e).append('b');
        if (pts.codePointAt(1) != 0x1d11e || pts.codePointBefore(3) != 0x1d11e) {
            bad = bad + 1;
        }
        if (pts.codePointCount(0, 4) != 3 || pts.offsetByCodePoints(0, 2) != 3) {
            bad = bad + 1;
        }
        int[] asPoints = pts.codePoints().toArray();
        if (asPoints.length != 3 || asPoints[1] != 0x1d11e) {
            bad = bad + 1;
        }
        int[] asChars = pts.chars().toArray();
        if (asChars.length != 4 || asChars[1] != 0xd834) {
            bad = bad + 1;
        }

        StringBuffer rep = new StringBuffer();
        rep.repeat('x', 3).repeat("ab", 2);
        bad = bad + SbTest.eq(rep.toString(), "xxxabab");

        char[] chars = new char[3];
        chars[0] = 'p';
        chars[1] = 'q';
        chars[2] = 'r';
        StringBuffer arr = new StringBuffer();
        arr.append(chars).append(chars, 1, 2).insert(0, chars).insert(0, chars, 0, 1);
        bad = bad + SbTest.eq(arr.toString(), "ppqrpqrqr");
        char[] out = new char[3];
        arr.getChars(0, 3, out, 0);
        if (out[0] != 'p' || out[1] != 'p' || out[2] != 'q') {
            bad = bad + 1;
        }

        StringBuffer nulls = new StringBuffer();
        String nullString = null;
        Object nullObject = null;
        CharSequence nullSeq = null;
        StringBuffer nullBuffer = null;
        nulls.append(nullString).append(nullObject).append(nullSeq).append(nullBuffer);
        bad = bad + SbTest.eq(nulls.toString(), "nullnullnullnull");
        nulls.setLength(0);
        nulls.append(nullSeq, 1, 3);
        bad = bad + SbTest.eq(nulls.toString(), "ul");

        StringBuffer trim = new StringBuffer();
        trim.append("abc");
        trim.ensureCapacity(500);
        if (trim.capacity() != 500) {
            bad = bad + 1;
        }
        trim.trimToSize();
        if (trim.capacity() != 3) {
            bad = bad + 1;
        }

        StringBuffer fp = new StringBuffer();
        fp.append(0.5d).append(' ').append(0.5f).insert(0, 0.25f).insert(0, 0.25d);
        bad = bad + SbTest.eq(fp.toString(), "0.250.250.5 0.5");
        StringBuffer objs = new StringBuffer();
        Object seven = Integer.valueOf(7);
        objs.append(seven).insert(0, seven).insert(0, 3L).insert(0, false);
        bad = bad + SbTest.eq(objs.toString(), "false377");
        StringBuffer cs = new StringBuffer();
        CharSequence abcdef = "abcdef";
        cs.append(abcdef, 1, 3).insert(0, abcdef).insert(0, abcdef, 0, 2);
        bad = bad + SbTest.eq(cs.toString(), "ababcdefbc");

        // A StringBuilder can take a StringBuffer and the other way round.
        StringBuilder mixed = new StringBuilder();
        StringBuffer source = new StringBuffer("hola");
        mixed.append(source);
        bad = bad + SbTest.eq(mixed.toString(), "hola");
        StringBuffer target = new StringBuffer();
        target.append(source);
        bad = bad + SbTest.eq(target.toString(), "hola");
        return bad;
    }

    /**
     * The refusals.
     *
     * <p>Which exception is thrown is part of the contract and not a detail: code catches
     * {@code IndexOutOfBoundsException} and {@code StringIndexOutOfBoundsException} is a subclass
     * of it, so throwing the wrong one of the two is invisible to a catch and visible to
     * anything that looks at the class.
     */
    public static int limites() {
        int bad = 0;
        int which = 1;
        while (which <= 16) {
            bad = bad + SbTest.expectIndex(which);
            which = which + 1;
        }
        bad = bad + SbTest.expectIllegal(1);
        bad = bad + SbTest.expectIllegal(2);
        bad = bad + SbTest.expectIllegal(3);
        return bad;
    }

    static int expectIndex(int which) {
        try {
            SbTest.indexCase(which);
        } catch (IndexOutOfBoundsException ex) {
            return 0;
        }
        return 1;
    }

    static void indexCase(int which) {
        StringBuilder sb = new StringBuilder("abcde");
        if (which == 1) {
            sb.charAt(-1);
        } else if (which == 2) {
            sb.charAt(5);
        } else if (which == 3) {
            sb.setCharAt(5, 'x');
        } else if (which == 4) {
            sb.deleteCharAt(5);
        } else if (which == 5) {
            sb.deleteCharAt(-1);
        } else if (which == 6) {
            sb.delete(-1, 2);
        } else if (which == 7) {
            sb.delete(3, 1);
        } else if (which == 8) {
            sb.insert(6, "x");
        } else if (which == 9) {
            sb.insert(-1, "x");
        } else if (which == 10) {
            sb.substring(6);
        } else if (which == 11) {
            sb.substring(3, 2);
        } else if (which == 12) {
            sb.setLength(-1);
        } else if (which == 13) {
            sb.getChars(-1, 2, new char[5], 0);
        } else if (which == 14) {
            sb.replace(-1, 2, "x");
        } else if (which == 15) {
            CharSequence seq = "abc";
            sb.append(seq, 0, 4);
        } else if (which == 16) {
            sb.codePointAt(5);
        }
    }

    static int expectIllegal(int which) {
        try {
            SbTest.illegalCase(which);
        } catch (IllegalArgumentException ex) {
            return 0;
        }
        return 1;
    }

    static void illegalCase(int which) {
        StringBuilder sb = new StringBuilder("abc");
        if (which == 1) {
            sb.repeat('x', -1);
        } else if (which == 2) {
            sb.repeat("x", -1);
        } else if (which == 3) {
            sb.appendCodePoint(0x110000);
        }
    }

    public static int todo() {
        return SbTest.basico() + SbTest.capacidad() + SbTest.borrado() + SbTest.insercion()
                + SbTest.busqueda() + SbTest.puntos() + SbTest.reverso() + SbTest.repetir()
                + SbTest.lectura() + SbTest.comparar() + SbTest.buffer() + SbTest.limites();
    }

    public static void main(String[] args) {
        System.out.println("basico      " + SbTest.basico());
        System.out.println("capacidad   " + SbTest.capacidad());
        System.out.println("borrado     " + SbTest.borrado());
        System.out.println("insercion   " + SbTest.insercion());
        System.out.println("busqueda    " + SbTest.busqueda());
        System.out.println("puntos      " + SbTest.puntos());
        System.out.println("reverso     " + SbTest.reverso());
        System.out.println("repetir     " + SbTest.repetir());
        System.out.println("lectura     " + SbTest.lectura());
        System.out.println("comparar    " + SbTest.comparar());
        System.out.println("buffer      " + SbTest.buffer());
        System.out.println("limites     " + SbTest.limites());
        System.out.println("TOTAL       " + SbTest.todo());
    }
}
