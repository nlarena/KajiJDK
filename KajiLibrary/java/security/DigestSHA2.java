package java.security;

// SHA-224 and SHA-256, according to FIPS 180-4. The 32-bit branch of SHA-2.
//
// They are **the same algorithm**: identical compression function, identical schedule, identical
// constants. The only thing that changes is the initial vector and that SHA-224 throws away the
// last four words of the result. That truncation is not only for saving bytes: it makes SHA-224 not
// suffer the length extension that does affect SHA-256, because the attacker does not know the
// complete state they would go on with.
//
// Unlike MD5 and SHA-1, these have no practical attacks: SHA-256 is the reasonable default choice
// today.
//
// Checked against the vectors of FIPS 180-4 and against JDK 25.
final class DigestSHA2 extends BlockDigest {

    // K[i] = the first 32 bits of the fractional part of the cube root of the i-th prime. Just as
    // in MD5, they are "nothing up my sleeve numbers": anybody can recompute them and check that
    // they were not hand-picked.
    private static final int[] K = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    // The first 32 bits of the fractional part of the square root of the first eight primes.
    private static final int[] IV_256 = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };

    // The IV of SHA-224 is the **second** 32 bits of the square root of the ninth to the sixteenth
    // prime. That it is another and not that of SHA-256 is what keeps SHA-224(m) from being
    // deducible from SHA-256(m) or the other way round.
    private static final int[] IV_224 = {
        0xc1059ed8, 0x367cd507, 0x3070dd17, 0xf70e5939,
        0xffc00b31, 0x68581511, 0x64f98fa7, 0xbefa4fa4
    };

    private final int[] iv;
    private final int[] h = new int[8];
    private final int[] w = new int[64];

    private DigestSHA2(String algorithmName, int[] iv, int len) {
        super(algorithmName, 64, len);
        this.iv = iv;
        this.engineReset();
    }

    static DigestSHA2 sha256() {
        return new DigestSHA2("SHA-256", IV_256, 32);
    }

    static DigestSHA2 sha224() {
        return new DigestSHA2("SHA-224", IV_224, 28);
    }

    @Override
    void resetState() {
        System.arraycopy(this.iv, 0, this.h, 0, 8);
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
        while (i < 64) {
            int x = this.w[i - 15];
            int y = this.w[i - 2];
            int s0 = rotRight(x, 7) ^ rotRight(x, 18) ^ (x >>> 3);
            int s1 = rotRight(y, 17) ^ rotRight(y, 19) ^ (y >>> 10);
            this.w[i] = this.w[i - 16] + s0 + this.w[i - 7] + s1;
            i = i + 1;
        }

        int a = this.h[0];
        int b = this.h[1];
        int c = this.h[2];
        int d = this.h[3];
        int e = this.h[4];
        int f = this.h[5];
        int g = this.h[6];
        int hh = this.h[7];

        int t = 0;
        while (t < 64) {
            int S1 = rotRight(e, 6) ^ rotRight(e, 11) ^ rotRight(e, 25);
            int ch = (e & f) ^ ((~e) & g);
            int t1 = hh + S1 + ch + K[t] + this.w[t];
            int S0 = rotRight(a, 2) ^ rotRight(a, 13) ^ rotRight(a, 22);
            int maj = (a & b) ^ (a & c) ^ (b & c);
            int t2 = S0 + maj;
            hh = g;
            g = f;
            f = e;
            e = d + t1;
            d = c;
            c = b;
            b = a;
            a = t1 + t2;
            t = t + 1;
        }

        this.h[0] = this.h[0] + a;
        this.h[1] = this.h[1] + b;
        this.h[2] = this.h[2] + c;
        this.h[3] = this.h[3] + d;
        this.h[4] = this.h[4] + e;
        this.h[5] = this.h[5] + f;
        this.h[6] = this.h[6] + g;
        this.h[7] = this.h[7] + hh;
    }

    // It writes only `digestLen` bytes: for SHA-224 that cuts the last word whole.
    @Override
    void writeState(byte[] out) {
        int i = 0;
        while (i * 4 + 4 <= out.length) {
            writeIntBE(out, i * 4, this.h[i]);
            i = i + 1;
        }
    }

    @Override
    BlockDigest freshInstance() {
        return new DigestSHA2(this.getAlgorithm(), this.iv, this.digestLen);
    }

    @Override
    void copyStateFrom(BlockDigest other) {
        DigestSHA2 o = (DigestSHA2) other;
        System.arraycopy(o.h, 0, this.h, 0, 8);
    }
}
