package java.security;

// MD5, segun el RFC 1321.
//
// **Esta roto para uso criptografico.** Se conocen colisiones desde 2004 y se generan en segundos
// en una notebook: dos entradas distintas con el mismo digest. No sirve para firmar, ni para
// integridad frente a un adversario, ni para guardar contraseñas.
//
// Se implementa igual porque sigue apareciendo donde no hay adversario —checksums de formatos
// viejos, ETags, deduplicacion— y porque la alternativa realista a no tenerlo no es que nadie lo
// use: es que cada uno se escriba el suyo. Lo que esta clase promete es exactamente lo que hace,
// que es devolver el MD5 que define el RFC.
//
// Verificado contra los vectores del apendice A.5 del RFC 1321 y contra el JDK 25.
final class DigestMD5 extends DigestBloque {

    // K[i] = floor(2^32 * |sin(i+1)|), con i en radianes. Es la unica fuente de "numeros al azar"
    // del algoritmo: se sacan de una funcion trascendente para que nadie pueda sospechar que
    // fueron elegidos para dejar una puerta.
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

    // Cuanto rota cada paso. Cuatro valores por ronda, repetidos de a cuatro pasos.
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

    // Reusado entre bloques: son 16 palabras por bloque y pedirle 64 bytes al recolector por cada
    // uno seria el costo dominante en entradas grandes.
    private final int[] x = new int[16];

    DigestMD5() {
        super("MD5", 64, 16);
        this.engineReset();
    }

    @Override
    void reiniciarEstado() {
        this.a = 0x67452301;
        this.b = 0xefcdab89;
        this.c = 0x98badcfe;
        this.d = 0x10325476;
    }

    // MD5 lee sus palabras en little endian. Es el unico de los cuatro; venia de una epoca en que
    // los procesadores de escritorio eran little endian y no habia una convencion de red.
    @Override
    boolean bigEndian() {
        return false;
    }

    @Override
    int bytesDeLargo() {
        return 8;
    }

    @Override
    void comprimir(byte[] in, int ofs) {
        int i = 0;
        while (i < 16) {
            this.x[i] = leerIntLE(in, ofs + i * 4);
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
            bb = bb + rotIzq(aa + f + K[paso] + this.x[g], S[paso]);
            aa = tmp;
            paso = paso + 1;
        }

        this.a = this.a + aa;
        this.b = this.b + bb;
        this.c = this.c + cc;
        this.d = this.d + dd;
    }

    @Override
    void escribirEstado(byte[] out) {
        escribirIntLE(out, 0, this.a);
        escribirIntLE(out, 4, this.b);
        escribirIntLE(out, 8, this.c);
        escribirIntLE(out, 12, this.d);
    }

    @Override
    DigestBloque nuevoIgual() {
        return new DigestMD5();
    }

    @Override
    void copiarEstadoDe(DigestBloque otro) {
        DigestMD5 o = (DigestMD5) otro;
        this.a = o.a;
        this.b = o.b;
        this.c = o.c;
        this.d = o.d;
    }
}
