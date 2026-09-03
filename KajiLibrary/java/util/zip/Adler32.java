package java.util.zip;

// Adler-32 (RFC 1950 3.2.1) — the checksum zlib carries. Two running sums: `a` accumulates the
// bytes, `b` accumulates `a`. That second sum is the whole idea: a plain byte sum cannot see a
// reordering, but summing the running total makes every byte's contribution depend on its
// POSITION, so swapping two bytes changes the result.
//
// It is much weaker than CRC-32 at catching burst errors, and much cheaper — no table, no
// polynomial, just two additions per byte. zlib picked it for exactly that trade.
public class Adler32 implements Checksum {

    // The largest prime below 65536. Keeping the sums reduced modulo a prime is what spreads the
    // carries around instead of letting them pile into the high bits.
    private static final int BASE = 65521;

    // How many bytes can be folded in before the accumulators could overflow a signed 32-bit
    // int. 5552 is the standard bound: it is the largest n for which the worst case still fits.
    private static final int NMAX = 5552;

    private int a;
    private int b;

    public Adler32() {
        reset();
    }

    public void update(int value) {
        a = (a + (value & 0xff)) % BASE;
        b = (b + a) % BASE;
    }

    /**
     * Suma los bytes que quedan en `buffer`, y **lo deja consumido**.
     *
     * <p>Dejar la posicion en el limite no es un detalle de implementacion: es lo que distingue a un
     * metodo que "lee un buffer" de uno que lo espia. Sin eso, un bucle que sume y vuelva a sumar
     * procesaria los mismos bytes para siempre.
     */
    public void update(java.nio.ByteBuffer buffer) {
        int n = buffer.remaining();
        if (n <= 0) {
            return;
        }
        byte[] tmp = new byte[n];
        buffer.get(tmp, 0, n);
        this.update(tmp, 0, n);
    }

    public void update(byte[] buf, int off, int len) {
        int i = off;
        int remaining = len;
        while (remaining > 0) {
            // Take the modulo once per block instead of once per byte: the point of NMAX.
            int block = remaining;
            if (block > NMAX) {
                block = NMAX;
            }
            int end = i + block;
            while (i < end) {
                a = a + (buf[i] & 0xff);
                b = b + a;
                i = i + 1;
            }
            a = a % BASE;
            b = b % BASE;
            remaining = remaining - block;
        }
    }

    public void reset() {
        // `a` starts at one, not zero — that is what makes an empty input checksum to 1 instead
        // of 0, so a missing stream is distinguishable from an empty one.
        a = 1;
        b = 0;
    }

    public long getValue() {
        return ((long) b << 16) | (long) a;
    }
}
