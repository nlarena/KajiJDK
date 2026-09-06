import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Checks the nine asynchronous statics of {@code java.nio.channels} against JDK 25.
 *
 * <h2>What is compared</h2>
 *
 * <p>That the groups get built, shut down in both their forms and wait; that an asynchronous file
 * channel really reads and writes through both routes --{@code Future} and
 * {@code CompletionHandler}-- and that its read sees what its write left; and that an asynchronous
 * server and client connect over {@code localhost} and pass bytes to each other.
 *
 * <p>What is not compared is timing or parallelism, nor when a group left with operations pending
 * terminates: there the two implementations wait for different things --the JDK for the system, this
 * one for the pool-- and comparing that would be comparing the waiting model and not the API.
 * Underneath this library there are blocking channels on a thread pool --like the JDK on the
 * platforms with no asynchronous I/O of their own-- so the answers are the same but the cost is not.
 * That is documented in the classes, not measured here.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class ASY1 {

    static final String[] EXPECTED = {
        "fixed|true|false|false|true",
        "cached|true|false",
        "pool|true|false",
        "same-provider|true|true",
        "shutdown|true|true|true",
        "shutdown-now|true|true",
        "bad-args|IllegalArgumentException|NullPointerException|NullPointerException|NullPointerException",
        "write|8|8",
        "read|8|0a141e28323c4650",
        "partial-read|3|3c4650",
        "handler|ok:4:true|1e28323c",
        "past-the-end|-1",
        "negative-position|IllegalArgumentException",
        "truncate|4",
        "closed|false",
        "server|true|true",
        "connected|true|true",
        "sent|5",
        "received|5|0102030405",
        "two-reads|ReadPendingException",
        "two-accepts|AcceptPendingException",
        "already-connected|AlreadyConnectedException",
        "both-closed|false|false",
        "clean-group|true|true",
        "grupo-shutdown|ShutdownChannelGroupException",
    };

    /** What the nine statics do, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        // The three groups.
        final AsynchronousChannelGroup g1 =
                AsynchronousChannelGroup.withFixedThreadPool(2, Executors.defaultThreadFactory());
        a.add("fixed|" + (g1 != null) + "|" + g1.isShutdown() + "|" + g1.isTerminated()
                + "|" + (g1.provider() != null));
        final ExecutorService p2 = Executors.newCachedThreadPool();
        final AsynchronousChannelGroup g2 = AsynchronousChannelGroup.withCachedThreadPool(p2, 1);
        a.add("cached|" + (g2 != null) + "|" + g2.isShutdown());
        final ExecutorService p3 = Executors.newFixedThreadPool(2);
        final AsynchronousChannelGroup g3 = AsynchronousChannelGroup.withThreadPool(p3);
        a.add("pool|" + (g3 != null) + "|" + g3.isShutdown());
        a.add("same-provider|" + (g1.provider() == g2.provider())
                + "|" + (g2.provider() == g3.provider()));

        // An empty group shuts down and terminates.
        g1.shutdown();
        a.add("shutdown|" + g1.isShutdown() + "|" + g1.awaitTermination(5, TimeUnit.SECONDS)
                + "|" + g1.isTerminated());
        g2.shutdownNow();
        a.add("shutdown-now|" + g2.isShutdown() + "|" + g2.awaitTermination(5, TimeUnit.SECONDS));

        // The arguments that are no good.
        a.add("bad-args|" + attempt(new FixedZero()) + "|" + attempt(new FixedNoFactory())
                + "|" + attempt(new NullPool()) + "|" + attempt(new NullCache()));

        // An asynchronous file channel.
        final Path tmp = Files.createTempFile("asy1", ".bin");
        try {
            final AsynchronousFileChannel af = AsynchronousFileChannel.open(tmp,
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
            final byte[] data = {10, 20, 30, 40, 50, 60, 70, 80};
            final Future<Integer> fw = af.write(ByteBuffer.wrap(data), 0L);
            a.add("write|" + fw.get(10, TimeUnit.SECONDS) + "|" + af.size());
            final ByteBuffer readBuf = ByteBuffer.allocate(8);
            final Future<Integer> fr = af.read(readBuf, 0L);
            a.add("read|" + fr.get(10, TimeUnit.SECONDS) + "|" + hex(readBuf.array()));
            final ByteBuffer partial = ByteBuffer.allocate(3);
            a.add("partial-read|" + af.read(partial, 5L).get(10, TimeUnit.SECONDS)
                    + "|" + hex(partial.array()));

            // Through a handler, which runs on the pool and not on this thread.
            final Box box = new Box();
            final ByteBuffer other = ByteBuffer.allocate(4);
            af.read(other, 2L, box, new Note(box));
            a.add("handler|" + box.await() + "|" + hex(other.array()));

            a.add("past-the-end|"
                    + af.read(ByteBuffer.allocate(4), 100L).get(10, TimeUnit.SECONDS));
            a.add("negative-position|" + attempt(new NegativePosition(af)));
            a.add("truncate|" + af.truncate(4).size());
            af.force(true);
            af.close();
            a.add("closed|" + af.isOpen());
        } finally {
            Files.deleteIfExists(tmp);
        }

        // An asynchronous server and client talking to each other.
        final AsynchronousChannelGroup g4 =
                AsynchronousChannelGroup.withFixedThreadPool(4, Executors.defaultThreadFactory());
        final AsynchronousServerSocketChannel srv = AsynchronousServerSocketChannel.open(g4);
        srv.bind(new InetSocketAddress("127.0.0.1", 0));
        final int port = ((InetSocketAddress) srv.getLocalAddress()).getPort();
        a.add("server|" + (port > 0) + "|" + srv.isOpen());

        final Future<AsynchronousSocketChannel> incoming = srv.accept();
        final AsynchronousSocketChannel cli = AsynchronousSocketChannel.open(g4);
        cli.connect(new InetSocketAddress("127.0.0.1", port)).get(10, TimeUnit.SECONDS);
        final AsynchronousSocketChannel serverSide = incoming.get(10, TimeUnit.SECONDS);
        a.add("connected|" + (serverSide != null) + "|" + cli.isOpen());

        final byte[] message = {1, 2, 3, 4, 5};
        a.add("sent|" + cli.write(ByteBuffer.wrap(message)).get(10, TimeUnit.SECONDS));
        final ByteBuffer received = ByteBuffer.allocate(5);
        int total = 0;
        while (total < 5) {
            final int n = serverSide.read(received).get(10, TimeUnit.SECONDS);
            if (n < 0) {
                break;
            }
            total = total + n;
        }
        a.add("received|" + total + "|" + hex(received.array()));
        a.add("two-reads|" + attempt(new TwoReads(serverSide)));
        a.add("two-accepts|" + attempt(new TwoAccepts(srv)));
        a.add("already-connected|" + attempt(new AlreadyConnected(cli, port)));

        cli.close();
        serverSide.close();
        srv.close();
        a.add("both-closed|" + cli.isOpen() + "|" + srv.isOpen());
        g4.shutdown();

        // A group whose only channel was closed terminates. `g4` is not checked, having been left
        // with the two operations `two-reads` and `two-accepts` asked for: there the two
        // implementations differ in how they wait --the JDK through the system, this one through the
        // pool-- and comparing that would be comparing the waiting model and not the API.
        final AsynchronousChannelGroup g6 =
                AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory());
        final AsynchronousSocketChannel loose = AsynchronousSocketChannel.open(g6);
        loose.close();
        g6.shutdown();
        a.add("clean-group|" + g6.awaitTermination(10, TimeUnit.SECONDS)
                + "|" + g6.isTerminated());

        // A channel opened in an already shut down group is not accepted.
        final AsynchronousChannelGroup g5 =
                AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory());
        g5.shutdown();
        a.add("grupo-shutdown|" + attempt(new OpenInShutdown(g5)));

        p3.shutdown();
        return a.toArray(new String[a.size()]);
    }

    /** The bytes in hexadecimal. */
    static String hex(byte[] b) {
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            final int v = b[i] & 0xff;
            s.append("0123456789abcdef".charAt(v >> 4));
            s.append("0123456789abcdef".charAt(v & 0xf));
        }
        return s.toString();
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
            return this.value == null ? "no notice" : this.value;
        }
    }

    /** A handler that writes down what it was handed. */
    static class Note implements CompletionHandler<Integer, Box> {
        private final Box box;

        Note(Box box) {
            this.box = box;
        }

        public void completed(Integer result, Box attachment) {
            this.box.put("ok:" + result + ":" + (attachment == this.box));
        }

        public void failed(Throwable t, Box attachment) {
            final String n = t.getClass().getName();
            this.box.put("failed:" + n.substring(n.lastIndexOf('.') + 1));
        }
    }

    /** Something run to see what it fails with. */
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

    static class FixedZero implements Throwing {
        public void run() throws Exception {
            AsynchronousChannelGroup.withFixedThreadPool(0, Executors.defaultThreadFactory());
        }
    }

    static class FixedNoFactory implements Throwing {
        public void run() throws Exception {
            AsynchronousChannelGroup.withFixedThreadPool(1, null);
        }
    }

    static class NullPool implements Throwing {
        public void run() throws Exception {
            AsynchronousChannelGroup.withThreadPool(null);
        }
    }

    static class NullCache implements Throwing {
        public void run() throws Exception {
            AsynchronousChannelGroup.withCachedThreadPool(null, 1);
        }
    }

    static class NegativePosition implements Throwing {
        private final AsynchronousFileChannel af;

        NegativePosition(AsynchronousFileChannel af) {
            this.af = af;
        }

        public void run() throws Exception {
            af.read(ByteBuffer.allocate(4), -1L);
        }
    }

    static class TwoReads implements Throwing {
        private final AsynchronousSocketChannel c;

        TwoReads(AsynchronousSocketChannel c) {
            this.c = c;
        }

        public void run() throws Exception {
            c.read(ByteBuffer.allocate(4));
            c.read(ByteBuffer.allocate(4));
        }
    }

    static class TwoAccepts implements Throwing {
        private final AsynchronousServerSocketChannel s;

        TwoAccepts(AsynchronousServerSocketChannel s) {
            this.s = s;
        }

        public void run() throws Exception {
            s.accept();
            s.accept();
        }
    }

    static class AlreadyConnected implements Throwing {
        private final AsynchronousSocketChannel c;
        private final int port;

        AlreadyConnected(AsynchronousSocketChannel c, int port) {
            this.c = c;
            this.port = port;
        }

        public void run() throws Exception {
            c.connect(new InetSocketAddress("127.0.0.1", this.port));
        }
    }

    static class OpenInShutdown implements Throwing {
        private final AsynchronousChannelGroup g;

        OpenInShutdown(AsynchronousChannelGroup g) {
            this.g = g;
        }

        public void run() throws Exception {
            AsynchronousSocketChannel.open(this.g);
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
            return 9000;
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
