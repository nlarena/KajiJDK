import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import jdk.nio.mapmode.ExtendedMapMode;

/**
 * Checks {@code FileChannel.map} against JDK 25.
 *
 * <h2>What is compared</h2>
 *
 * <p>That a mapping is the file and not a copy of it, which is the whole reason the method took as
 * long as it did to arrive: what a mapped buffer reports about itself, that a write through it
 * reaches the disk and that a {@code PRIVATE} one does not, what a slice and a view over it see,
 * that a mapping outlives the channel it came from, and which requests are refused and with which
 * exception.
 *
 * <p>Two things are deliberately outside the table. {@link MappedByteBuffer#isLoaded()} is a hint
 * the system is free to answer either way, so only the fact that {@code load()} returns the same
 * buffer is checked. And {@code map(MapMode, long, long, Arena)} is not called at all: it needs
 * foreign memory, which this library does not have, so it throws the
 * {@code UnsupportedOperationException} that its own signature declares while the JDK returns a
 * segment. That gap is visible in its javadoc, which is where a gap belongs.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class MAP1 {

    static final String[] EXPECTED = {
        "shape|32|0|32|true|false|BIG_ENDIAN|false",
        "bytes|0|31|67438087|2571|1736447835066146335",
        "bulk|8|15|16",
        "load|true",
        "through|99|1|4|64",
        "bulk-through|-1|-2",
        "force-range|IndexOutOfBoundsException|IndexOutOfBoundsException|ok",
        "slice|28|4|true|false|true",
        "slice-through|55",
        "dup|32|4|55",
        "ints|8|923076103|16909060",
        "ro|true|true|99|16",
        "ro-put|ReadOnlyBufferException",
        "ro-force|ok",
        "private|1|77|1|false",
        "offset|8|5|12",
        "offset-through|-5",
        "grow|64|80|20|0",
        "sync|UnsupportedOperationException|UnsupportedOperationException",
        "null-mode|NullPointerException",
        "neg-pos|IllegalArgumentException",
        "neg-size|IllegalArgumentException",
        "huge|IllegalArgumentException",
        "read-only-channel|99",
        "rc-read-write|NonWritableChannelException",
        "rc-private|NonWritableChannelException",
        "closed|ClosedChannelException",
        "write-only-channel|NonReadableChannelException",
        "zero|0|true|true|0",
        "outlives|7|7|false",
        "modes|READ_ONLY|READ_WRITE|PRIVATE|true",
    };

    /** How many checks got through before something threw. */
    static int done;

    /** Records one answer and counts it. See {@link #done}. */
    static void add(List<String> a, String s) {
        a.add(s);
        done++;
    }

    /** What mapping does, one line per check. */
    static String[] actual() throws Exception {
        final List<String> a = new ArrayList<String>();
        final Path tmp = Files.createTempFile("map1", ".bin");
        try {
            final byte[] data = new byte[64];
            for (int i = 0; i < 64; i++) {
                data[i] = (byte) i;
            }
            Files.write(tmp, data);

            final FileChannel c =
                    FileChannel.open(tmp, StandardOpenOption.READ, StandardOpenOption.WRITE);

            // ---- what a mapping is ---------------------------------------------------------
            final MappedByteBuffer m = c.map(MapMode.READ_WRITE, 0, 32);
            add(a, "shape|" + m.capacity() + "|" + m.position() + "|" + m.limit()
                    + "|" + m.isDirect() + "|" + m.isReadOnly() + "|" + m.order()
                    + "|" + m.hasArray());
            add(a, "bytes|" + m.get(0) + "|" + m.get(31) + "|" + m.getInt(4)
                    + "|" + m.getShort(10) + "|" + m.getLong(24));
            final byte[] out = new byte[8];
            m.position(8);
            m.get(out);
            add(a, "bulk|" + out[0] + "|" + out[7] + "|" + m.position());
            add(a, "load|" + (m.load() == m));

            // ---- a write through it reaches the file ---------------------------------------
            m.put(0, (byte) 99);
            m.putInt(16, 0x01020304);
            m.force();
            byte[] disk = Files.readAllBytes(tmp);
            add(a, "through|" + disk[0] + "|" + disk[16] + "|" + disk[19] + "|" + disk.length);
            m.position(20);
            m.put(new byte[] {(byte) -1, (byte) -2});
            m.force(20, 2);
            disk = Files.readAllBytes(tmp);
            add(a, "bulk-through|" + disk[20] + "|" + disk[21]);
            add(a, "force-range|" + attempt(new Force(m, -1, 2)) + "|"
                    + attempt(new Force(m, 30, 4)) + "|" + attempt(new Force(m, 0, 32)));

            // ---- slices, duplicates and views ----------------------------------------------
            m.position(4);
            final ByteBuffer s = m.slice();
            add(a, "slice|" + s.capacity() + "|" + s.get(0) + "|" + s.isDirect()
                    + "|" + s.isReadOnly() + "|" + (s instanceof MappedByteBuffer));
            s.put(0, (byte) 55);
            m.force();
            add(a, "slice-through|" + Files.readAllBytes(tmp)[4]);
            final ByteBuffer d = m.duplicate();
            add(a, "dup|" + d.capacity() + "|" + d.position() + "|" + d.get(4));
            m.position(0);
            final IntBuffer ib = m.asIntBuffer();
            add(a, "ints|" + ib.capacity() + "|" + ib.get(1) + "|" + ib.get(4));

            // ---- read-only ------------------------------------------------------------------
            final MappedByteBuffer ro = c.map(MapMode.READ_ONLY, 0, 16);
            add(a, "ro|" + ro.isReadOnly() + "|" + ro.isDirect() + "|" + ro.get(0)
                    + "|" + ro.capacity());
            add(a, "ro-put|" + attempt(new Put(ro)));
            add(a, "ro-force|" + attempt(new Force(ro, 0, 8)));

            // ---- private: writes that stay here ----------------------------------------------
            final MappedByteBuffer pv = c.map(MapMode.PRIVATE, 0, 16);
            final byte was = pv.get(1);
            pv.put(1, (byte) 77);
            pv.force();
            add(a, "private|" + was + "|" + pv.get(1) + "|" + Files.readAllBytes(tmp)[1]
                    + "|" + pv.isReadOnly());

            // ---- a mapping that does not start at zero ---------------------------------------
            final MappedByteBuffer off = c.map(MapMode.READ_WRITE, 5, 8);
            add(a, "offset|" + off.capacity() + "|" + off.get(0) + "|" + off.get(7));
            off.put(0, (byte) -5);
            off.force();
            add(a, "offset-through|" + Files.readAllBytes(tmp)[5]);

            // ---- past the end grows the file --------------------------------------------------
            final long before = Files.size(tmp);
            final MappedByteBuffer grow = c.map(MapMode.READ_WRITE, 60, 20);
            add(a, "grow|" + before + "|" + Files.size(tmp) + "|" + grow.capacity()
                    + "|" + grow.get(19));

            // ---- a mode we cannot honour ---------------------------------------------------------
            add(a, "sync|" + attempt(new Map(c, ExtendedMapMode.READ_ONLY_SYNC, 0, 8))
                    + "|" + attempt(new Map(c, ExtendedMapMode.READ_WRITE_SYNC, 0, 8)));

            // ---- what is refused ---------------------------------------------------------------
            add(a, "null-mode|" + attempt(new Map(c, null, 0, 1)));
            add(a, "neg-pos|" + attempt(new Map(c, MapMode.READ_WRITE, -1, 1)));
            add(a, "neg-size|" + attempt(new Map(c, MapMode.READ_WRITE, 0, -1)));
            add(a, "huge|" + attempt(new Map(c, MapMode.READ_WRITE, 0,
                    ((long) Integer.MAX_VALUE) + 1L)));

            final FileChannel rc = FileChannel.open(tmp, StandardOpenOption.READ);
            add(a, "read-only-channel|" + rc.map(MapMode.READ_ONLY, 0, 8).get(0));
            add(a, "rc-read-write|" + attempt(new Map(rc, MapMode.READ_WRITE, 0, 8)));
            add(a, "rc-private|" + attempt(new Map(rc, MapMode.PRIVATE, 0, 8)));
            rc.close();
            add(a, "closed|" + attempt(new Map(rc, MapMode.READ_ONLY, 0, 8)));

            final FileChannel wc = FileChannel.open(tmp, StandardOpenOption.WRITE);
            add(a, "write-only-channel|" + attempt(new Map(wc, MapMode.READ_ONLY, 0, 8)));
            wc.close();

            // ---- nothing to map ----------------------------------------------------------------
            final MappedByteBuffer z = c.map(MapMode.READ_ONLY, 0, 0);
            add(a, "zero|" + z.capacity() + "|" + z.isDirect() + "|" + z.isReadOnly()
                    + "|" + z.remaining());

            // ---- the mapping outlives the channel ----------------------------------------------
            final MappedByteBuffer after = c.map(MapMode.READ_WRITE, 0, 8);
            c.close();
            after.put(0, (byte) 7);
            after.force();
            add(a, "outlives|" + after.get(0) + "|" + Files.readAllBytes(tmp)[0]
                    + "|" + c.isOpen());

            // ---- the modes themselves ------------------------------------------------------------
            add(a, "modes|" + MapMode.READ_ONLY + "|" + MapMode.READ_WRITE + "|"
                    + MapMode.PRIVATE + "|" + (MapMode.READ_ONLY == MapMode.READ_ONLY));
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignored) {
                // The mapping may still hold the file on Windows; the temporary directory is not
                // this test's problem.
            }
        }
        return a.toArray(new String[0]);
    }

    interface Throwing {
        void run() throws Exception;
    }

    /** Runs it and returns "ok" or the simple name of whatever it threw. */
    static String attempt(Throwing r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class Map implements Throwing {
        private final FileChannel c;
        private final MapMode mode;
        private final long position;
        private final long size;

        Map(FileChannel c, MapMode mode, long position, long size) {
            this.c = c;
            this.mode = mode;
            this.position = position;
            this.size = size;
        }

        public void run() throws Exception {
            c.map(this.mode, this.position, this.size);
        }
    }

    static class Force implements Throwing {
        private final MappedByteBuffer b;
        private final int index;
        private final int length;

        Force(MappedByteBuffer b, int index, int length) {
            this.b = b;
            this.index = index;
            this.length = length;
        }

        public void run() throws Exception {
            b.force(this.index, this.length);
        }
    }

    static class Put implements Throwing {
        private final ByteBuffer b;

        Put(ByteBuffer b) {
            this.b = b;
        }

        public void run() throws Exception {
            b.put(0, (byte) 1);
        }
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000 + done;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
