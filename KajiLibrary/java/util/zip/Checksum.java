package java.util.zip;

// A running checksum over a byte stream. The shape that matters is that it is INCREMENTAL:
// `update` is called as bytes arrive and `getValue` can be asked at any point, which is what
// lets a checksum ride along a stream decorator without ever buffering the data.
//
// OMITTED (subset): `update(java.nio.ByteBuffer)`. KajiLibrary has no `java.nio`, and the method
// adds nothing an implementation could not already do through the array form.
public interface Checksum {

    // One byte. Only the low eight bits are used, which is why the parameter is an `int` — it is
    // the value `InputStream.read()` hands back.
    void update(int b);

    // The whole array. A default, because it is the range form over the full length.
    default void update(byte[] b) {
        update(b, 0, b.length);
    }

    /**
     * Suma los bytes que quedan en `buffer`, y **lo deja consumido**.
     *
     * <p>Dejar la posicion en el limite no es un detalle de implementacion: es lo que distingue a un
     * metodo que "lee un buffer" de uno que lo espia. Sin eso, un bucle que sume y vuelva a sumar
     * procesaria los mismos bytes para siempre.
     */
    default void update(java.nio.ByteBuffer buffer) {
        int n = buffer.remaining();
        if (n <= 0) {
            return;
        }
        byte[] tmp = new byte[n];
        buffer.get(tmp, 0, n);
        this.update(tmp, 0, n);
    }

    void update(byte[] b, int off, int len);

    // The checksum so far, as an unsigned value widened into a `long`.
    long getValue();

    void reset();
}
