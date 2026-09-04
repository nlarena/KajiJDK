package java.security;

// SHA-1, segun FIPS 180-4.
//
// **Roto para uso criptografico** desde SHAttered (2017), que exhibio dos PDF distintos con el
// mismo digest, y desde 2020 se generan colisiones con prefijo elegido. No sirve para firmar ni
// para integridad frente a un adversario.
//
// Se implementa por el mismo motivo que MD5: sigue siendo obligatorio para leer cosas que ya
// existen —Git, HMAC-SHA1 en protocolos viejos, WebSocket— y esas cosas no dejan de existir porque
// la biblioteca no lo tenga.
//
// Verificado contra los vectores de FIPS 180-2 ("abc" y la cadena de 56 caracteres) y contra el
// JDK 25.
final class DigestSHA1 extends DigestBloque {

    private int h0;
    private int h1;
    private int h2;
    private int h3;
    private int h4;

    // El schedule expandido de 80 palabras, reusado entre bloques.
    private final int[] w = new int[80];

    DigestSHA1() {
        super("SHA-1", 64, 20);
        this.engineReset();
    }

    @Override
    void reiniciarEstado() {
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
        // La expansion a 80 palabras. La rotacion de 1 bit es lo unico que separa a SHA-1 de
        // SHA-0, y es exactamente lo que le costo a SHA-0 ser roto veinte años antes.
        while (i < 80) {
            this.w[i] = rotIzq(this.w[i - 3] ^ this.w[i - 8] ^ this.w[i - 14] ^ this.w[i - 16], 1);
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
            int tmp = rotIzq(a, 5) + f + e + k + this.w[t];
            e = d;
            d = c;
            c = rotIzq(b, 30);
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
    void escribirEstado(byte[] out) {
        escribirIntBE(out, 0, this.h0);
        escribirIntBE(out, 4, this.h1);
        escribirIntBE(out, 8, this.h2);
        escribirIntBE(out, 12, this.h3);
        escribirIntBE(out, 16, this.h4);
    }

    @Override
    DigestBloque nuevoIgual() {
        return new DigestSHA1();
    }

    @Override
    void copiarEstadoDe(DigestBloque otro) {
        DigestSHA1 o = (DigestSHA1) otro;
        this.h0 = o.h0;
        this.h1 = o.h1;
        this.h2 = o.h2;
        this.h3 = o.h3;
        this.h4 = o.h4;
    }
}
