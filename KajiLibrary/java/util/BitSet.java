package java.util;

// A set of non-negative ints stored as *bits*: bit `i` of the backing `long[]` says whether
// `i` is in the set. That is the whole idea, and it is why this class exists next to
// {@link HashSet} — for a dense set of small integers a HashSet spends a node and a boxed
// Integer per element (tens of bytes), while this spends one bit. A million-element BitSet is
// 128 KB; the same set of `Integer`s is a couple of orders of magnitude larger and chases a
// pointer per lookup.
//
// The price is that it only holds non-negative ints, and that it is dense: a BitSet holding
// only the number 1_000_000 still allocates the 125 KB of words below it. Sparse sets of large
// numbers are exactly the case where HashSet wins.
//
// The layout is two numbers away from trivial:
//
//   - bit `i` lives in `words[i >> 6]` at position `i & 63` — the shift is a divide by 64 and
//     the mask is the remainder, because a `long` holds 64 bits. Every operation here is those
//     two lines plus one bitwise instruction.
//   - `wordsInUse` is how many words are non-zero-prefixed: the invariant is that
//     `words[wordsInUse - 1] != 0`, so `length()` and `equals` never have to scan trailing
//     zeros. Anything that can clear the top word has to restore it.
//
// The set operations (and/or/xor/andNot) are the real payoff: they are one machine instruction
// per 64 elements, which is why bitsets are the standard representation for dataflow analysis,
// reachability, and any other place a compiler intersects sets in a loop.
//
// Subset of the JDK's: toByteArray/valueOf(byte[]) and the NIO buffer factories, get(int,int),
// previousClearBit, stream() and clone are not modelled.
public class BitSet {

    // The bits, six bits of index per word. Never null; may hold trailing zero words.
    private long[] words;

    // Invariant: words[wordsInUse - 1] != 0, and words[i] == 0 for i >= wordsInUse.
    private int wordsInUse;

    public BitSet() {
        words = new long[1];
        wordsInUse = 0;
    }

    // `nbits` is a *hint*: the set grows on demand regardless, this only avoids the first
    // few reallocations.
    public BitSet(int nbits) {
        if (nbits < 0) {
            throw new NegativeArraySizeException("nbits < 0");
        }
        words = new long[wordIndex(nbits - 1) + 1];
        wordsInUse = 0;
    }

    // Which word bit `i` lives in. A method rather than the JDK's `static final int
    // ADDRESS_BITS_PER_WORD = 6`, because a `static final` *primitive* reads back as 0 at run
    // time under our javac (finding #112) — which would silently put every bit in word 0.
    private static int wordIndex(int bitIndex) {
        return bitIndex >> 6;
    }

    // The single bit `i` selects inside its word. `1L << i` uses only the low six bits of the
    // shift distance, so the `& 63` the layout implies is already done by the instruction.
    private static long bitMask(int bitIndex) {
        return 1L << bitIndex;
    }

    private void checkIndex(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0");
        }
    }

    // Make sure `words[wordIndex]` exists, doubling rather than fitting exactly so that a
    // loop of ascending `set` calls stays amortized O(1) instead of O(n) per call.
    private void expandTo(int wordIndex) {
        int needed = wordIndex + 1;
        if (words.length < needed) {
            int newLength = words.length * 2;
            if (newLength < needed) {
                newLength = needed;
            }
            long[] bigger = new long[newLength];
            for (int i = 0; i < words.length; i++) {
                bigger[i] = words[i];
            }
            words = bigger;
        }
        if (wordsInUse < needed) {
            wordsInUse = needed;
        }
    }

    // Restore the `words[wordsInUse - 1] != 0` invariant after an operation that may have
    // zeroed the top words. Everything that clears bits ends here.
    private void recalculateWordsInUse() {
        int i = wordsInUse - 1;
        while (i >= 0 && words[i] == 0L) {
            i = i - 1;
        }
        wordsInUse = i + 1;
    }

    // --- single bits ----------------------------------------------------------------

    public void set(int bitIndex) {
        checkIndex(bitIndex);
        int w = wordIndex(bitIndex);
        expandTo(w);
        words[w] = words[w] | bitMask(bitIndex);
    }

    public void set(int bitIndex, boolean value) {
        if (value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }

    public void clear(int bitIndex) {
        checkIndex(bitIndex);
        int w = wordIndex(bitIndex);
        // Nothing to do past the end: those bits are already zero, and touching them would
        // grow the array to store zeros.
        if (w < wordsInUse) {
            words[w] = words[w] & ~bitMask(bitIndex);
            recalculateWordsInUse();
        }
    }

    // Toggle: XOR is the one operation that sets a clear bit and clears a set one.
    public void flip(int bitIndex) {
        checkIndex(bitIndex);
        int w = wordIndex(bitIndex);
        expandTo(w);
        words[w] = words[w] ^ bitMask(bitIndex);
        recalculateWordsInUse();
    }

    public boolean get(int bitIndex) {
        checkIndex(bitIndex);
        int w = wordIndex(bitIndex);
        boolean present = false;
        if (w < wordsInUse) {
            present = (words[w] & bitMask(bitIndex)) != 0L;
        }
        return present;
    }

    // --- ranges ---------------------------------------------------------------------
    //
    // A range [from, to) touches at most two partial words and any number of whole ones. The
    // masks below are the standard pair: `-1L << from` keeps the bits at or above `from`
    // (the shift is mod 64, so it already handles `from` being mid-word), and `-1L >>> -to`
    // keeps the bits below `to` — `>>> -to` being `>>> (64 - to)` for the same reason.

    private long firstWordMask(int fromIndex) {
        return -1L << fromIndex;
    }

    private long lastWordMask(int toIndex) {
        return -1L >>> -toIndex;
    }

    private void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0");
        }
        if (toIndex < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0");
        }
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex > toIndex");
        }
    }

    public void set(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex) {
            int startWord = wordIndex(fromIndex);
            int endWord = wordIndex(toIndex - 1);
            expandTo(endWord);
            long first = firstWordMask(fromIndex);
            long last = lastWordMask(toIndex);
            if (startWord == endWord) {
                // Entirely inside one word: the intersection of the two masks.
                words[startWord] = words[startWord] | (first & last);
            } else {
                words[startWord] = words[startWord] | first;
                for (int i = startWord + 1; i < endWord; i++) {
                    words[i] = -1L;
                }
                words[endWord] = words[endWord] | last;
            }
        }
    }

    public void set(int fromIndex, int toIndex, boolean value) {
        if (value) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }

    public void clear(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        int startWord = wordIndex(fromIndex);
        if (fromIndex != toIndex && startWord < wordsInUse) {
            int endWord = wordIndex(toIndex - 1);
            // Clamp to what actually exists — bits past the top word are already clear.
            if (endWord >= wordsInUse) {
                endWord = wordsInUse - 1;
            }
            long first = firstWordMask(fromIndex);
            long last = lastWordMask(toIndex);
            if (startWord == endWord) {
                words[startWord] = words[startWord] & ~(first & last);
            } else {
                words[startWord] = words[startWord] & ~first;
                for (int i = startWord + 1; i < endWord; i++) {
                    words[i] = 0L;
                }
                words[endWord] = words[endWord] & ~last;
            }
            recalculateWordsInUse();
        }
    }

    public void flip(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex) {
            int startWord = wordIndex(fromIndex);
            int endWord = wordIndex(toIndex - 1);
            expandTo(endWord);
            long first = firstWordMask(fromIndex);
            long last = lastWordMask(toIndex);
            if (startWord == endWord) {
                words[startWord] = words[startWord] ^ (first & last);
            } else {
                words[startWord] = words[startWord] ^ first;
                for (int i = startWord + 1; i < endWord; i++) {
                    words[i] = ~words[i];
                }
                words[endWord] = words[endWord] ^ last;
            }
            recalculateWordsInUse();
        }
    }

    public void clear() {
        for (int i = 0; i < wordsInUse; i++) {
            words[i] = 0L;
        }
        wordsInUse = 0;
    }

    // --- bit-twiddling helpers ------------------------------------------------------
    //
    // KajiLibrary's `Long` has no bitCount/numberOfTrailingZeros, so these build the 64-bit
    // answers out of the 32-bit intrinsics on `Integer`. Splitting a long into halves is the
    // usual way these are implemented anyway.

    private static int bitCount(long word) {
        int lo = Integer.bitCount((int) word);
        int hi = Integer.bitCount((int) (word >>> 32));
        return lo + hi;
    }

    // Position of the lowest set bit of a non-zero int. `x & -x` clears every bit but the
    // lowest set one (two's complement: `-x` is `~x + 1`, which flips everything above that
    // bit and leaves it alone), and the number of leading zeros of a lone bit at position k
    // is 31 - k — so the subtraction reads k straight back out.
    private static int trailingZeros32(int x) {
        return 31 - Integer.numberOfLeadingZeros(x & -x);
    }

    private static int trailingZeros(long word) {
        int lo = (int) word;
        int n;
        if (lo != 0) {
            n = trailingZeros32(lo);
        } else {
            n = 32 + trailingZeros32((int) (word >>> 32));
        }
        return n;
    }

    private static int leadingZeros(long word) {
        int hi = (int) (word >>> 32);
        int n;
        if (hi != 0) {
            n = Integer.numberOfLeadingZeros(hi);
        } else {
            n = 32 + Integer.numberOfLeadingZeros((int) word);
        }
        return n;
    }

    // --- scanning -------------------------------------------------------------------

    // The smallest set bit at or after `fromIndex`, or -1. This is the method that makes a
    // BitSet iterable at a reasonable cost: it skips 64 clear bits per word compare instead
    // of testing them one at a time, which is the whole point of the word layout.
    public int nextSetBit(int fromIndex) {
        checkIndex(fromIndex);
        int u = wordIndex(fromIndex);
        int found = -1;
        if (u < wordsInUse) {
            // Mask off the bits below fromIndex in the first word; whole words after that.
            long word = words[u] & firstWordMask(fromIndex);
            while (found < 0) {
                if (word != 0L) {
                    found = u * 64 + trailingZeros(word);
                } else {
                    u = u + 1;
                    if (u >= wordsInUse) {
                        found = -2;              // ran off the end: report absence below
                    } else {
                        word = words[u];
                    }
                }
            }
            if (found == -2) {
                found = -1;
            }
        }
        return found;
    }

    // The mirror of nextSetBit over the complement. Note it never returns -1: past the last
    // word every bit is clear, so there is always an answer.
    public int nextClearBit(int fromIndex) {
        checkIndex(fromIndex);
        int u = wordIndex(fromIndex);
        int found = -1;
        if (u >= wordsInUse) {
            found = fromIndex;
        } else {
            long word = ~words[u] & firstWordMask(fromIndex);
            while (found < 0) {
                if (word != 0L) {
                    found = u * 64 + trailingZeros(word);
                } else {
                    u = u + 1;
                    if (u >= wordsInUse) {
                        found = u * 64;
                    } else {
                        word = ~words[u];
                    }
                }
            }
        }
        return found;
    }

    // The largest set bit at or before `fromIndex`, or -1. `-1` is accepted as an argument
    // (and answers -1) so a backwards loop can end on it, matching the JDK.
    public int previousSetBit(int fromIndex) {
        int found = -1;
        if (fromIndex < 0) {
            if (fromIndex != -1) {
                throw new IndexOutOfBoundsException("fromIndex < -1");
            }
        } else {
            int u = wordIndex(fromIndex);
            if (u >= wordsInUse) {
                found = length() - 1;
            } else {
                long word = words[u] & (-1L >>> -(fromIndex + 1));
                while (found < 0 && u >= 0) {
                    if (word != 0L) {
                        // 63 - leadingZeros is the position of the *highest* set bit.
                        found = u * 64 + 63 - leadingZeros(word);
                    } else {
                        u = u - 1;
                        if (u >= 0) {
                            word = words[u];
                        }
                    }
                }
            }
        }
        return found;
    }

    // --- whole-set queries ----------------------------------------------------------

    // One past the highest set bit — the *logical* size, independent of how many words are
    // allocated. Zero for an empty set.
    public int length() {
        int n = 0;
        if (wordsInUse != 0) {
            n = 64 * (wordsInUse - 1) + (64 - leadingZeros(words[wordsInUse - 1]));
        }
        return n;
    }

    // The *allocated* size in bits. Deliberately different from length(): this one is an
    // implementation detail (how much room there is), that one is the content.
    public int size() {
        return words.length * 64;
    }

    public boolean isEmpty() {
        return wordsInUse == 0;
    }

    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < wordsInUse; i++) {
            sum = sum + bitCount(words[i]);
        }
        return sum;
    }

    // Do the two sets share an element? Cheaper than `and`-ing a copy and testing it, and it
    // can stop at the first overlapping word.
    public boolean intersects(BitSet set) {
        boolean any = false;
        int limit = wordsInUse;
        if (set.wordsInUse < limit) {
            limit = set.wordsInUse;
        }
        for (int i = 0; i < limit; i++) {
            if ((words[i] & set.words[i]) != 0L) {
                any = true;
            }
        }
        return any;
    }

    // --- set algebra ----------------------------------------------------------------
    //
    // Sixty-four elements per instruction. Everything above `wordsInUse` on either side is
    // implicitly zero, which is what makes the loop bounds asymmetric between the four.

    // Intersection. Words past the other set's length become zero, so they are truncated.
    public void and(BitSet set) {
        if (this != set) {
            while (wordsInUse > set.wordsInUse) {
                words[wordsInUse - 1] = 0L;
                wordsInUse = wordsInUse - 1;
            }
            for (int i = 0; i < wordsInUse; i++) {
                words[i] = words[i] & set.words[i];
            }
            recalculateWordsInUse();
        }
    }

    // Union. Grows to the longer of the two.
    public void or(BitSet set) {
        if (this != set) {
            int common = wordsInUse;
            if (set.wordsInUse < common) {
                common = set.wordsInUse;
            }
            if (wordsInUse < set.wordsInUse) {
                expandTo(set.wordsInUse - 1);
            }
            for (int i = 0; i < common; i++) {
                words[i] = words[i] | set.words[i];
            }
            for (int i = common; i < set.wordsInUse; i++) {
                words[i] = set.words[i];
            }
            recalculateWordsInUse();
        }
    }

    // Symmetric difference — the elements in exactly one of the two sets. XOR-ing a set with
    // itself is the identity of the family: it empties the set.
    public void xor(BitSet set) {
        int common = wordsInUse;
        if (set.wordsInUse < common) {
            common = set.wordsInUse;
        }
        if (wordsInUse < set.wordsInUse) {
            expandTo(set.wordsInUse - 1);
        }
        for (int i = 0; i < common; i++) {
            words[i] = words[i] ^ set.words[i];
        }
        for (int i = common; i < set.wordsInUse; i++) {
            words[i] = set.words[i];
        }
        recalculateWordsInUse();
    }

    // Difference: keep what is here and not there. Never grows — removing elements cannot
    // add any.
    public void andNot(BitSet set) {
        int limit = wordsInUse;
        if (set.wordsInUse < limit) {
            limit = set.wordsInUse;
        }
        for (int i = 0; i < limit; i++) {
            words[i] = words[i] & ~set.words[i];
        }
        recalculateWordsInUse();
    }

    // --- conversion -----------------------------------------------------------------

    // A copy of the words in use, little-endian by bit index: bit `i` is bit `i % 64` of
    // element `i / 64`. A copy, not the array itself — handing out `words` would let a caller
    // break the wordsInUse invariant from outside.
    public long[] toLongArray() {
        long[] copy = new long[wordsInUse];
        for (int i = 0; i < wordsInUse; i++) {
            copy[i] = words[i];
        }
        return copy;
    }

    /**
     * Los bits como bytes, **little-endian**: el bit 0 es el bit menos significativo del byte 0.
     *
     * <p>El orden importa y es facil de invertir. Es el mismo que usa `toLongArray`, un nivel mas
     * abajo, y el que hace que `BitSet.valueOf(x.toByteArray())` devuelva un conjunto igual a `x`.
     */
    public byte[] toByteArray() {
        int n = wordsInUse;
        if (n == 0) {
            return new byte[0];
        }
        // El ultimo word puede aportar menos de 8 bytes: solo los que llegan hasta el bit mas alto.
        int len = 8 * (n - 1) + (64 - leadingZeros(words[n - 1]) + 7) / 8;
        byte[] out = new byte[len];
        int i = 0;
        int b = 0;
        while (i < n - 1) {
            int k = 0;
            while (k < 8) {
                out[b] = (byte) ((words[i] >>> (8 * k)) & 0xffL);
                b = b + 1;
                k = k + 1;
            }
            i = i + 1;
        }
        long ultimo = words[n - 1];
        while (b < len) {
            out[b] = (byte) ((ultimo >>> (8 * (b % 8))) & 0xffL);
            b = b + 1;
        }
        return out;
    }

    /**
     * El indice del ultimo bit **en cero** en `[0, fromIndex]`, o -1.
     *
     * <p>Nunca devuelve -1 para un `fromIndex` valido, a diferencia de `previousSetBit`: mas alla de
     * `length()` todo esta en cero, asi que siempre hay un cero que encontrar. El -1 solo aparece si
     * los bits 0..fromIndex estan todos en uno.
     *
     * @throws IndexOutOfBoundsException si `fromIndex` es menor que -1
     */
    public int previousClearBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex != -1) {
                throw new IndexOutOfBoundsException("fromIndex < -1");
            }
            return -1;
        }
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return fromIndex;   // pasado el final no hay nada seteado
        }
        // Se busca sobre el complemento: un cero de `words` es un uno de `~words`.
        long word = ~words[u] & (-1L >>> -(fromIndex + 1));
        int found = -1;
        while (found < 0 && u >= 0) {
            if (word != 0L) {
                found = u * 64 + 63 - leadingZeros(word);
            } else {
                u = u - 1;
                if (u >= 0) {
                    word = ~words[u];
                }
            }
        }
        return found;
    }

    /**
     * Un `BitSet` nuevo con los bits de `[fromIndex, toIndex)`, **recorridos a la posicion 0**.
     *
     * <p>El desplazamiento es lo que lo distingue de un `and` con una mascara: `get(64, 66)` de un
     * conjunto con el bit 64 puesto devuelve un conjunto con el bit **0** puesto, no el 64.
     *
     * @throws IndexOutOfBoundsException si algun indice es negativo, o `fromIndex > toIndex`
     */
    public BitSet get(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        BitSet out = new BitSet();
        int len = length();
        if (fromIndex >= len || fromIndex == toIndex) {
            return out;
        }
        int hasta = toIndex;
        if (hasta > len) {
            hasta = len;
        }
        int i = fromIndex;
        while (i < hasta) {
            if (get(i)) {
                out.set(i - fromIndex);
            }
            i = i + 1;
        }
        return out;
    }

    /** Los indices de los bits en uno, en orden creciente. */
    public java.util.stream.IntStream stream() {
        int n = cardinality();
        int[] idx = new int[n];
        int k = 0;
        int i = nextSetBit(0);
        while (i >= 0) {
            idx[k] = i;
            k = k + 1;
            i = nextSetBit(i + 1);
        }
        return java.util.stream.IntStream.of(idx);
    }

    /**
     * Un `BitSet` con los bits de `bytes`, en el mismo orden little-endian que `toByteArray`.
     */
    public static BitSet valueOf(byte[] bytes) {
        int n = bytes.length;
        while (n > 0 && bytes[n - 1] == 0) {
            n = n - 1;
        }
        long[] words = new long[(n + 7) / 8];
        int i = 0;
        while (i < n) {
            words[i / 8] = words[i / 8] | ((bytes[i] & 0xffL) << (8 * (i % 8)));
            i = i + 1;
        }
        return BitSet.valueOf(words);
    }

    /**
     * Idem, leyendo desde la **posicion actual** del buffer hasta su limite.
     *
     * <p>El buffer no se toca: ni su posicion ni su limite cambian. Es lo que promete el JDK, y lo
     * que hace que se lo pueda seguir usando despues de esta llamada.
     */
    public static BitSet valueOf(java.nio.ByteBuffer bb) {
        int n = bb.remaining();
        byte[] copia = new byte[n];
        int i = 0;
        while (i < n) {
            copia[i] = bb.get(bb.position() + i);
            i = i + 1;
        }
        return BitSet.valueOf(copia);
    }

    /** Idem, desde un buffer de `long`. */
    public static BitSet valueOf(java.nio.LongBuffer lb) {
        int n = lb.remaining();
        long[] copia = new long[n];
        int i = 0;
        while (i < n) {
            copia[i] = lb.get(lb.position() + i);
            i = i + 1;
        }
        return BitSet.valueOf(copia);
    }

    public static BitSet valueOf(long[] longs) {
        int n = longs.length;
        while (n > 0 && longs[n - 1] == 0L) {
            n = n - 1;
        }
        BitSet set = new BitSet();
        set.words = new long[n + 1];
        for (int i = 0; i < n; i++) {
            set.words[i] = longs[i];
        }
        set.wordsInUse = n;
        return set;
    }

    // --- equality -------------------------------------------------------------------

    // Two BitSets are equal when the same bits are set — which, given the wordsInUse
    // invariant, is exactly word-for-word equality over the words in use. Allocated capacity
    // deliberately does not count.
    public boolean equals(Object obj) {
        boolean same;
        if (obj == this) {
            same = true;
        } else if (!(obj instanceof BitSet)) {
            same = false;
        } else {
            BitSet other = (BitSet) obj;
            if (wordsInUse != other.wordsInUse) {
                same = false;
            } else {
                same = true;
                for (int i = 0; i < wordsInUse; i++) {
                    if (words[i] != other.words[i]) {
                        same = false;
                    }
                }
            }
        }
        return same;
    }

    // The JDK's exact mix, kept because the constant and the index weighting are what stop
    // two sets with the same words in a different order from colliding. The final fold XORs
    // the high half into the low one so all 64 bits reach the returned int.
    public int hashCode() {
        long h = 1234L;
        for (int i = wordsInUse - 1; i >= 0; i--) {
            // The `(long)` is explicit: our javac does not widen int to long implicitly
            // (finding #103), and `i + 1` is an int.
            h = h ^ words[i] * (long) (i + 1);
        }
        return (int) ((h >> 32) ^ h);
    }

    // "{}" when empty, "{1, 4, 64}" otherwise — the set's *elements*, not its words, because
    // the words are the representation and the elements are the content.
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('{');
        int i = nextSetBit(0);
        boolean first = true;
        while (i >= 0) {
            if (!first) {
                b.append(',');
                b.append(' ');
            }
            first = false;
            b.append(Integer.toString(i));
            i = nextSetBit(i + 1);
        }
        b.append('}');
        return b.toString();
    }
}
