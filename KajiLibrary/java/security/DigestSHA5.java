package java.security;

// SHA-384 y SHA-512, segun FIPS 180-4. La rama de 64 bits de SHA-2.
//
// Misma estructura que SHA-256 con todo escalado: palabras de 64 bits, bloques de 128 bytes, 80
// rondas, y otras cantidades de rotacion. En maquinas de 64 bits suele ser **mas rapido** que
// SHA-256 pese a dar el doble de salida, porque procesa el doble de mensaje por ronda.
//
// SHA-384 es SHA-512 con otro IV y truncado a 48 bytes; igual que con SHA-224, el truncado es lo
// que lo hace inmune a la extension de longitud.
//
// Verificados contra los vectores de FIPS 180-4 y contra el JDK 25.
final class DigestSHA5 extends DigestBloque {

    // Los primeros 64 bits de la parte fraccionaria de la raiz cubica de los primeros 80 primos.
    private static final long[] K = {
        0x428a2f98d728ae22L, 0x7137449123ef65cdL,
        0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL,
        0x3956c25bf348b538L, 0x59f111f1b605d019L,
        0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L,
        0xd807aa98a3030242L, 0x12835b0145706fbeL,
        0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L,
        0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L,
        0x9bdc06a725c71235L, 0xc19bf174cf692694L,
        0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L,
        0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
        0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L,
        0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
        0x983e5152ee66dfabL, 0xa831c66d2db43210L,
        0xb00327c898fb213fL, 0xbf597fc7beef0ee4L,
        0xc6e00bf33da88fc2L, 0xd5a79147930aa725L,
        0x06ca6351e003826fL, 0x142929670a0e6e70L,
        0x27b70a8546d22ffcL, 0x2e1b21385c26c926L,
        0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
        0x650a73548baf63deL, 0x766a0abb3c77b2a8L,
        0x81c2c92e47edaee6L, 0x92722c851482353bL,
        0xa2bfe8a14cf10364L, 0xa81a664bbc423001L,
        0xc24b8b70d0f89791L, 0xc76c51a30654be30L,
        0xd192e819d6ef5218L, 0xd69906245565a910L,
        0xf40e35855771202aL, 0x106aa07032bbd1b8L,
        0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L,
        0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L,
        0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL,
        0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
        0x748f82ee5defb2fcL, 0x78a5636f43172f60L,
        0x84c87814a1f0ab72L, 0x8cc702081a6439ecL,
        0x90befffa23631e28L, 0xa4506cebde82bde9L,
        0xbef9a3f7b2c67915L, 0xc67178f2e372532bL,
        0xca273eceea26619cL, 0xd186b8c721c0c207L,
        0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L,
        0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L,
        0x113f9804bef90daeL, 0x1b710b35131c471bL,
        0x28db77f523047d84L, 0x32caab7b40c72493L,
        0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL,
        0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL,
        0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L
    };

    private static final long[] IV_512 = {
        0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL,
        0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
        0x510e527fade682d1L, 0x9b05688c2b3e6c1fL,
        0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L
    };

    private static final long[] IV_384 = {
        0xcbbb9d5dc1059ed8L, 0x629a292a367cd507L,
        0x9159015a3070dd17L, 0x152fecd8f70e5939L,
        0x67332667ffc00b31L, 0x8eb44a8768581511L,
        0xdb0c2e0d64f98fa7L, 0x47b5481dbefa4fa4L
    };

    private final long[] iv;
    private final long[] h = new long[8];
    private final long[] w = new long[80];

    private DigestSHA5(String algoritmo, long[] iv, int largo) {
        super(algoritmo, 128, largo);
        this.iv = iv;
        this.engineReset();
    }

    static DigestSHA5 sha512() {
        return new DigestSHA5("SHA-512", IV_512, 64);
    }

    static DigestSHA5 sha384() {
        return new DigestSHA5("SHA-384", IV_384, 48);
    }

    @Override
    void reiniciarEstado() {
        System.arraycopy(this.iv, 0, this.h, 0, 8);
    }

    @Override
    boolean bigEndian() {
        return true;
    }

    // 16 bytes de contador: la especificacion define el largo como un entero de 128 bits. Los 8
    // altos van siempre en cero — harian falta 2^61 bytes de mensaje para usarlos.
    @Override
    int bytesDeLargo() {
        return 16;
    }

    @Override
    void comprimir(byte[] in, int ofs) {
        int i = 0;
        while (i < 16) {
            this.w[i] = leerLongBE(in, ofs + i * 8);
            i = i + 1;
        }
        while (i < 80) {
            long x = this.w[i - 15];
            long y = this.w[i - 2];
            long s0 = rotDer(x, 1) ^ rotDer(x, 8) ^ (x >>> 7);
            long s1 = rotDer(y, 19) ^ rotDer(y, 61) ^ (y >>> 6);
            this.w[i] = this.w[i - 16] + s0 + this.w[i - 7] + s1;
            i = i + 1;
        }

        long a = this.h[0];
        long b = this.h[1];
        long c = this.h[2];
        long d = this.h[3];
        long e = this.h[4];
        long f = this.h[5];
        long g = this.h[6];
        long hh = this.h[7];

        int t = 0;
        while (t < 80) {
            long S1 = rotDer(e, 14) ^ rotDer(e, 18) ^ rotDer(e, 41);
            long ch = (e & f) ^ ((~e) & g);
            long t1 = hh + S1 + ch + K[t] + this.w[t];
            long S0 = rotDer(a, 28) ^ rotDer(a, 34) ^ rotDer(a, 39);
            long maj = (a & b) ^ (a & c) ^ (b & c);
            long t2 = S0 + maj;
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

    @Override
    void escribirEstado(byte[] out) {
        int i = 0;
        while (i * 8 + 8 <= out.length) {
            long v = this.h[i];
            int j = 0;
            while (j < 8) {
                out[i * 8 + j] = (byte) (v >>> (56 - 8 * j));
                j = j + 1;
            }
            i = i + 1;
        }
    }

    @Override
    DigestBloque nuevoIgual() {
        return new DigestSHA5(this.getAlgorithm(), this.iv, this.largoDigest);
    }

    @Override
    void copiarEstadoDe(DigestBloque otro) {
        DigestSHA5 o = (DigestSHA5) otro;
        System.arraycopy(o.h, 0, this.h, 0, 8);
    }
}
