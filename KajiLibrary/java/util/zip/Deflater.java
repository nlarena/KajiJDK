package java.util.zip;

// DEFLATE compression (RFC 1951) with the zlib wrapper (RFC 1950), in pure Java.
//
// WHAT THIS DOES AND DOES NOT DO, up front so nobody is surprised by a file that grew: it emits
// only STORED blocks — the block type that carries its payload verbatim. That is valid DEFLATE
// and any inflater in the world reads it, but it does not compress: the output is the input plus
// five bytes of block header per 65535-byte chunk, plus the wrapper.
//
// The reason is that the two halves of DEFLATE are not the same size of problem. Decoding is
// mechanical — the format tells you what to do. ENCODING is where the algorithm lives: a match
// finder (a hash chain over the last 32 KB), the choice between a literal and a back-reference,
// and building Huffman tables that beat the fixed ones often enough to pay for transmitting
// them. Shipping a correct stored-block encoder first means `DeflaterOutputStream`,
// `GZIPOutputStream` and the zip writer all work end to end, and the match finder can be dropped
// in behind this same surface later without any caller noticing.
//
// The level and strategy setters are therefore accepted and remembered, but they do not yet
// change the output.
public class Deflater implements AutoCloseable {

    public static final int DEFLATED = 8;
    public static final int NO_COMPRESSION = 0;
    public static final int BEST_SPEED = 1;
    public static final int BEST_COMPRESSION = 9;
    public static final int DEFAULT_COMPRESSION = -1;
    public static final int FILTERED = 1;
    public static final int HUFFMAN_ONLY = 2;
    public static final int DEFAULT_STRATEGY = 0;
    public static final int NO_FLUSH = 0;
    public static final int SYNC_FLUSH = 2;
    public static final int FULL_FLUSH = 3;

    // The most a single stored block can carry: its length is a 16-bit field.
    private static final int MAX_STORED = 65535;

    private final boolean nowrap;
    private int level;
    private int strategy;

    private byte[] pending;
    private int pendingLen;
    private int pendingOff;

    private byte[] out;
    private int outLen;
    private int outOff;

    private boolean finishing;
    private boolean finished;
    private boolean headerWritten;

    private final Adler32 adler;
    private long bytesRead;
    private long bytesWritten;

    public Deflater(int level, boolean nowrap) {
        this.level = level;
        this.nowrap = nowrap;
        this.strategy = DEFAULT_STRATEGY;
        this.pending = new byte[0];
        this.out = new byte[0];
        this.adler = new Adler32();
    }

    public Deflater(int level) {
        this(level, false);
    }

    public Deflater() {
        this(DEFAULT_COMPRESSION, false);
    }

    /**
     * Toma como entrada los bytes que quedan en `input`, y lo deja consumido.
     *
     * <p>**Se copia**, y eso es una diferencia con el JDK que conviene decir: alli un buffer directo
     * se pasa al deflate nativo sin copiar, y por eso el JDK exige no tocarlo hasta que
     * `needsInput()` vuelva a dar `true`. Aca la copia hace que esa exigencia no aplique -- el
     * codigo que la respeta funciona igual, y el que no la respetaba tambien.
     */
    public void setInput(java.nio.ByteBuffer input) {
        int n = input.remaining();
        byte[] tmp = new byte[n];
        if (n > 0) {
            input.get(tmp, 0, n);
        }
        this.setInput(tmp, 0, n);
    }

    /** El diccionario de precarga, desde los bytes que quedan en `dictionary`. */
    public void setDictionary(java.nio.ByteBuffer dictionary) {
        int n = dictionary.remaining();
        byte[] tmp = new byte[n];
        if (n > 0) {
            dictionary.get(tmp, 0, n);
        }
        this.setDictionary(tmp, 0, n);
    }

    /** Comprime en el espacio que queda en `output`, avanzando su posicion por lo escrito. */
    public int deflate(java.nio.ByteBuffer output) {
        return this.deflate(output, NO_FLUSH);
    }

    /** El de arriba con modo de vaciado explicito. */
    public int deflate(java.nio.ByteBuffer output, int flush) {
        int espacio = output.remaining();
        if (espacio <= 0) {
            return 0;
        }
        byte[] tmp = new byte[espacio];
        int n = this.deflate(tmp, 0, espacio, flush);
        if (n > 0) {
            output.put(tmp, 0, n);
        }
        return n;
    }

    public void setInput(byte[] b, int off, int len) {
        byte[] grown = new byte[pendingLen - pendingOff + len];
        int i = 0;
        while (pendingOff + i < pendingLen) {
            grown[i] = pending[pendingOff + i];
            i = i + 1;
        }
        int k = 0;
        while (k < len) {
            grown[i + k] = b[off + k];
            k = k + 1;
        }
        pending = grown;
        pendingLen = grown.length;
        pendingOff = 0;
        // The zlib trailer is an Adler-32 of the UNCOMPRESSED data, so it is accumulated here,
        // as the bytes arrive, rather than anywhere near the block writer.
        adler.update(b, off, len);
        bytesRead = bytesRead + (long) len;
    }

    public void setInput(byte[] b) {
        setInput(b, 0, b.length);
    }

    // Preset dictionaries would change what a back-reference may point at, and with no back
    // references there is nothing for them to change.
    public void setDictionary(byte[] b, int off, int len) {
    }

    public void setDictionary(byte[] b) {
    }

    public void setStrategy(int strategy) {
        this.strategy = strategy;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean needsInput() {
        return pendingLen - pendingOff == 0;
    }

    public void finish() {
        finishing = true;
    }

    public boolean finished() {
        return finished && outLen - outOff == 0;
    }

    public int deflate(byte[] b, int off, int len) {
        while (outLen - outOff < len && !finished && canProduce()) {
            produce();
        }
        int available = outLen - outOff;
        int n = len;
        if (available < n) {
            n = available;
        }
        int i = 0;
        while (i < n) {
            b[off + i] = out[outOff + i];
            i = i + 1;
        }
        outOff = outOff + n;
        bytesWritten = bytesWritten + (long) n;
        return n;
    }

    public int deflate(byte[] b) {
        return deflate(b, 0, b.length);
    }

    // The flush modes collapse into one behaviour here: a stored block is already byte-aligned
    // and self-contained, so every block boundary is a valid flush point.
    public int deflate(byte[] b, int off, int len, int flush) {
        return deflate(b, off, len);
    }

    // Whether another block can be produced right now. Without this guard the produce loop would
    // spin on an empty deflater that has not been asked to finish — and marking it `finished` to
    // break out would be a lie: more input may still arrive.
    private boolean canProduce() {
        return !headerWritten || pendingLen - pendingOff > 0 || finishing;
    }

    private void produce() {
        if (!headerWritten) {
            writeHeader();
            return;
        }
        int available = pendingLen - pendingOff;
        int size = available;
        if (size > MAX_STORED) {
            size = MAX_STORED;
        }
        boolean last = finishing && size == available;
        writeStoredBlock(size, last);
        if (last) {
            writeTrailer();
            finished = true;
        }
    }

    private void writeHeader() {
        headerWritten = true;
        if (!nowrap) {
            // zlib header: method 8 with a 32 KB window, no preset dictionary, and a check byte
            // chosen so the two bytes together are a multiple of 31.
            emit((byte) 0x78);
            emit((byte) 0x01);
        }
    }

    private void writeStoredBlock(int size, boolean last) {
        int header = 0;
        if (last) {
            header = 1;
        }
        // Block type 00 = stored. The type bits ride in the same byte as the final-block flag,
        // and the payload starts at the NEXT byte boundary — which is what makes a stored block
        // copyable without touching the bit reader.
        emit((byte) header);
        emit((byte) (size & 0xff));
        emit((byte) ((size >> 8) & 0xff));
        int complement = size ^ 0xffff;
        emit((byte) (complement & 0xff));
        emit((byte) ((complement >> 8) & 0xff));
        int i = 0;
        while (i < size) {
            emit(pending[pendingOff + i]);
            i = i + 1;
        }
        pendingOff = pendingOff + size;
    }

    private void writeTrailer() {
        if (!nowrap) {
            // Adler-32, big-endian — the one big-endian field in a format that is otherwise
            // little-endian, because RFC 1950 predates the convention RFC 1951 settled on.
            long sum = adler.getValue();
            emit((byte) ((sum >> 24) & 0xff));
            emit((byte) ((sum >> 16) & 0xff));
            emit((byte) ((sum >> 8) & 0xff));
            emit((byte) (sum & 0xff));
        }
    }

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

    public int getAdler() {
        return (int) adler.getValue();
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
        pending = new byte[0];
        pendingLen = 0;
        pendingOff = 0;
        out = new byte[0];
        outLen = 0;
        outOff = 0;
        finishing = false;
        finished = false;
        headerWritten = false;
        adler.reset();
        bytesRead = 0;
        bytesWritten = 0;
    }

    public void end() {
        pending = new byte[0];
        out = new byte[0];
        pendingLen = 0;
        outLen = 0;
    }

    public void close() {
        end();
    }
}
