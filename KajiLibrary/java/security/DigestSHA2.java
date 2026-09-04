package java.security;

// SHA-224 y SHA-256, segun FIPS 180-4. La rama de 32 bits de SHA-2.
//
// Son **el mismo algoritmo**: identica funcion de compresion, identico schedule, identicas
// constantes. Lo unico que cambia es el vector inicial y que SHA-224 tira las ultimas cuatro
// palabras del resultado. Ese truncado no es solo para ahorrar bytes: hace que SHA-224 no sufra
// la extension de longitud que si afecta a SHA-256, porque el atacante no conoce el estado
// completo con el que seguiria.
//
// A diferencia de MD5 y SHA-1, estos no tienen ataques practicos: SHA-256 es la eleccion por
// defecto razonable hoy.
//
// Verificados contra los vectores de FIPS 180-4 y contra el JDK 25.
final class DigestSHA2 extends DigestBloque {

    // K[i] = los primeros 32 bits de la parte fraccionaria de la raiz cubica del primo i-esimo.
    // Igual que en MD5, son "nothing up my sleeve numbers": cualquiera puede recalcularlos y
    // comprobar que no se eligieron a dedo.
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

    // Los primeros 32 bits de la parte fraccionaria de la raiz cuadrada de los ocho primeros
    // primos.
    private static final int[] IV_256 = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };

    // El IV de SHA-224 son los **segundos** 32 bits de la raiz cuadrada del noveno al decimosexto
    // primo. Que sea otro y no el de SHA-256 es lo que evita que SHA-224(m) sea deducible de
    // SHA-256(m) o al reves.
    private static final int[] IV_224 = {
        0xc1059ed8, 0x367cd507, 0x3070dd17, 0xf70e5939,
        0xffc00b31, 0x68581511, 0x64f98fa7, 0xbefa4fa4
    };

    private final int[] iv;
    private final int[] h = new int[8];
    private final int[] w = new int[64];

    private DigestSHA2(String algoritmo, int[] iv, int largo) {
        super(algoritmo, 64, largo);
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
    void reiniciarEstado() {
        System.arraycopy(this.iv, 0, this.h, 0, 8);
    }

    @Override
    boolean bigEndian() {
        return true;
    }

    @Override
    int bytesDeLargo() {
        return 8;
    }

    @Override
    void comprimir(byte[] in, int ofs) {
        int i = 0;
        while (i < 16) {
            this.w[i] = leerIntBE(in, ofs + i * 4);
            i = i + 1;
        }
        while (i < 64) {
            int x = this.w[i - 15];
            int y = this.w[i - 2];
            int s0 = rotDer(x, 7) ^ rotDer(x, 18) ^ (x >>> 3);
            int s1 = rotDer(y, 17) ^ rotDer(y, 19) ^ (y >>> 10);
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
            int S1 = rotDer(e, 6) ^ rotDer(e, 11) ^ rotDer(e, 25);
            int ch = (e & f) ^ ((~e) & g);
            int t1 = hh + S1 + ch + K[t] + this.w[t];
            int S0 = rotDer(a, 2) ^ rotDer(a, 13) ^ rotDer(a, 22);
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

    // Escribe solo `largoDigest` bytes: para SHA-224 eso corta la ultima palabra entera.
    @Override
    void escribirEstado(byte[] out) {
        int i = 0;
        while (i * 4 + 4 <= out.length) {
            escribirIntBE(out, i * 4, this.h[i]);
            i = i + 1;
        }
    }

    @Override
    DigestBloque nuevoIgual() {
        return new DigestSHA2(this.getAlgorithm(), this.iv, this.largoDigest);
    }

    @Override
    void copiarEstadoDe(DigestBloque otro) {
        DigestSHA2 o = (DigestSHA2) otro;
        System.arraycopy(o.h, 0, this.h, 0, 8);
    }
}
