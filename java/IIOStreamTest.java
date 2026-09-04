import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Comportamiento de javax.imageio.stream.
 *
 * <p>Es el paquete con mas logica propia del lote --orden de bytes, lectura y escritura por bits,
 * marcas apiladas, descarte-- asi que se prueba antes de construir el resto de imageio encima.
 *
 * <p>Cada caso vale contra el JDK 25 real y contra KajiJDK. {@link #run} devuelve -1 si pasan todos,
 * o el indice del primero que falla.
 */
public class IIOStreamTest {

    public static int run() {
        int i = 0;
        try {
            // ---- orden de bytes ------------------------------------------------
            byte[] data = { 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
                            (byte) 0xF0 };
            ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(data));
            // Por omision es grande primero.
            if (in.getByteOrder() != ByteOrder.BIG_ENDIAN) {
                return i;
            }
            i++;
            if (in.readInt() != 0x12345678) {
                return i;
            }
            i++;
            in.seek(0);
            in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            if (in.readInt() != 0x78563412) {
                return i;
            }
            i++;
            in.seek(0);
            in.setByteOrder(ByteOrder.BIG_ENDIAN);
            if (in.readShort() != 0x1234 || in.readUnsignedShort() != 0x5678) {
                return i;
            }
            i++;
            in.seek(4);
            // Cuatro bytes sin signo no entran en un int; por eso readUnsignedInt da long.
            if (in.readUnsignedInt() != 0x9ABCDEF0L) {
                return i;
            }
            i++;
            in.seek(0);
            if (in.readLong() != 0x123456789ABCDEF0L) {
                return i;
            }
            i++;
            in.seek(0);
            in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            if (in.readLong() != 0xF0DEBC9A78563412L) {
                return i;
            }
            i++;
            in.close();

            // ---- lectura por bits ----------------------------------------------
            // 0xB4 es 10110100.
            ImageInputStream bits = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(new byte[] { (byte) 0xB4, (byte) 0x2D }));
            if (bits.readBit() != 1 || bits.readBit() != 0 || bits.readBit() != 1
                || bits.readBit() != 1) {
                return i;
            }
            i++;
            // Cuatro bits leidos: sigue en el byte 0, desplazamiento 4.
            if (bits.getStreamPosition() != 0 || bits.getBitOffset() != 4) {
                return i;
            }
            i++;
            if (bits.readBits(4) != 0x4) {
                return i;
            }
            i++;
            // Cerrado el byte, la posicion avanza y el desplazamiento vuelve a cero.
            if (bits.getStreamPosition() != 1 || bits.getBitOffset() != 0) {
                return i;
            }
            i++;
            // Doce bits a caballo de dos bytes.
            bits.seek(0);
            if (bits.readBits(12) != 0xB42) {
                return i;
            }
            i++;
            // Cualquier lectura de byte limpia el desplazamiento.
            bits.seek(0);
            bits.setBitOffset(3);
            bits.readByte();
            if (bits.getBitOffset() != 0) {
                return i;
            }
            i++;
            if (!iae(bits, 8) || !iae(bits, -1)) {
                return i;
            }
            i++;
            bits.close();

            // ---- marcas apiladas -----------------------------------------------
            ImageInputStream m = new MemoryCacheImageInputStream(new ByteArrayInputStream(data));
            m.readInt();
            m.mark();
            m.readShort();
            m.mark();
            m.readByte();
            if (m.getStreamPosition() != 7) {
                return i;
            }
            i++;
            m.reset();
            if (m.getStreamPosition() != 6) {
                return i;
            }
            i++;
            m.reset();
            if (m.getStreamPosition() != 4) {
                return i;
            }
            i++;
            m.close();

            // ---- descarte ------------------------------------------------------
            final ImageInputStream f = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(data));
            f.seek(4);
            f.flushBefore(4);
            if (f.getFlushedPosition() != 4) {
                return i;
            }
            i++;
            // Volver antes del descarte ya no se puede.
            if (!ioobe(new Runnable() {
                public void run() {
                    try {
                        f.seek(2);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            })) {
                return i;
            }
            i++;
            f.close();

            // ---- readFully de arreglos ------------------------------------------
            ImageInputStream rf = new MemoryCacheImageInputStream(new ByteArrayInputStream(data));
            short[] shorts = new short[4];
            rf.readFully(shorts, 0, 4);
            if (shorts[0] != 0x1234 || shorts[3] != (short) 0xDEF0) {
                return i;
            }
            i++;
            rf.seek(0);
            int[] ints = new int[2];
            rf.readFully(ints, 0, 2);
            if (ints[0] != 0x12345678 || ints[1] != 0x9ABCDEF0) {
                return i;
            }
            i++;
            rf.seek(0);
            if (!eof(rf, 9)) {
                return i;
            }
            i++;
            rf.close();

            // ---- IIOByteBuffer ---------------------------------------------------
            ImageInputStream bb = new MemoryCacheImageInputStream(new ByteArrayInputStream(data));
            IIOByteBuffer buf = new IIOByteBuffer(new byte[0], 0, 0);
            bb.readBytes(buf, 3);
            if (buf.getLength() != 3 || buf.getOffset() != 0
                || buf.getData()[0] != 0x12 || buf.getData()[2] != 0x56) {
                return i;
            }
            i++;
            bb.close();

            // ---- escritura: orden de bytes ---------------------------------------
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageOutputStream os = new MemoryCacheImageOutputStream(out);
            os.writeInt(0x12345678);
            os.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            os.writeInt(0x12345678);
            os.close();
            byte[] written = out.toByteArray();
            if (written.length != 8 || written[0] != 0x12 || written[3] != 0x78
                || written[4] != 0x78 || written[7] != 0x12) {
                return i;
            }
            i++;

            // ---- escritura por bits -----------------------------------------------
            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
            ImageOutputStream bos = new MemoryCacheImageOutputStream(out2);
            // 1011 0100 = 0xB4, escrito de a bits sueltos.
            bos.writeBit(1);
            bos.writeBit(0);
            bos.writeBit(1);
            bos.writeBit(1);
            bos.writeBits(0x4, 4);
            bos.close();
            byte[] w2 = out2.toByteArray();
            if (w2.length != 1 || w2[0] != (byte) 0xB4) {
                return i;
            }
            i++;
            // Doce bits: el ultimo byte se completa con ceros.
            ByteArrayOutputStream out3 = new ByteArrayOutputStream();
            ImageOutputStream bos3 = new MemoryCacheImageOutputStream(out3);
            bos3.writeBits(0xB42, 12);
            bos3.close();
            byte[] w3 = out3.toByteArray();
            if (w3.length != 2 || w3[0] != (byte) 0xB4 || w3[1] != (byte) 0x20) {
                return i;
            }
            i++;
            // Una escritura de byte cierra el byte de bits pendiente.
            ByteArrayOutputStream out4 = new ByteArrayOutputStream();
            ImageOutputStream bos4 = new MemoryCacheImageOutputStream(out4);
            bos4.writeBits(0x5, 3);
            bos4.writeByte(0xFF);
            bos4.close();
            byte[] w4 = out4.toByteArray();
            if (w4.length != 2 || w4[0] != (byte) 0xA0 || w4[1] != (byte) 0xFF) {
                return i;
            }
            i++;

            // ---- volver atras a corregir, que es para lo que existe ----------------
            ByteArrayOutputStream out5 = new ByteArrayOutputStream();
            ImageOutputStream fix = new MemoryCacheImageOutputStream(out5);
            fix.writeInt(0);
            fix.writeInt(0x11223344);
            fix.seek(0);
            fix.writeInt(0xCAFEBABE);
            fix.close();
            byte[] w5 = out5.toByteArray();
            if (w5.length != 8 || (w5[0] & 0xFF) != 0xCA || (w5[3] & 0xFF) != 0xBE
                || (w5[4] & 0xFF) != 0x11) {
                return i;
            }
            i++;
            // Y se puede releer lo escrito.
            ByteArrayOutputStream out6 = new ByteArrayOutputStream();
            ImageOutputStream rw = new MemoryCacheImageOutputStream(out6);
            rw.writeInt(0xCAFEBABE);
            rw.seek(0);
            if (rw.readInt() != 0xCAFEBABE || rw.length() != 4) {
                return i;
            }
            i++;
            rw.close();

            // ---- UTF va en orden de red pase lo que pase ---------------------------
            ByteArrayOutputStream out7 = new ByteArrayOutputStream();
            ImageOutputStream u = new MemoryCacheImageOutputStream(out7);
            u.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            u.writeUTF("hola");
            u.close();
            byte[] w7 = out7.toByteArray();
            // El largo, 4, va grande primero aunque el flujo este en chico primero.
            if (w7.length != 6 || w7[0] != 0 || w7[1] != 4 || w7[2] != 'h') {
                return i;
            }
            i++;
            ImageInputStream ur = new MemoryCacheImageInputStream(new ByteArrayInputStream(w7));
            ur.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            if (!"hola".equals(ur.readUTF()) || ur.getByteOrder() != ByteOrder.LITTLE_ENDIAN) {
                return i;
            }
            i++;
            ur.close();

            // ---- readLine ----------------------------------------------------------
            byte[] lines = "uno\ndos\r\ntres".getBytes("ISO-8859-1");
            ImageInputStream lr = new MemoryCacheImageInputStream(new ByteArrayInputStream(lines));
            if (!"uno".equals(lr.readLine()) || !"dos".equals(lr.readLine())
                || !"tres".equals(lr.readLine()) || lr.readLine() != null) {
                return i;
            }
            i++;
            lr.close();

            // ---- que dice cada implementacion sobre su cache -----------------------
            ImageInputStream mem = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(data));
            if (!mem.isCached() || !mem.isCachedMemory() || mem.isCachedFile()) {
                return i;
            }
            i++;
            mem.close();
            File tmp = File.createTempFile("kaji", ".bin");
            tmp.deleteOnExit();
            ImageOutputStream fo = new FileImageOutputStream(tmp);
            fo.writeInt(0xDEADBEEF);
            fo.writeBits(0xF, 4);
            fo.close();
            ImageInputStream fi = new FileImageInputStream(tmp);
            // Un archivo no necesita cache: ya se puede posicionar.
            if (fi.isCached() || fi.isCachedMemory() || fi.isCachedFile()) {
                return i;
            }
            i++;
            if (fi.length() != 5 || fi.readInt() != 0xDEADBEEF
                || (fi.readByte() & 0xFF) != 0xF0) {
                return i;
            }
            i++;
            fi.close();
            ImageInputStream fc = new FileCacheImageInputStream(
                new ByteArrayInputStream(data), null);
            if (!fc.isCached() || fc.isCachedMemory() || !fc.isCachedFile()) {
                return i;
            }
            i++;
            if (fc.readInt() != 0x12345678) {
                return i;
            }
            i++;
            fc.seek(0);
            if (fc.readShort() != 0x1234) {
                return i;
            }
            i++;
            fc.close();
            ByteArrayOutputStream out8 = new ByteArrayOutputStream();
            ImageOutputStream fco = new FileCacheImageOutputStream(out8, null);
            fco.writeInt(0x11223344);
            fco.seek(0);
            fco.writeShort(0xAABB);
            fco.close();
            byte[] w8 = out8.toByteArray();
            if (w8.length != 4 || (w8[0] & 0xFF) != 0xAA || (w8[1] & 0xFF) != 0xBB
                || (w8[2] & 0xFF) != 0x33) {
                return i;
            }
            i++;
            tmp.delete();

            // ---- el flujo de abajo no se cierra ------------------------------------
            Counting counting = new Counting(new ByteArrayInputStream(data));
            ImageInputStream keep = new MemoryCacheImageInputStream(counting);
            keep.readByte();
            keep.close();
            if (counting.closed) {
                return i;
            }
            i++;
        } catch (IOException e) {
            return i;
        }
        return -1;
    }

    /** Un flujo que anota si lo cerraron. */
    static class Counting extends java.io.FilterInputStream {
        boolean closed = false;

        Counting(java.io.InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }
    }

    /** Si fijar ese desplazamiento de bit es invalido. */
    private static boolean iae(ImageInputStream s, int offset) {
        try {
            s.setBitOffset(offset);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Si leer esa cantidad de bytes se pasa del final. */
    private static boolean eof(ImageInputStream s, int n) {
        try {
            s.readFully(new byte[n], 0, n);
            return false;
        } catch (EOFException e) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean ioobe(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IndexOutOfBoundsException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
