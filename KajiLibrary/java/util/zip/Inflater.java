package java.util.zip;

// DEFLATE decompression (RFC 1951) and its zlib wrapper (RFC 1950), in pure Java.
//
// Two things about the format are easy to get backwards, and both fail silently rather than
// loudly:
//
//   - The stream is read LSB-FIRST, bit by bit from the low end of each byte, but a HUFFMAN CODE
//     is packed MSB-first, most significant bit of the code arriving first. The two orders
//     coexist in the same stream, and `bits()` versus `huffman()` below are exactly that split.
//   - A back-reference may OVERLAP the bytes it is still producing (a distance smaller than the
//     length). Copying it as a block would be wrong: it has to go byte by byte, and that is what
//     lets a run of a thousand identical bytes cost three.
//
// Resumability, which is what makes this usable from a stream: input arrives in pieces, so a
// symbol can run out of bits half-decoded. Rather than suspend the decoder mid-symbol, the bit
// cursor is CHECKPOINTED after every completed symbol; on underflow it rewinds there and asks
// for more input, and the partial symbol is simply decoded again later. Input is accumulated, so
// re-decoding is always possible.
//
// The decoded output is kept whole rather than in a 32 KB circular window: back-references index
// straight into it. That costs memory proportional to the output — the honest trade for code
// that stays readable — and it is why `end()` drops the buffers.
public class Inflater implements AutoCloseable {

    private final boolean nowrap;

    private byte[] in;
    private int inLen;
    // Bit cursor into `in`, and the position to rewind to when a symbol runs out of bits.
    private int bitPos;
    private int checkpoint;

    private byte[] out;
    private int outLen;
    private int handed;

    private boolean finished;
    private boolean headerDone;
    private boolean underflow;
    // Se quedo sin bits a mitad de un simbolo. Distinto de "no queda input": pueden sobrar bytes
    // y aun asi no alcanzar para el simbolo siguiente.
    private boolean stalled;

    // Set while a block is being decoded, so a resumed call does not re-read its header.
    private boolean inBlock;
    private boolean lastBlock;
    private int storedLeft;
    private int[] litCounts;
    private int[] litSymbols;
    private int[] distCounts;
    private int[] distSymbols;

    private long bytesRead;
    private long bytesWritten;

    // Extra bits and base values for the length and distance codes (RFC 1951 3.2.5). They are
    // tables and not formulas because the ranges are deliberately irregular at both ends.
    private static final int[] LEN_BASE = {
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258
    };
    private static final int[] LEN_EXTRA = {
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
    };
    private static final int[] DIST_BASE = {
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
    };
    private static final int[] DIST_EXTRA = {
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
    };
    // The order the code-length code lengths appear in, for a dynamic block. Front-loaded with
    // the lengths most likely to be non-zero, so trailing zeros can be omitted.
    private static final int[] CLEN_ORDER = {
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
    };

    public Inflater(boolean nowrap) {
        this.nowrap = nowrap;
        this.in = new byte[0];
        this.out = new byte[0];
        reset();
    }

    public Inflater() {
        this(false);
    }

    public void setInput(byte[] b, int off, int len) {
        // Input accumulates: a rewound checkpoint has to be able to re-read what it already saw.
        int keep = inLen - (bitPos >> 3);
        byte[] grown = new byte[keep + len];
        int i = 0;
        int from = bitPos >> 3;
        while (i < keep) {
            grown[i] = in[from + i];
            i = i + 1;
        }
        int k = 0;
        while (k < len) {
            grown[keep + k] = b[off + k];
            k = k + 1;
        }
        bitPos = bitPos & 7;
        checkpoint = bitPos;
        in = grown;
        inLen = keep + len;
        stalled = false;
        bytesRead = bytesRead + (long) len;
    }

    public void setInput(byte[] b) {
        setInput(b, 0, b.length);
    }

    // Preset dictionaries are not supported: `needsDictionary()` is always false, so a caller
    // following the JDK's contract never reaches this. Kept for surface compatibility.
    public void setDictionary(byte[] b, int off, int len) {
    }

    public void setDictionary(byte[] b) {
    }

    public int getRemaining() {
        return inLen - (bitPos >> 3);
    }

    public boolean needsInput() {
        // El `stalled` es lo que evita que el consumidor gire en falso: sin el, un decodificador
        // frenado por medio simbolo reporta que todavia tiene input, el llamador no alimenta mas
        // y `inflate` devuelve 0 para siempre. Lo encontro el self-test colgandose.
        return getRemaining() == 0 || stalled;
    }

    public boolean needsDictionary() {
        return false;
    }

    public boolean finished() {
        return finished && handed == outLen;
    }

    public int inflate(byte[] b, int off, int len) throws DataFormatException {
        // Decode until there is enough to satisfy the request, or the input runs dry.
        while (!finished && outLen - handed < len && !underflowed()) {
            step();
        }
        int available = outLen - handed;
        int n = len;
        if (available < n) {
            n = available;
        }
        int i = 0;
        while (i < n) {
            b[off + i] = out[handed + i];
            i = i + 1;
        }
        handed = handed + n;
        bytesWritten = bytesWritten + (long) n;
        return n;
    }

    public int inflate(byte[] b) throws DataFormatException {
        return inflate(b, 0, b.length);
    }

    private boolean underflowed() {
        boolean hit = underflow;
        underflow = false;
        stalled = hit;
        return hit;
    }

    // One unit of work: a header, a block header, or a single symbol.
    private void step() throws DataFormatException {
        if (!headerDone) {
            readHeader();
            return;
        }
        if (!inBlock) {
            readBlockHeader();
            return;
        }
        if (litCounts == null) {
            copyStored();
        } else {
            decodeSymbol();
        }
    }

    private void readHeader() throws DataFormatException {
        if (nowrap) {
            headerDone = true;
            return;
        }
        // zlib's two-byte header (RFC 1950): compression method and flags.
        int cmf = bits(8);
        int flg = bits(8);
        if (underflow) {
            rewind();
            return;
        }
        if ((cmf & 0x0f) != 8) {
            throw new DataFormatException("unsupported compression method");
        }
        if (((cmf << 8) + flg) % 31 != 0) {
            throw new DataFormatException("bad zlib header checksum");
        }
        headerDone = true;
        commit();
    }

    private void readBlockHeader() throws DataFormatException {
        int last = bits(1);
        int type = bits(2);
        if (underflow) {
            rewind();
            return;
        }
        lastBlock = last == 1;
        if (type == 0) {
            // Stored: skip to a byte boundary, then a length and its complement.
            bitPos = (bitPos + 7) & ~7;
            int lo = bits(8);
            int hi = bits(8);
            bits(8);
            bits(8);
            if (underflow) {
                rewind();
                return;
            }
            storedLeft = lo | (hi << 8);
            litCounts = null;
        } else if (type == 1) {
            buildFixedTables();
        } else if (type == 2) {
            readDynamicTables();
            if (underflow) {
                rewind();
                return;
            }
        } else {
            throw new DataFormatException("invalid block type");
        }
        inBlock = true;
        commit();
    }

    private void copyStored() {
        if (storedLeft == 0) {
            endBlock();
            return;
        }
        int b = bits(8);
        if (underflow) {
            rewind();
            return;
        }
        emit((byte) b);
        storedLeft = storedLeft - 1;
        commit();
    }

    private void decodeSymbol() throws DataFormatException {
        int sym = huffman(litCounts, litSymbols);
        if (underflow) {
            rewind();
            return;
        }
        if (sym < 256) {
            emit((byte) sym);
            commit();
        } else if (sym == 256) {
            endBlock();
            commit();
        } else {
            int idx = sym - 257;
            if (idx >= LEN_BASE.length) {
                throw new DataFormatException("invalid length code");
            }
            int length = LEN_BASE[idx] + bits(LEN_EXTRA[idx]);
            int distSym = huffman(distCounts, distSymbols);
            if (underflow) {
                rewind();
                return;
            }
            if (distSym >= DIST_BASE.length) {
                throw new DataFormatException("invalid distance code");
            }
            int distance = DIST_BASE[distSym] + bits(DIST_EXTRA[distSym]);
            if (underflow) {
                rewind();
                return;
            }
            if (distance > outLen) {
                throw new DataFormatException("distance beyond output");
            }
            // Byte by byte on purpose: the source may still be being written. See the note above.
            int from = outLen - distance;
            int copied = 0;
            while (copied < length) {
                emit(out[from + copied]);
                copied = copied + 1;
            }
            commit();
        }
    }

    private void endBlock() {
        inBlock = false;
        litCounts = null;
        distCounts = null;
        if (lastBlock) {
            finished = true;
        }
    }

    // ---- bit reading ----

    // `count` bits, LSB-first — the order DEFLATE packs its fields in.
    private int bits(int count) {
        int value = 0;
        int got = 0;
        while (got < count) {
            int byteIndex = bitPos >> 3;
            if (byteIndex >= inLen) {
                underflow = true;
                return 0;
            }
            int bit = (in[byteIndex] >> (bitPos & 7)) & 1;
            value = value | (bit << got);
            bitPos = bitPos + 1;
            got = got + 1;
        }
        return value;
    }

    // A canonical Huffman symbol, MSB-first. Walks code lengths from short to long, checking at
    // each length whether the code accumulated so far falls inside that length's range — which
    // is what makes the encoding self-delimiting without any length prefix.
    private int huffman(int[] counts, int[] symbols) {
        int code = 0;
        int first = 0;
        int index = 0;
        int len = 1;
        while (len <= 15) {
            code = code | bits(1);
            if (underflow) {
                return 0;
            }
            int count = counts[len];
            if (code - first < count) {
                return symbols[index + (code - first)];
            }
            index = index + count;
            first = (first + count) << 1;
            code = code << 1;
            len = len + 1;
        }
        return -1;
    }

    // ---- tables ----

    // Canonical decoding tables: how many codes of each length, and the symbols in code order.
    private static HuffTable buildTable(int[] lengths, int n) {
        int[] counts = new int[16];
        int i = 0;
        while (i < n) {
            counts[lengths[i]] = counts[lengths[i]] + 1;
            i = i + 1;
        }
        counts[0] = 0;
        int[] offsets = new int[16];
        int len = 1;
        while (len < 16) {
            offsets[len] = offsets[len - 1] + counts[len - 1];
            len = len + 1;
        }
        int[] symbols = new int[n];
        int s = 0;
        while (s < n) {
            if (lengths[s] != 0) {
                symbols[offsets[lengths[s]]] = s;
                offsets[lengths[s]] = offsets[lengths[s]] + 1;
            }
            s = s + 1;
        }
        // Devuelve un objeto y no un `int[][]`: el emisor todavia no soporta la creacion de un
        // array escalonado (`new int[2][]`), que es como estaba escrito primero.
        return new HuffTable(counts, symbols);
    }

    // The fixed tables of RFC 1951 3.2.6 — a fallback encoding every decoder knows by heart, so
    // a small block need not pay for transmitting its own.
    private void buildFixedTables() {
        int[] lit = new int[288];
        int i = 0;
        while (i < 144) { lit[i] = 8; i = i + 1; }
        while (i < 256) { lit[i] = 9; i = i + 1; }
        while (i < 280) { lit[i] = 7; i = i + 1; }
        while (i < 288) { lit[i] = 8; i = i + 1; }
        HuffTable litTable = buildTable(lit, 288);
        litCounts = litTable.counts;
        litSymbols = litTable.symbols;

        int[] dist = new int[30];
        int k = 0;
        while (k < 30) { dist[k] = 5; k = k + 1; }
        HuffTable distTable = buildTable(dist, 30);
        distCounts = distTable.counts;
        distSymbols = distTable.symbols;
    }

    // The dynamic case: the block carries its own tables, themselves Huffman-coded. That third
    // level of indirection is the price of adapting the code lengths to the actual data.
    private void readDynamicTables() throws DataFormatException {
        int hlit = bits(5) + 257;
        int hdist = bits(5) + 1;
        int hclen = bits(4) + 4;
        if (underflow) {
            return;
        }
        int[] clen = new int[19];
        int i = 0;
        while (i < hclen) {
            clen[CLEN_ORDER[i]] = bits(3);
            i = i + 1;
        }
        if (underflow) {
            return;
        }
        HuffTable clenTable = buildTable(clen, 19);

        int[] lengths = new int[hlit + hdist];
        int n = 0;
        while (n < hlit + hdist) {
            int sym = huffman(clenTable.counts, clenTable.symbols);
            if (underflow) {
                return;
            }
            if (sym < 16) {
                lengths[n] = sym;
                n = n + 1;
            } else if (sym == 16) {
                // Repeat the previous length 3-6 times.
                if (n == 0) {
                    throw new DataFormatException("no length to repeat");
                }
                int prev = lengths[n - 1];
                int repeat = 3 + bits(2);
                if (underflow) {
                    return;
                }
                int r = 0;
                while (r < repeat && n < hlit + hdist) {
                    lengths[n] = prev;
                    n = n + 1;
                    r = r + 1;
                }
            } else if (sym == 17) {
                int repeat = 3 + bits(3);
                if (underflow) {
                    return;
                }
                n = n + repeat;
            } else {
                int repeat = 11 + bits(7);
                if (underflow) {
                    return;
                }
                n = n + repeat;
            }
        }
        int[] lit = new int[hlit];
        int a = 0;
        while (a < hlit) { lit[a] = lengths[a]; a = a + 1; }
        int[] dist = new int[hdist];
        int d = 0;
        while (d < hdist) { dist[d] = lengths[hlit + d]; d = d + 1; }
        HuffTable litTable = buildTable(lit, hlit);
        litCounts = litTable.counts;
        litSymbols = litTable.symbols;
        HuffTable distTable = buildTable(dist, hdist);
        distCounts = distTable.counts;
        distSymbols = distTable.symbols;
    }

    // ---- output and checkpointing ----

    private void emit(byte b) {
        if (outLen == out.length) {
            int size = out.length * 2;
            if (size < 1024) {
                size = 1024;
            }
            byte[] grown = new byte[size];
            int i = 0;
            while (i < outLen) {
                grown[i] = out[i];
                i = i + 1;
            }
            out = grown;
        }
        out[outLen] = b;
        outLen = outLen + 1;
    }

    private void commit() {
        checkpoint = bitPos;
    }

    private void rewind() {
        bitPos = checkpoint;
    }

    public int getAdler() {
        return 1;
    }

    public int getTotalIn() {
        return (int) bytesRead;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public int getTotalOut() {
        return (int) bytesWritten;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public void reset() {
        in = new byte[0];
        inLen = 0;
        bitPos = 0;
        checkpoint = 0;
        out = new byte[0];
        outLen = 0;
        handed = 0;
        finished = false;
        headerDone = false;
        underflow = false;
        stalled = false;
        inBlock = false;
        litCounts = null;
        distCounts = null;
        bytesRead = 0;
        bytesWritten = 0;
    }

    public void end() {
        in = new byte[0];
        out = new byte[0];
        inLen = 0;
        outLen = 0;
    }

    public void close() {
        end();
    }
}

// Un par (counts, symbols) de decodificacion canonica. Clase top-level package-private en el
// mismo archivo, el idioma del proyecto para un tipo auxiliar; el gate la saltea porque el JDK
// no tiene contraparte.
class HuffTable {

    final int[] counts;
    final int[] symbols;

    HuffTable(int[] counts, int[] symbols) {
        this.counts = counts;
        this.symbols = symbols;
    }
}
