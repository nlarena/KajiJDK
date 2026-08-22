package java.text;

/**
 * The canonical normalization engine behind {@link Normalizer}.
 *
 * <p>Normalization exists because Unicode lets the same text be spelled more than one way. "á" can
 * be one code point (U+00E1) or two (U+0061 U+0301), and those two strings are <em>canonically
 * equivalent</em> — they mean the same thing and must render the same, yet {@code equals} says they
 * differ. Normalization picks one spelling so comparison works.
 *
 * <p>NFD (decompose) and NFC (decompose, then compose) are the two canonical forms. NFD is the
 * simpler of the pair and NFC is defined in terms of it, which is why the engine only really has
 * one hard algorithm plus a recomposition pass.
 *
 * <p>Three pieces do the work:
 *
 * <ol>
 * <li><b>Decomposition.</b> A canonical decomposition can itself decompose — U+1E09 goes to
 *     U+00E7 plus an accent, and U+00E7 goes further — so the table stores the mapping already
 *     expanded and a lookup is final.</li>
 * <li><b>Canonical ordering.</b> Marks that attach to the same base must appear in a fixed order,
 *     or the two spellings would still differ. The order is a STABLE sort by combining class over
 *     each run of marks — stable because two marks of the SAME class are typographically distinct
 *     and reordering them would change the meaning.</li>
 * <li><b>Composition.</b> Walk the decomposed string pairing each starter with the following marks,
 *     honouring the BLOCKING rule: a mark cannot reach its starter if a mark of equal-or-higher class
 *     sits between them. Composition reads its own table of primary pairs rather than the
 *     decomposition table backwards — see {@link NormTables} for why those are not the same
 *     thing.</li>
 * </ol>
 *
 * <p>Hangul is handled by arithmetic rather than by table. A syllable is
 * {@code base + (L*21 + V)*28 + T}, so decomposing is division and composing is multiplication —
 * which is why 11172 code points cost no data at all.
 */
final class NormImpl {

    private NormImpl() {
    }

    // ---- surrogate arithmetic ----
    //
    // A code point above U+FFFF is stored as a SURROGATE PAIR: a high unit in D800..DBFF and a low
    // one in DC00..DFFF, together encoding cp - 0x10000 in twenty bits. KajiLibrary's Character
    // exposes none of this, so the three operations that need it live here.

    private static boolean isHighSurrogate(char c) {
        return c >= 0xd800 && c <= 0xdbff;
    }

    private static boolean isLowSurrogate(char c) {
        return c >= 0xdc00 && c <= 0xdfff;
    }

    private static int codePointAt(CharSequence s, int i) {
        char c = s.charAt(i);
        if (NormImpl.isHighSurrogate(c) && i + 1 < s.length()
                && NormImpl.isLowSurrogate(s.charAt(i + 1))) {
            return 0x10000 + ((c - 0xd800) << 10) + (s.charAt(i + 1) - 0xdc00);
        }
        return c;
    }

    private static int charCount(int cp) {
        if (cp >= 0x10000) {
            return 2;
        }
        return 1;
    }

    private static void appendCodePoint(StringBuilder b, int cp) {
        if (cp < 0x10000) {
            b.append((char) cp);
            return;
        }
        int v = cp - 0x10000;
        b.append((char) (0xd800 + (v >> 10)));
        b.append((char) (0xdc00 + (v & 0x3ff)));
    }


    // Hangul composition constants (Unicode 3.12). Methods rather than static-final primitives,
    // which our compiler leaves uninitialized (finding #112).
    private static int sBase() {
        return 0xAC00;
    }

    private static int lBase() {
        return 0x1100;
    }

    private static int vBase() {
        return 0x1161;
    }

    private static int tBase() {
        return 0x11A7;
    }

    private static int vCount() {
        return 21;
    }

    private static int tCount() {
        return 28;
    }

    private static int sCount() {
        return 11172;
    }

    // ---- table lookup ----

    /** The canonical combining rank of a code point, or 0 (the default) when it has none. */
    static int combiningClass(int cp) {
        String data = NormTables.markData();
        String key = Integer.toHexString(cp) + ":";
        int at = NormImpl.findEntry(data, key);
        if (at < 0) {
            return 0;
        }
        int start = at + key.length();
        int end = start;
        while (end < data.length() && data.charAt(end) != ';') {
            end = end + 1;
        }
        return NormImpl.parseHex(data.substring(start, end));
    }

    /** The canonical decomposition of a code point as code points, or null when it has none. */
    static int[] decomposition(int cp) {
        String data = NormTables.decompData();
        String key = Integer.toHexString(cp) + ":";
        int at = NormImpl.findEntry(data, key);
        if (at < 0) {
            return null;
        }
        int start = at + key.length();
        int end = start;
        while (end < data.length() && data.charAt(end) != ';') {
            end = end + 1;
        }
        String body = data.substring(start, end);
        int count = 1;
        int i = 0;
        while (i < body.length()) {
            if (body.charAt(i) == ':') {
                count = count + 1;
            }
            i = i + 1;
        }
        int[] out = new int[count];
        int w = 0;
        int from = 0;
        i = 0;
        while (i <= body.length()) {
            if (i == body.length() || body.charAt(i) == ':') {
                out[w] = NormImpl.parseHex(body.substring(from, i));
                w = w + 1;
                from = i + 1;
            }
            i = i + 1;
        }
        return out;
    }

    // Finds `key` at an entry boundary — the start of the data, or just after a ';'. Without the
    // boundary check "301:" would also match inside "10301:...".
    private static int findEntry(String data, String key) {
        int at = 0;
        int found = -1;
        while (at <= data.length() - key.length()) {
            boolean matches = true;
            int k = 0;
            while (k < key.length()) {
                if (data.charAt(at + k) != key.charAt(k)) {
                    matches = false;
                    k = key.length();
                } else {
                    k = k + 1;
                }
            }
            if (matches && (at == 0 || data.charAt(at - 1) == ';')) {
                found = at;
                at = data.length();
            } else {
                at = at + 1;
            }
        }
        return found;
    }

    private static int parseHex(String s) {
        int v = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            int d = 0;
            if (c >= '0' && c <= '9') {
                d = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                d = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                d = c - 'A' + 10;
            }
            v = v * 16 + d;
            i = i + 1;
        }
        return v;
    }

    // ---- Hangul, by arithmetic ----

    private static boolean isHangulSyllable(int cp) {
        return cp >= NormImpl.sBase() && cp < NormImpl.sBase() + NormImpl.sCount();
    }

    private static void decomposeHangul(int cp, StringBuilder out) {
        int index = cp - NormImpl.sBase();
        int l = NormImpl.lBase() + index / (NormImpl.vCount() * NormImpl.tCount());
        int v = NormImpl.vBase() + (index % (NormImpl.vCount() * NormImpl.tCount())) / NormImpl.tCount();
        int t = NormImpl.tBase() + index % NormImpl.tCount();
        out.append((char) l);
        out.append((char) v);
        // A trailing consonant of index 0 means "none", so it is simply not written.
        if (t != NormImpl.tBase()) {
            out.append((char) t);
        }
    }

    /** Composes a Hangul pair, or -1 when the two do not form a syllable. */
    private static int composeHangul(int a, int b) {
        int lIndex = a - NormImpl.lBase();
        if (lIndex >= 0 && lIndex < 19) {
            int vIndex = b - NormImpl.vBase();
            if (vIndex >= 0 && vIndex < NormImpl.vCount()) {
                return NormImpl.sBase()
                        + (lIndex * NormImpl.vCount() + vIndex) * NormImpl.tCount();
            }
        }
        if (NormImpl.isHangulSyllable(a)) {
            int tIndex = b - NormImpl.tBase();
            // Only a syllable with no trailing consonant can take one.
            if (tIndex > 0 && tIndex < NormImpl.tCount()
                    && (a - NormImpl.sBase()) % NormImpl.tCount() == 0) {
                return a + tIndex;
            }
        }
        return -1;
    }

    // ---- the three passes ----

    private static void decomposeInto(int cp, StringBuilder out) {
        if (NormImpl.isHangulSyllable(cp)) {
            NormImpl.decomposeHangul(cp, out);
            return;
        }
        int[] parts = NormImpl.decomposition(cp);
        if (parts == null) {
            NormImpl.appendCodePoint(out, cp);
            return;
        }
        // No recursion: the table holds the FULL decomposition, so a target never decomposes
        // again. Recursing anyway would cost one fruitless table scan per character produced.
        int i = 0;
        while (i < parts.length) {
            NormImpl.appendCodePoint(out, parts[i]);
            i = i + 1;
        }
    }

    // A stable insertion sort over each run of non-zero-class marks. Insertion sort is not a
    // fallback here — it is the algorithm the standard specifies, because it is stable and the runs
    // are almost always one or two marks long.
    private static int[] canonicalOrder(int[] cps) {
        int i = 1;
        while (i < cps.length) {
            int cc = NormImpl.combiningClass(cps[i]);
            if (cc != 0) {
                int j = i;
                while (j > 0) {
                    int prev = NormImpl.combiningClass(cps[j - 1]);
                    if (prev == 0 || prev <= cc) {
                        j = 0;
                    } else {
                        int tmp = cps[j];
                        cps[j] = cps[j - 1];
                        cps[j - 1] = tmp;
                        j = j - 1;
                    }
                }
            }
            i = i + 1;
        }
        return cps;
    }

    static String decompose(CharSequence src) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < src.length()) {
            int cp = NormImpl.codePointAt(src, i);
            NormImpl.decomposeInto(cp, out);
            i = i + NormImpl.charCount(cp);
        }
        String flat = out.toString();
        int[] cps = NormImpl.toCodePoints(flat);
        cps = NormImpl.canonicalOrder(cps);
        return NormImpl.fromCodePoints(cps);
    }

    static String compose(CharSequence src) {
        int[] cps = NormImpl.toCodePoints(NormImpl.decompose(src));
        if (cps.length == 0) {
            return "";
        }
        // The output is written IN ORDER and the starter is patched IN PLACE. Collecting the
        // non-composing characters separately and prefixing the starter at the end looks simpler
        // and is wrong: any text with two starters comes out reordered.
        int[] out = new int[cps.length];
        out[0] = cps[0];
        int written = 1;
        // Where the starter that a following mark would attach to sits, or -1 while the text has
        // not produced one yet — a string may begin with a mark.
        int starterAt = -1;
        if (NormImpl.combiningClass(cps[0]) == 0) {
            starterAt = 0;
        }
        // The class of the last character written AFTER the starter, or 0 when the starter itself
        // is the last thing written. This is what the blocking rule reads.
        int lastClass = 0;
        int i = 1;
        while (i < cps.length) {
            int cp = cps[i];
            int cc = NormImpl.combiningClass(cp);
            boolean joined = false;
            // Nothing may sit between the starter and this character except marks of strictly
            // lower class. lastClass == 0 means nothing sits between them at all, which is also
            // the only way two starters (Hangul L and V) are allowed to join.
            if (starterAt >= 0 && (lastClass == 0 || lastClass < cc)) {
                int composed = NormImpl.composePair(out[starterAt], cp);
                if (composed >= 0) {
                    out[starterAt] = composed;
                    joined = true;
                }
            }
            if (!joined) {
                if (cc == 0) {
                    starterAt = written;
                    lastClass = 0;
                } else {
                    lastClass = cc;
                }
                out[written] = cp;
                written = written + 1;
            }
            i = i + 1;
        }
        int[] result = new int[written];
        int k = 0;
        while (k < written) {
            result[k] = out[k];
            k = k + 1;
        }
        return NormImpl.fromCodePoints(result);
    }

    /** Composes two code points into their primary composite, or -1 when they do not combine. */
    private static int composePair(int a, int b) {
        int hangul = NormImpl.composeHangul(a, b);
        if (hangul >= 0) {
            return hangul;
        }
        String data = NormTables.compData();
        String key = Integer.toHexString(a) + ":" + Integer.toHexString(b) + ":";
        int at = NormImpl.findEntry(data, key);
        if (at < 0) {
            return -1;
        }
        int start = at + key.length();
        int end = start;
        while (end < data.length() && data.charAt(end) != ';') {
            end = end + 1;
        }
        return NormImpl.parseHex(data.substring(start, end));
    }


    private static int[] toCodePoints(String s) {
        int count = 0;
        int i = 0;
        while (i < s.length()) {
            int cp = NormImpl.codePointAt(s, i);
            i = i + NormImpl.charCount(cp);
            count = count + 1;
        }
        int[] out = new int[count];
        int w = 0;
        i = 0;
        while (i < s.length()) {
            int cp = NormImpl.codePointAt(s, i);
            out[w] = cp;
            w = w + 1;
            i = i + NormImpl.charCount(cp);
        }
        return out;
    }

    private static String fromCodePoints(int[] cps) {
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < cps.length) {
            NormImpl.appendCodePoint(b, cps[i]);
            i = i + 1;
        }
        return b.toString();
    }
}
