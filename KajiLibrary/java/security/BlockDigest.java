package java.security;

// The part MD5, SHA-1 and the whole SHA-2 family have in common: buffering the input in blocks of
// fixed size and closing with the Merkle-Damgard padding.
//
// The four functions this library implements are Merkle-Damgard constructions and share the same
// outer structure: the message is split into blocks, a compression function is applied to each one
// dragging a state along, and at the end a padding is added that **includes the length of the
// message**. That length at the end is not decorative — without it, two different messages where
// one is the other with zeroes in front would give the same hash.
//
// The only thing that changes between algorithms is the compression function, the block size and
// whether the counter of bits goes little or big endian. All that is left to the subclasses.
abstract class BlockDigest extends MessageDigest implements Cloneable {

    // 64 bytes for MD5/SHA-1/SHA-256, 128 for SHA-512.
    final int blockSize;

    // How many bytes `digest()` returns. It can be smaller than the internal state: SHA-224 and
    // SHA-384 compute the complete state and **truncate**, and that is what makes them resistant to
    // the length extension that does affect their complete siblings.
    final int digestLen;

    // The block that is being filled.
    final byte[] buffer;

    int inBuffer;

    // Total bytes consumed since the last reset. It is needed whole, not modulo the block, because
    // it goes written in the padding.
    long totalBytes;

    BlockDigest(String algorithmName, int blockSize, int digestLen) {
        super(algorithmName);
        this.blockSize = blockSize;
        this.digestLen = digestLen;
        this.buffer = new byte[blockSize];
    }

    // It consumes a complete block of `b` from `ofs`.
    abstract void compress(byte[] b, int ofs);

    // It dumps the internal state into `out`, truncating to `digestLen`.
    abstract void writeState(byte[] out);

    // It leaves the state at the initial vector of the algorithm.
    abstract void resetState();

    // A new instance of the same algorithm, for cloning.
    abstract BlockDigest freshInstance();

    // It copies the compression state of `other`. The buffer and the counters are copied by
    // `clone`.
    abstract void copyStateFrom(BlockDigest other);

    @Override
    protected final int engineGetDigestLength() {
        return this.digestLen;
    }

    @Override
    protected final void engineReset() {
        this.inBuffer = 0;
        this.totalBytes = 0L;
        int i = 0;
        while (i < this.buffer.length) {
            this.buffer[i] = 0;
            i = i + 1;
        }
        this.resetState();
    }

    @Override
    protected final void engineUpdate(byte input) {
        this.buffer[this.inBuffer] = input;
        this.inBuffer = this.inBuffer + 1;
        this.totalBytes = this.totalBytes + 1L;
        if (this.inBuffer == this.blockSize) {
            this.compress(this.buffer, 0);
            this.inBuffer = 0;
        }
    }

    // The whole blocks that are in `input` already are compressed **in place**, without copying
    // them into the buffer. With big inputs that copy would be the dominant cost.
    @Override
    protected final void engineUpdate(byte[] input, int offset, int len) {
        if (len == 0) {
            return;
        }
        if (offset < 0 || len < 0 || offset > input.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.totalBytes = this.totalBytes + (long) len;
        int ofs = offset;
        int n = len;
        if (this.inBuffer > 0) {
            int missing = this.blockSize - this.inBuffer;
            int howMany = n < missing ? n : missing;
            System.arraycopy(input, ofs, this.buffer, this.inBuffer, howMany);
            this.inBuffer = this.inBuffer + howMany;
            ofs = ofs + howMany;
            n = n - howMany;
            if (this.inBuffer == this.blockSize) {
                this.compress(this.buffer, 0);
                this.inBuffer = 0;
            }
        }
        while (n >= this.blockSize) {
            this.compress(input, ofs);
            ofs = ofs + this.blockSize;
            n = n - this.blockSize;
        }
        if (n > 0) {
            System.arraycopy(input, ofs, this.buffer, 0, n);
            this.inBuffer = n;
        }
    }

    @Override
    protected final byte[] engineDigest() {
        this.pad(this.bigEndian(), this.lengthBytes());
        byte[] out = new byte[this.digestLen];
        this.writeState(out);
        this.engineReset();
        return out;
    }

    // Whether the final counter of bits goes big endian. MD5 is the only one that says no.
    abstract boolean bigEndian();

    // How many bytes the counter takes at the end of the last block: 8 for blocks of 64, 16 for
    // those of 128.
    abstract int lengthBytes();

    // The padding: one bit at 1, zeroes, and the length of the message in bits.
    //
    // The interesting case is when the 0x80 and the zeroes do not fit together with the counter in
    // the block that was being filled: then that block is closed with zeroes, it is compressed, and
    // the counter goes alone in one more. Without that, a message of exactly the right length would
    // lose its own length.
    private void pad(boolean big, int lenBytes) {
        long bits = this.totalBytes << 3;
        this.buffer[this.inBuffer] = (byte) 0x80;
        this.inBuffer = this.inBuffer + 1;
        if (this.inBuffer > this.blockSize - lenBytes) {
            while (this.inBuffer < this.blockSize) {
                this.buffer[this.inBuffer] = 0;
                this.inBuffer = this.inBuffer + 1;
            }
            this.compress(this.buffer, 0);
            this.inBuffer = 0;
        }
        while (this.inBuffer < this.blockSize - lenBytes) {
            this.buffer[this.inBuffer] = 0;
            this.inBuffer = this.inBuffer + 1;
        }
        if (big) {
            // The high bytes of the counter —the first 8 in SHA-512— are left at zero: this
            // implementation cannot receive 2^61 bytes.
            int i = this.inBuffer;
            while (i < this.blockSize - 8) {
                this.buffer[i] = 0;
                i = i + 1;
            }
            int base = this.blockSize - 8;
            int j = 0;
            while (j < 8) {
                this.buffer[base + j] = (byte) (bits >>> (56 - 8 * j));
                j = j + 1;
            }
        } else {
            int j = 0;
            while (j < 8) {
                this.buffer[this.inBuffer + j] = (byte) (bits >>> (8 * j));
                j = j + 1;
            }
        }
        this.compress(this.buffer, 0);
        this.inBuffer = 0;
    }

    // Cloning a half-done digest is what allows a common prefix to be hashed only once and then
    // followed by two different branches. Everything is copied by hand —not `Object.clone`— so that
    // it is plain to see that the buffer of the clone is its own: sharing it would make the two
    // branches step on each other.
    @Override
    public Object clone() throws CloneNotSupportedException {
        BlockDigest c = this.freshInstance();
        System.arraycopy(this.buffer, 0, c.buffer, 0, this.blockSize);
        c.inBuffer = this.inBuffer;
        c.totalBytes = this.totalBytes;
        c.copyStateFrom(this);
        c.setProvider(this.getProvider());
        return c;
    }

    // The four algorithms read and write big endian words except MD5. These helpers are here so
    // that the subclasses do not repeat the shifting of bytes.

    static int readIntBE(byte[] b, int i) {
        return ((b[i] & 0xff) << 24) | ((b[i + 1] & 0xff) << 16)
             | ((b[i + 2] & 0xff) << 8) | (b[i + 3] & 0xff);
    }

    static int readIntLE(byte[] b, int i) {
        return (b[i] & 0xff) | ((b[i + 1] & 0xff) << 8)
             | ((b[i + 2] & 0xff) << 16) | ((b[i + 3] & 0xff) << 24);
    }

    static long readLongBE(byte[] b, int i) {
        long v = 0L;
        int j = 0;
        while (j < 8) {
            v = (v << 8) | (long) (b[i + j] & 0xff);
            j = j + 1;
        }
        return v;
    }

    static void writeIntBE(byte[] out, int i, int v) {
        out[i] = (byte) (v >>> 24);
        out[i + 1] = (byte) (v >>> 16);
        out[i + 2] = (byte) (v >>> 8);
        out[i + 3] = (byte) v;
    }

    static void writeIntLE(byte[] out, int i, int v) {
        out[i] = (byte) v;
        out[i + 1] = (byte) (v >>> 8);
        out[i + 2] = (byte) (v >>> 16);
        out[i + 3] = (byte) (v >>> 24);
    }

    static int rotLeft(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    static int rotRight(int x, int n) {
        return (x >>> n) | (x << (32 - n));
    }

    static long rotRight(long x, int n) {
        return (x >>> n) | (x << (64 - n));
    }
}
