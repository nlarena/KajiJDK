package java.security;

// SHA-1, according to FIPS 180-4.
//
// **Broken for cryptographic use** since SHAttered (2017), which exhibited two different PDFs with
// the same digest, and since 2020 collisions with a chosen prefix are generated. It does not serve
// for signing or for integrity against an adversary.
//
// It is implemented for the same reason as MD5: it is still compulsory for reading things that
// exist already —Git, HMAC-SHA1 in old protocols, WebSocket— and those things do not stop existing
// because the library does not have it.
//
// Checked against the vectors of FIPS 180-2 ("abc" and the 56-character string) and against
// JDK 25.
final class DigestSHA1 extends BlockDigest {

    private int h0;
    private int h1;
    private int h2;
    private int h3;
    private int h4;

    // The expanded schedule of 80 words, reused between blocks.
    private final int[] w = new int[80];

    DigestSHA1() {
        super("SHA-1", 64, 20);
        this.engineReset();
    }

    @Override
    void resetState() {
        this.h0 = 0x67452301;
        this.h1 = 0xefcdab89;
        this.h2 = 0x98badcfe;
        this.h3 = 0x10325476;
        this.h4 = 0xc3d2e1f0;
    }

    @Override
    boolean bigEndian() {
        return true;
    }

    @Override
    int lengthBytes() {
        return 8;
    }

    @Override
    void compress(byte[] in, int ofs) {
        int i = 0;
        while (i < 16) {
            this.w[i] = readIntBE(in, ofs + i * 4);
            i = i + 1;
        }
        // The expansion to 80 words. The rotation of 1 bit is the only thing that separates SHA-1
        // from SHA-0, and it is exactly what cost SHA-0 being broken twenty years earlier.
        while (i < 80) {
            this.w[i] = rotLeft(this.w[i - 3] ^ this.w[i - 8] ^ this.w[i - 14] ^ this.w[i - 16], 1);
            i = i + 1;
        }

        int a = this.h0;
        int b = this.h1;
        int c = this.h2;
        int d = this.h3;
        int e = this.h4;

        int t = 0;
        while (t < 80) {
            int f;
            int k;
            if (t < 20) {
                f = (b & c) | ((~b) & d);
                k = 0x5a827999;
            } else if (t < 40) {
                f = b ^ c ^ d;
                k = 0x6ed9eba1;
            } else if (t < 60) {
                f = (b & c) | (b & d) | (c & d);
                k = 0x8f1bbcdc;
            } else {
                f = b ^ c ^ d;
                k = 0xca62c1d6;
            }
            int tmp = rotLeft(a, 5) + f + e + k + this.w[t];
            e = d;
            d = c;
            c = rotLeft(b, 30);
            b = a;
            a = tmp;
            t = t + 1;
        }

        this.h0 = this.h0 + a;
        this.h1 = this.h1 + b;
        this.h2 = this.h2 + c;
        this.h3 = this.h3 + d;
        this.h4 = this.h4 + e;
    }

    @Override
    void writeState(byte[] out) {
        writeIntBE(out, 0, this.h0);
        writeIntBE(out, 4, this.h1);
        writeIntBE(out, 8, this.h2);
        writeIntBE(out, 12, this.h3);
        writeIntBE(out, 16, this.h4);
    }

    @Override
    BlockDigest freshInstance() {
        return new DigestSHA1();
    }

    @Override
    void copyStateFrom(BlockDigest other) {
        DigestSHA1 o = (DigestSHA1) other;
        this.h0 = o.h0;
        this.h1 = o.h1;
        this.h2 = o.h2;
        this.h3 = o.h3;
        this.h4 = o.h4;
    }
}
