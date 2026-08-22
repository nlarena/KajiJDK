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

    void update(byte[] b, int off, int len);

    // The checksum so far, as an unsigned value widened into a `long`.
    long getValue();

    void reset();
}
