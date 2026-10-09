package java.security;

// MD5, according to RFC 1321.
//
// **It is broken for cryptographic use.** Collisions have been known since 2004 and are generated
// in seconds on a laptop: two different inputs with the same digest. It does not serve for signing,
// or for integrity against an adversary, or for keeping passwords.
//
// It is implemented all the same because it still appears where there is no adversary —checksums of
// old formats, ETags, deduplication— and because the realistic alternative to not having it is not
// that nobody uses it: it is that everybody writes their own. What this class promises is exactly
// what it does, which is to return the MD5 the RFC defines.
//
// Checked against the vectors of appendix A.5 of RFC 1321 and against JDK 25.
final class DigestMD5 extends BlockDigest {

    // K[i] = floor(2^32 * |sin(i+1)|), with i in radians. It is the only source of "random numbers"
    // of the algorithm: they are taken from a transcendental function so that nobody can suspect
    // they were chosen to leave a door open.
    private static final int[] K = {
        0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
        0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
        0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
        0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
        0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
        0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
        0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
        0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
        0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
        0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
        0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
        0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
        0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
        0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
        0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
        0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391
    };

    // How much each step rotates. Four values per round, repeated every four steps.
    private static final int[] S = {
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    };

    private int a;
    private int b;
    private int c;
    private int d;

    // Reused between blocks: it is 16 words per block and asking the collector for 64 bytes for
    // each one would be the dominant cost with big inputs.
    private final int[] x = new int[16];

    DigestMD5() {
        super("MD5", 64, 16);
        this.engineReset();
    }

    @Override
    void resetState() {
        this.a = 0x67452301;
        this.b = 0xefcdab89;
        this.c = 0x98badcfe;
        this.d = 0x10325476;
    }

    // MD5 reads its words in little endian. It is the only one of the four; it came from a time
    // when desktop processors were little endian and there was no network convention.
    @Override
    boolean bigEndian() {
        return false;
    }

    @Override
    int lengthBytes() {
        return 8;
    }

    @Override
    void compress(byte[] in, int ofs) {
        int i = 0;
        while (i < 16) {
            this.x[i] = readIntLE(in, ofs + i * 4);
            i = i + 1;
        }
        int aa = this.a;
        int bb = this.b;
        int cc = this.c;
        int dd = this.d;

        int paso = 0;
        while (paso < 64) {
            int f;
            int g;
            if (paso < 16) {
                f = (bb & cc) | ((~bb) & dd);
                g = paso;
            } else if (paso < 32) {
                f = (dd & bb) | ((~dd) & cc);
                g = (5 * paso + 1) & 15;
            } else if (paso < 48) {
                f = bb ^ cc ^ dd;
                g = (3 * paso + 5) & 15;
            } else {
                f = cc ^ (bb | (~dd));
                g = (7 * paso) & 15;
            }
            int tmp = dd;
            dd = cc;
            cc = bb;
            bb = bb + rotLeft(aa + f + K[paso] + this.x[g], S[paso]);
            aa = tmp;
            paso = paso + 1;
        }

        this.a = this.a + aa;
        this.b = this.b + bb;
        this.c = this.c + cc;
        this.d = this.d + dd;
    }

    @Override
    void writeState(byte[] out) {
        writeIntLE(out, 0, this.a);
        writeIntLE(out, 4, this.b);
        writeIntLE(out, 8, this.c);
        writeIntLE(out, 12, this.d);
    }

    @Override
    BlockDigest freshInstance() {
        return new DigestMD5();
    }

    @Override
    void copyStateFrom(BlockDigest other) {
        DigestMD5 o = (DigestMD5) other;
        this.a = o.a;
        this.b = o.b;
        this.c = o.c;
        this.d = o.d;
    }
}
