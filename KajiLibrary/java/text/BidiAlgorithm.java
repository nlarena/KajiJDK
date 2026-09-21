package java.text;

/**
 * Unicode's Bidirectional Algorithm (UAX #9), which is all there is behind {@link Bidi}.
 *
 * <p>The problem it solves: text mixing Arabic or Hebrew with Latin is STORED in logical order --the
 * order it is read in-- but DRAWN in a different one, and there is no local rule that settles it. A
 * number inside an Arabic sentence is written left to right even though the sentence runs the other
 * way, and a punctuation mark between two languages takes the direction of what surrounds it. The
 * algorithm assigns each character an embedding LEVEL --even is left to right, odd is right to
 * left-- and everything else follows from those levels.
 *
 * <p>It is not public because the JDK does not expose it either: the visible face is {@code Bidi}.
 * It lives apart so the public class reads as an API and not as a rule interpreter.
 *
 * <p><b>Which rules are here.</b> P2-P3 (the paragraph's direction), X1-X10 (explicit embeddings,
 * overrides and isolates, with the 125-level stack and the overflow counters), W1-W7 (resolving
 * weak types), N1-N2 (neutrals), I1-I2 (implicit levels) and L1 (putting the separators and the
 * trailing space back).
 *
 * <p><b>Which rule is NOT here, and what that implies.</b> <b>N0</b>, the bracket-pair rule Unicode
 * 6.3 added, needs the {@code BidiBrackets.txt} table --which character closes which-- that this
 * library does not carry. Without it brackets resolve as ordinary neutrals, which is exactly what
 * the algorithm did before 6.3. The difference shows only when a bracket pair encloses text whose
 * direction is opposite to what surrounds it; in every other case the result is identical. It is
 * documented instead of inventing a partial table: half a bracket table would give correct levels
 * for some pairs and wrong ones for others, with no way of telling which is which.
 */
final class BidiAlgorithm {

    // The types are Character.getDirectionality's, with the same numbers: reusing the numbering
    // avoids a translation table that could only ever be wrong.
    static final byte L = 0;
    static final byte R = 1;
    static final byte AL = 2;
    static final byte EN = 3;
    static final byte ES = 4;
    static final byte ET = 5;
    static final byte AN = 6;
    static final byte CS = 7;
    static final byte NSM = 8;
    static final byte BN = 9;
    static final byte B = 10;
    static final byte S = 11;
    static final byte WS = 12;
    static final byte ON = 13;
    static final byte LRE = 14;
    static final byte LRO = 15;
    static final byte RLE = 16;
    static final byte RLO = 17;
    static final byte PDF = 18;
    static final byte LRI = 19;
    static final byte RLI = 20;
    static final byte FSI = 21;
    static final byte PDI = 22;

    static final int MAX_DEPTH = 125;

    private final char[] text;
    private final byte[] initialType;
    private final byte[] types;
    private final byte[] level;
    private final int[] matchingPdi;      // by position: index of the PDI that closes an isolate
    private final int[] matchingStart;   // by the PDI's position: index of the opening isolate
    private final int n;
    private byte paragraphLevel;

    BidiAlgorithm(char[] text, byte[] embeddings, int baseLevel) {
        this.text = text;
        this.n = text.length;
        this.initialType = new byte[this.n];
        this.types = new byte[this.n];
        this.level = new byte[this.n];
        this.matchingPdi = new int[this.n];
        this.matchingStart = new int[this.n];
        for (int i = 0; i < this.n; i = i + 1) {
            byte t = Character.getDirectionality(text[i]);
            if (t < 0) {
                t = BidiAlgorithm.L;
            }
            this.initialType[i] = t;
            this.types[i] = t;
        }
        this.matchIsolates();
        if (baseLevel >= 0) {
            this.paragraphLevel = (byte) baseLevel;
        } else {
            // A baseLevel < 0 asks "work it out from the text" (P2/P3); -2 also means "if there is
            // no strong character, left to right" and -1, "right to left".
            int deduced = this.firstStrong(0, this.n);
            if (deduced < 0) {
                if (baseLevel == -1) {
                    deduced = 1;
                } else {
                    deduced = 0;
                }
            }
            this.paragraphLevel = (byte) deduced;
        }
        this.explicit();
        this.bySequences();
        this.applyL1(0, this.n);
        if (embeddings != null) {
            this.applyEmbeddings(embeddings);
        }
    }

    byte paragraphLevel() {
        return this.paragraphLevel;
    }

    byte[] levels() {
        byte[] out = new byte[this.n];
        for (int i = 0; i < this.n; i = i + 1) {
            out[i] = this.level[i];
        }
        return out;
    }

    /**
     * The embeddings the caller imposed by hand, applied over the ones the algorithm computed.
     *
     * <p>A positive value is an embedding and a negative one an override; zero leaves the computed
     * level. It is applied at the end and not at the start because the contract says it replaces the
     * result, not that it feeds the computation.
     */
    private void applyEmbeddings(byte[] embeddings) {
        for (int i = 0; i < this.n && i < embeddings.length; i = i + 1) {
            byte e = embeddings[i];
            if (e == 0) {
                continue;
            }
            int requestedLevel = e;
            if (requestedLevel < 0) {
                requestedLevel = -requestedLevel;
            }
            if (requestedLevel > BidiAlgorithm.MAX_DEPTH) {
                continue;
            }
            if (e > 0) {
                // Embedding: the text keeps its own direction inside the imposed level.
                if (this.level[i] < requestedLevel) {
                    this.level[i] = (byte) requestedLevel;
                }
            } else {
                // Override: the level's direction overrules the character's.
                this.level[i] = (byte) requestedLevel;
            }
        }
    }

    // P2/P3: the first strong character not inside an isolate. It returns 0 (L), 1 (R) or -1 if
    // there is none.
    private int firstStrong(int from, int to) {
        int i = from;
        while (i < to) {
            byte t = this.initialType[i];
            if (t == BidiAlgorithm.L) {
                return 0;
            }
            if (t == BidiAlgorithm.R || t == BidiAlgorithm.AL) {
                return 1;
            }
            if (t == BidiAlgorithm.LRI || t == BidiAlgorithm.RLI || t == BidiAlgorithm.FSI) {
                // An isolate's contents do NOT count towards the outside direction: that is what
                // "isolating" means. It skips ahead to the PDI that closes it.
                int closing = this.matchingPdi[i];
                if (closing < 0) {
                    return -1;
                }
                i = closing;
            }
            i = i + 1;
        }
        return -1;
    }

    // BD9: each isolate is paired with its PDI, counting the nested ones.
    private void matchIsolates() {
        for (int i = 0; i < this.n; i = i + 1) {
            this.matchingPdi[i] = -1;
            this.matchingStart[i] = -1;
        }
        int[] stack = new int[this.n + 1];
        int top = 0;
        for (int i = 0; i < this.n; i = i + 1) {
            byte t = this.initialType[i];
            if (t == BidiAlgorithm.LRI || t == BidiAlgorithm.RLI || t == BidiAlgorithm.FSI) {
                stack[top] = i;
                top = top + 1;
            } else if (t == BidiAlgorithm.PDI && top > 0) {
                top = top - 1;
                this.matchingPdi[stack[top]] = i;
                this.matchingStart[i] = stack[top];
            }
        }
    }

    // X1-X8.
    private void explicit() {
        byte[] levelStack = new byte[BidiAlgorithm.MAX_DEPTH + 3];
        byte[] overrideStack = new byte[BidiAlgorithm.MAX_DEPTH + 3];
        boolean[] isolateStack = new boolean[BidiAlgorithm.MAX_DEPTH + 3];
        int top = 0;
        levelStack[0] = this.paragraphLevel;
        overrideStack[0] = -1;
        isolateStack[0] = false;
        int isolateOverflow = 0;
        int embeddingOverflow = 0;
        int validIsolates = 0;

        for (int i = 0; i < this.n; i = i + 1) {
            byte t = this.initialType[i];
            if (t == BidiAlgorithm.RLE || t == BidiAlgorithm.LRE || t == BidiAlgorithm.RLO
                    || t == BidiAlgorithm.LRO) {
                this.level[i] = levelStack[top];
                boolean rightToLeft = t == BidiAlgorithm.RLE || t == BidiAlgorithm.RLO;
                int raised = BidiAlgorithm.nextLevel(levelStack[top], rightToLeft);
                if (raised <= BidiAlgorithm.MAX_DEPTH && isolateOverflow == 0 && embeddingOverflow == 0) {
                    top = top + 1;
                    levelStack[top] = (byte) raised;
                    isolateStack[top] = false;
                    if (t == BidiAlgorithm.RLO) {
                        overrideStack[top] = BidiAlgorithm.R;
                    } else if (t == BidiAlgorithm.LRO) {
                        overrideStack[top] = BidiAlgorithm.L;
                    } else {
                        overrideStack[top] = -1;
                    }
                } else if (isolateOverflow == 0) {
                    embeddingOverflow = embeddingOverflow + 1;
                }
            } else if (t == BidiAlgorithm.RLI || t == BidiAlgorithm.LRI || t == BidiAlgorithm.FSI) {
                boolean rightToLeft;
                if (t == BidiAlgorithm.FSI) {
                    // X5c: an FSI takes the direction of the first strong character in its own
                    // contents.
                    int closing = this.matchingPdi[i];
                    int to = this.n;
                    if (closing >= 0) {
                        to = closing;
                    }
                    rightToLeft = this.firstStrong(i + 1, to) == 1;
                } else {
                    rightToLeft = t == BidiAlgorithm.RLI;
                }
                this.level[i] = levelStack[top];
                if (overrideStack[top] >= 0) {
                    this.types[i] = overrideStack[top];
                }
                int raised = BidiAlgorithm.nextLevel(levelStack[top], rightToLeft);
                if (raised <= BidiAlgorithm.MAX_DEPTH && isolateOverflow == 0 && embeddingOverflow == 0) {
                    validIsolates = validIsolates + 1;
                    top = top + 1;
                    levelStack[top] = (byte) raised;
                    overrideStack[top] = -1;
                    isolateStack[top] = true;
                } else {
                    isolateOverflow = isolateOverflow + 1;
                }
            } else if (t == BidiAlgorithm.PDI) {
                if (isolateOverflow > 0) {
                    isolateOverflow = isolateOverflow - 1;
                } else if (validIsolates > 0) {
                    embeddingOverflow = 0;
                    while (!isolateStack[top]) {
                        top = top - 1;
                    }
                    top = top - 1;
                    validIsolates = validIsolates - 1;
                }
                this.level[i] = levelStack[top];
                if (overrideStack[top] >= 0) {
                    this.types[i] = overrideStack[top];
                }
            } else if (t == BidiAlgorithm.PDF) {
                this.level[i] = levelStack[top];
                if (isolateOverflow > 0) {
                    // A PDF does not close an overflowed isolate: the two mechanisms do not
                    // cross.
                    continue;
                }
                if (embeddingOverflow > 0) {
                    embeddingOverflow = embeddingOverflow - 1;
                } else if (!isolateStack[top] && top >= 1) {
                    top = top - 1;
                }
            } else if (t == BidiAlgorithm.B) {
                top = 0;
                isolateOverflow = 0;
                embeddingOverflow = 0;
                validIsolates = 0;
                this.level[i] = this.paragraphLevel;
            } else {
                this.level[i] = levelStack[top];
                if (overrideStack[top] >= 0) {
                    this.types[i] = overrideStack[top];
                }
            }
        }
    }

    private static int nextLevel(int level, boolean rightToLeft) {
        if (rightToLeft) {
            return (level + 1) | 1;
        }
        return (level + 2) & (~1);
    }

    // X9: the explicit controls and the BNs take no part in the rules that follow. They are not
    // deleted --that would mean reindexing everything-- but skipped, and at the end they inherit the
    // previous one's level.
    private boolean removed(int i) {
        byte t = this.initialType[i];
        return t == BidiAlgorithm.RLE || t == BidiAlgorithm.LRE || t == BidiAlgorithm.RLO
                || t == BidiAlgorithm.LRO || t == BidiAlgorithm.PDF || t == BidiAlgorithm.BN;
    }

    // X10: it builds each isolating run sequence and runs the W, N and I rules over it.
    private void bySequences() {
        boolean[] seen = new boolean[this.n];
        for (int i = 0; i < this.n; i = i + 1) {
            if (this.removed(i) || seen[i]) {
                continue;
            }
            // A sequence starts at a run whose first character is not a PDI closing an isolate:
            // that PDI already belongs to the sequence its isolate opened.
            if (this.initialType[i] == BidiAlgorithm.PDI && this.matchingStart[i] >= 0) {
                continue;
            }
            int[] indices = this.sequenceFrom(i, seen);
            if (indices.length == 0) {
                continue;
            }
            this.resolve(indices);
        }
        // X9 deletes the explicit controls and the BNs, so their level is "whatever the
        // implementation likes". The choice matters all the same, because `getLevelAt` returns them:
        // here they inherit the NEXT character's level, not the previous one's. Forwards and not
        // backwards because an RLE opens the embedding that comes after --it is part of it, not of
        // what came before-- and a closing PDF sticks to what follows. Verified against JDK 25 over
        // text with RLE/PDF and with RLO: with the backwards rule both cases come out different.
        for (int i = this.n - 1; i >= 0; i = i - 1) {
            if (this.removed(i)) {
                if (i + 1 < this.n) {
                    this.level[i] = this.level[i + 1];
                } else {
                    this.level[i] = this.paragraphLevel;
                }
            }
        }
    }

    private int[] sequenceFrom(int start, boolean[] seen) {
        int[] buffer = new int[this.n];
        int count = 0;
        int i = start;
        byte runLevel = this.level[start];
        while (i < this.n) {
            // A level run: consecutive characters (skipping the removed ones) at the same
            // level.
            while (i < this.n && (this.removed(i) || this.level[i] == runLevel)) {
                if (!this.removed(i)) {
                    seen[i] = true;
                    buffer[count] = i;
                    count = count + 1;
                }
                i = i + 1;
            }
            if (count == 0) {
                break;
            }
            int last = buffer[count - 1];
            byte lastType = this.initialType[last];
            // If the run ends in an isolate with its PDI, the sequence continues in the run where
            // that PDI is: that is what makes the text outside the isolate read as continuous.
            if ((lastType == BidiAlgorithm.LRI || lastType == BidiAlgorithm.RLI
                    || lastType == BidiAlgorithm.FSI)
                    && this.matchingPdi[last] >= 0) {
                i = this.matchingPdi[last];
            } else {
                break;
            }
        }
        int[] out = new int[count];
        for (int k = 0; k < count; k = k + 1) {
            out[k] = buffer[k];
        }
        return out;
    }

    private void resolve(int[] idx) {
        int m = idx.length;
        byte sequenceLevel = this.level[idx[0]];

        // sos/eos: the "outside" direction on each side, which is that of the higher level between
        // the sequence and its neighbour. Without this, a neutral at the edge would have nothing to
        // resolve against.
        byte sos = this.directionOf(this.greaterOf(sequenceLevel, this.levelBefore(idx[0])));
        int last = idx[m - 1];
        byte lastType = this.initialType[last];
        byte eos;
        if ((lastType == BidiAlgorithm.LRI || lastType == BidiAlgorithm.RLI
                || lastType == BidiAlgorithm.FSI)
                && this.matchingPdi[last] < 0) {
            // An unclosed isolate leaves the rest of the paragraph inside it: the sequence's edge
            // is the paragraph's.
            eos = this.directionOf(this.greaterOf(sequenceLevel, this.paragraphLevel));
        } else {
            eos = this.directionOf(this.greaterOf(sequenceLevel, this.levelAfter(last)));
        }

        byte[] t = new byte[m];
        for (int k = 0; k < m; k = k + 1) {
            t[k] = this.types[idx[k]];
        }

        // W1: a non-spacing mark takes the previous one's type; after an isolate or a PDI, ON.
        byte previous = sos;
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == BidiAlgorithm.NSM) {
                if (previous == BidiAlgorithm.LRI || previous == BidiAlgorithm.RLI
                        || previous == BidiAlgorithm.FSI || previous == BidiAlgorithm.PDI) {
                    t[k] = BidiAlgorithm.ON;
                } else {
                    t[k] = previous;
                }
            }
            previous = t[k];
        }

        // W2: a European number becomes Arabic if the last strong character was an Arabic letter.
        byte strong = sos;
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == BidiAlgorithm.L || t[k] == BidiAlgorithm.R || t[k] == BidiAlgorithm.AL) {
                strong = t[k];
            } else if (t[k] == BidiAlgorithm.EN && strong == BidiAlgorithm.AL) {
                t[k] = BidiAlgorithm.AN;
            }
        }

        // W3: the Arabic letter has played its part in W2 and from here on is R.
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == BidiAlgorithm.AL) {
                t[k] = BidiAlgorithm.R;
            }
        }

        // W4: a separator BETWEEN two numbers of the same type becomes a number. "1.234" is a
        // number; "1." followed by something else is not.
        for (int k = 1; k + 1 < m; k = k + 1) {
            if (t[k] == BidiAlgorithm.ES && t[k - 1] == BidiAlgorithm.EN
                    && t[k + 1] == BidiAlgorithm.EN) {
                t[k] = BidiAlgorithm.EN;
            } else if (t[k] == BidiAlgorithm.CS && t[k - 1] == t[k + 1]
                    && (t[k - 1] == BidiAlgorithm.EN || t[k - 1] == BidiAlgorithm.AN)) {
                t[k] = t[k - 1];
            }
        }

        // W5: a run of terminators next to a European number becomes a number ("$12", "12%").
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] != BidiAlgorithm.ET) {
                continue;
            }
            int end = k;
            while (end < m && t[end] == BidiAlgorithm.ET) {
                end = end + 1;
            }
            boolean adjacent = (k > 0 && t[k - 1] == BidiAlgorithm.EN)
                    || (end < m && t[end] == BidiAlgorithm.EN);
            if (adjacent) {
                for (int j = k; j < end; j = j + 1) {
                    t[j] = BidiAlgorithm.EN;
                }
            }
            k = end - 1;
        }

        // W6: whatever is left of separators and terminators is neutral.
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == BidiAlgorithm.ET || t[k] == BidiAlgorithm.ES || t[k] == BidiAlgorithm.CS) {
                t[k] = BidiAlgorithm.ON;
            }
        }

        // W7: a European number preceded by Latin text IS Latin text.
        strong = sos;
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == BidiAlgorithm.L || t[k] == BidiAlgorithm.R) {
                strong = t[k];
            } else if (t[k] == BidiAlgorithm.EN && strong == BidiAlgorithm.L) {
                t[k] = BidiAlgorithm.L;
            }
        }

        // N1/N2: a run of neutrals takes the surrounding direction if it is the same on both
        // sides, and otherwise the embedding's. Numbers count as right to left here.
        for (int k = 0; k < m; k = k + 1) {
            if (!BidiAlgorithm.isNeutral(t[k])) {
                continue;
            }
            int end = k;
            while (end < m && BidiAlgorithm.isNeutral(t[end])) {
                end = end + 1;
            }
            byte toTheLeft;
            if (k == 0) {
                toTheLeft = sos;
            } else {
                toTheLeft = BidiAlgorithm.asStrong(t[k - 1]);
            }
            byte toTheRight;
            if (end == m) {
                toTheRight = eos;
            } else {
                toTheRight = BidiAlgorithm.asStrong(t[end]);
            }
            byte resolved;
            if (toTheLeft == toTheRight) {
                resolved = toTheLeft;
            } else {
                resolved = this.directionOf(sequenceLevel);
            }
            for (int j = k; j < end; j = j + 1) {
                t[j] = resolved;
            }
            k = end - 1;
        }

        // I1/I2: the implicit levels. At an even level right-to-left rises by one and numbers by
        // two; at an odd one, left-to-right and numbers rise by one.
        for (int k = 0; k < m; k = k + 1) {
            byte nv = sequenceLevel;
            if ((sequenceLevel & 1) == 0) {
                if (t[k] == BidiAlgorithm.R) {
                    nv = (byte) (sequenceLevel + 1);
                } else if (t[k] == BidiAlgorithm.AN || t[k] == BidiAlgorithm.EN) {
                    nv = (byte) (sequenceLevel + 2);
                }
            } else {
                if (t[k] == BidiAlgorithm.L || t[k] == BidiAlgorithm.AN
                        || t[k] == BidiAlgorithm.EN) {
                    nv = (byte) (sequenceLevel + 1);
                }
            }
            this.level[idx[k]] = nv;
            this.types[idx[k]] = t[k];
        }
    }

    private static boolean isNeutral(byte t) {
        return t == BidiAlgorithm.B || t == BidiAlgorithm.S || t == BidiAlgorithm.WS
                || t == BidiAlgorithm.ON || t == BidiAlgorithm.LRI || t == BidiAlgorithm.RLI
                || t == BidiAlgorithm.FSI || t == BidiAlgorithm.PDI;
    }

    private static byte asStrong(byte t) {
        if (t == BidiAlgorithm.EN || t == BidiAlgorithm.AN || t == BidiAlgorithm.R) {
            return BidiAlgorithm.R;
        }
        return BidiAlgorithm.L;
    }

    private byte directionOf(int level) {
        if ((level & 1) == 0) {
            return BidiAlgorithm.L;
        }
        return BidiAlgorithm.R;
    }

    private int greaterOf(int a, int b) {
        if (a > b) {
            return a;
        }
        return b;
    }

    private int levelBefore(int i) {
        int k = i - 1;
        while (k >= 0 && this.removed(k)) {
            k = k - 1;
        }
        if (k < 0) {
            return this.paragraphLevel;
        }
        return this.level[k];
    }

    private int levelAfter(int i) {
        int k = i + 1;
        while (k < this.n && this.removed(k)) {
            k = k + 1;
        }
        if (k >= this.n) {
            return this.paragraphLevel;
        }
        return this.level[k];
    }

    /**
     * L1 over a range: the separators and the space preceding them go back to the paragraph's level.
     *
     * <p>It is what stops a line's trailing space being drawn on the wrong side. It is applied over
     * the ORIGINAL types, not the resolved ones: a space N1 turned into "right to left" is still a
     * space as far as this rule is concerned.
     */
    void applyL1(int from, int to) {
        boolean queued = true;
        for (int i = to - 1; i >= from; i = i - 1) {
            byte t = this.initialType[i];
            if (t == BidiAlgorithm.B || t == BidiAlgorithm.S) {
                this.level[i] = this.paragraphLevel;
                queued = true;
            } else if (queued && (t == BidiAlgorithm.WS || t == BidiAlgorithm.LRI
                    || t == BidiAlgorithm.RLI || t == BidiAlgorithm.FSI
                    || t == BidiAlgorithm.PDI || this.removed(i))) {
                this.level[i] = this.paragraphLevel;
            } else {
                queued = false;
            }
        }
    }
}
