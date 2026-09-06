import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * Checks file locking in {@code java.nio.channels} against JDK 25.
 *
 * <h2>What is compared</h2>
 *
 * <p>The whole contract of {@code FileChannel.lock}/{@code tryLock} and of the six asynchronous
 * forms: what a lock reports about itself, which requests are rejected and with which exception,
 * what happens on a read-only channel, and that closing the channel invalidates the locks it held.
 *
 * <p>What is not compared is {@code toString}, which names the implementation class and therefore
 * cannot match, and exclusion between two processes, which needs a second JVM and belongs in a test
 * that can start one.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class LCK1 {

    static final String[] EXPECTED = {
        "whole|0|9223372036854775807|false|true|true|true",
        "overlaps|true|true",
        "second|OverlappingFileLockException",
        "second-try|OverlappingFileLockException",
        "released|false|ok",
        "side-by-side|true|true|0|8|8",
        "shared|true|true",
        "shared-twice|OverlappingFileLockException",
        "bad-args|IllegalArgumentException|IllegalArgumentException",
        "past-the-end|true",
        "closing|false|false",
        "lock-when-closed|ClosedChannelException",
        "read-only-exclusive|NonWritableChannelException",
        "read-only-shared|true",
        "async-lock|0|9223372036854775807|false|true|true|true",
        "async-second|OverlappingFileLockException",
        "async-try|true|0|16",
        "async-handler|ok:0:8:true",
        "async-closed|false",
        "still-readable|8",
    };

    /**
     * How many checks got through before something threw.
     *
     * <p>It exists because a bare 9000 says "it threw" and nothing else. With the count, the failure
     * names the line, and that turned a bug in the asynchronous overlap check from a hunt into one
     * run.
     */
    static int done;

    /** Records one answer and counts it. See {@link #done}. */
    static void add(java.util.List<String> a, String s) {
        a.add(s);
        done++;
    }

    /** What locking does, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final Path tmp = Files.createTempFile("lck1", ".bin");
        try {
            Files.write(tmp, new byte[64]);
            final FileChannel c =
                    FileChannel.open(tmp, StandardOpenOption.READ, StandardOpenOption.WRITE);

            final FileLock whole = c.lock();
            add(a, "whole|" + whole.position() + "|" + whole.size() + "|" + whole.isShared()
                    + "|" + whole.isValid() + "|" + (whole.channel() == c)
                    + "|" + (whole.acquiredBy() == c));
            add(a, "overlaps|" + whole.overlaps(0, 10) + "|" + whole.overlaps(100, 10));
            add(a, "second|" + attempt(new Lock(c, 0, 10, false)));
            add(a, "second-try|" + attempt(new TryLock(c, 0, 10, false)));
            whole.release();
            add(a, "released|" + whole.isValid() + "|" + attempt(new Release(whole)));

            // Two locks side by side do not overlap, so both are granted.
            final FileLock left = c.tryLock(0, 8, false);
            final FileLock right = c.tryLock(8, 8, false);
            add(a, "side-by-side|" + (left != null) + "|" + (right != null)
                    + "|" + left.position() + "|" + left.size() + "|" + right.position());
            left.release();
            right.release();

            // Shared with shared is still an overlap for the JDK.
            final FileLock s1 = c.tryLock(0, 8, true);
            add(a, "shared|" + (s1 != null) + "|" + s1.isShared());
            add(a, "shared-twice|" + attempt(new TryLock(c, 0, 8, true)));
            s1.release();

            add(a, "bad-args|" + attempt(new TryLock(c, -1, 8, false))
                    + "|" + attempt(new TryLock(c, 0, -8, false)));

            // A region past the end of the file is fine: a lock reserves what may still be written.
            final FileLock beyond = c.tryLock(1000, 10, false);
            add(a, "past-the-end|" + (beyond != null));
            beyond.release();

            // Closing the channel invalidates whatever it still held.
            final FileLock alive = c.lock(0, 4, false);
            c.close();
            add(a, "closing|" + alive.isValid() + "|" + c.isOpen());
            add(a, "lock-when-closed|" + attempt(new Lock(c, 0, 4, false)));

            // A read-only channel cannot take an exclusive lock.
            final FileChannel ro = FileChannel.open(tmp, StandardOpenOption.READ);
            add(a, "read-only-exclusive|" + attempt(new TryLock(ro, 0, 4, false)));
            final FileLock rs = ro.tryLock(0, 4, true);
            add(a, "read-only-shared|" + (rs != null));
            rs.release();
            ro.close();

            // The asynchronous side, over the same file.
            final AsynchronousFileChannel af = AsynchronousFileChannel.open(tmp,
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
            final FileLock afl = af.lock().get(10, TimeUnit.SECONDS);
            add(a, "async-lock|" + afl.position() + "|" + afl.size() + "|" + afl.isShared()
                    + "|" + afl.isValid() + "|" + (afl.channel() == null)
                    + "|" + (afl.acquiredBy() == af));
            add(a, "async-second|" + attempt(new AsyncLock(af, 0, 10, false)));
            afl.release();

            final FileLock atl = af.tryLock(0, 16, false);
            add(a, "async-try|" + (atl != null) + "|" + atl.position() + "|" + atl.size());
            atl.release();

            // By handler, which runs on the pool and not on this thread.
            final Box box = new Box();
            af.lock(0L, 8L, false, box, new Record(box));
            add(a, "async-handler|" + box.await());
            af.close();
            add(a, "async-closed|" + af.isOpen());

            // The lock really did protect the bytes: after all of that the file still reads back.
            final FileChannel again = FileChannel.open(tmp, StandardOpenOption.READ);
            final ByteBuffer b = ByteBuffer.allocate(8);
            add(a, "still-readable|" + again.read(b, 0L));
            again.close();
        } finally {
            Files.deleteIfExists(tmp);
        }
        return a.toArray(new String[a.size()]);
    }

    /** Where the handler leaves what it was told. */
    static class Box {
        private String value;

        synchronized void put(String v) {
            this.value = v;
            notifyAll();
        }

        synchronized String await() throws InterruptedException {
            long left = 10000L;
            while (this.value == null && left > 0) {
                final long before = System.currentTimeMillis();
                wait(left);
                left = left - (System.currentTimeMillis() - before);
            }
            return this.value == null ? "no answer" : this.value;
        }
    }

    /** A handler that records what it was handed, and releases the lock. */
    static class Record implements java.nio.channels.CompletionHandler<FileLock, Box> {
        private final Box box;

        Record(Box box) {
            this.box = box;
        }

        public void completed(FileLock l, Box attachment) {
            String s = "ok:" + l.position() + ":" + l.size() + ":" + (attachment == this.box);
            try {
                l.release();
            } catch (IOException e) {
                s = s + ":release-failed";
            }
            this.box.put(s);
        }

        public void failed(Throwable t, Box attachment) {
            final String n = t.getClass().getName();
            this.box.put("failed:" + n.substring(n.lastIndexOf('.') + 1));
        }
    }

    /** Something run to see how it fails. */
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

    static class Lock implements Throwing {
        private final FileChannel c;
        private final long position;
        private final long size;
        private final boolean shared;

        Lock(FileChannel c, long position, long size, boolean shared) {
            this.c = c;
            this.position = position;
            this.size = size;
            this.shared = shared;
        }

        public void run() throws Exception {
            c.lock(this.position, this.size, this.shared);
        }
    }

    static class TryLock implements Throwing {
        private final FileChannel c;
        private final long position;
        private final long size;
        private final boolean shared;

        TryLock(FileChannel c, long position, long size, boolean shared) {
            this.c = c;
            this.position = position;
            this.size = size;
            this.shared = shared;
        }

        public void run() throws Exception {
            c.tryLock(this.position, this.size, this.shared);
        }
    }

    static class AsyncLock implements Throwing {
        private final AsynchronousFileChannel c;
        private final long position;
        private final long size;
        private final boolean shared;

        AsyncLock(AsynchronousFileChannel c, long position, long size, boolean shared) {
            this.c = c;
            this.position = position;
            this.size = size;
            this.shared = shared;
        }

        public void run() throws Exception {
            c.lock(this.position, this.size, this.shared);
        }
    }

    static class Release implements Throwing {
        private final FileLock l;

        Release(FileLock l) {
            this.l = l;
        }

        public void run() throws Exception {
            l.release();
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
