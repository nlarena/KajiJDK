package java.util.zip;

import java.io.OutputStream;

// gzip (RFC 1952) on top of a raw DEFLATE stream. The three layers are worth keeping straight,
// because all three are called "compression" in conversation:
//
//   DEFLATE (RFC 1951)  the algorithm — blocks, Huffman codes, back-references
//   zlib    (RFC 1950)  a 2-byte header and an Adler-32 trailer around it
//   gzip    (RFC 1952)  a 10-byte header and a CRC-32 + size trailer around it
//
// So this class is not "more compressed" than a `DeflaterOutputStream`; it is the same bytes in
// a different envelope. The deflater is built with `nowrap = true` for exactly that reason —
// otherwise the stream would carry both envelopes.
public class GZIPOutputStream extends DeflaterOutputStream {

    protected CRC32 crc;

    public GZIPOutputStream(OutputStream out, int size, boolean syncFlush) {
        super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true), size, syncFlush);
        this.crc = new CRC32();
        writeHeader();
    }

    public GZIPOutputStream(OutputStream out, int size) {
        this(out, size, false);
    }

    public GZIPOutputStream(OutputStream out, boolean syncFlush) {
        this(out, 512, syncFlush);
    }

    public GZIPOutputStream(OutputStream out) {
        this(out, 512, false);
    }

    // The header is fixed except for the timestamp, which is left at zero: "no time available"
    // is a legal value, and KajiLibrary has no clock native to ask.
    private void writeHeader() {
        out.write(31);          // magic, low byte
        out.write(139);         // magic, high byte (0x8b)
        out.write(8);           // compression method: deflate
        out.write(0);           // flags: no name, no comment, no extra field, no header CRC
        out.write(0);           // modification time, four bytes, little-endian
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);           // extra flags
        out.write(255);         // operating system: unknown
    }

    public void write(byte[] b, int off, int len) {
        // El cuerpo de `DeflaterOutputStream.write` inlineado en vez de `super.write(...)`, que el
        // emisor todavia no soporta (finding #125). Son dos lineas, asi que copiarlas cuesta menos
        // que el rodeo de renombrar el metodo del padre.
        def.setInput(b, off, len);
        deflate();
        // The trailer checksums the UNCOMPRESSED bytes, so they are counted on the way in.
        crc.update(b, off, len);
    }

    public void finish() {
        if (!def.finished()) {
            // Idem: el cuerpo de `DeflaterOutputStream.finish` inlineado (finding #125).
            def.finish();
            deflate();
            writeTrailer();
        }
    }

    // CRC-32 then the uncompressed size, both little-endian — the opposite byte order from
    // zlib's Adler-32 trailer, which is one of the small ways the two envelopes differ.
    private void writeTrailer() {
        long sum = crc.getValue();
        long total = def.getBytesRead();
        writeInt(sum);
        writeInt(total);
    }

    private void writeInt(long value) {
        out.write((int) (value & 0xff));
        out.write((int) ((value >> 8) & 0xff));
        out.write((int) ((value >> 16) & 0xff));
        out.write((int) ((value >> 24) & 0xff));
    }
}
