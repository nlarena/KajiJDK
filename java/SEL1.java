import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Checks {@code Selector} and {@code Pipe} against JDK 25.
 *
 * <h2>What is compared</h2>
 *
 * <p>A whole accept-read-write loop driven by a selector: a server registered for accept, a client
 * connecting, the connection registered for read, bytes crossing, and every key reporting what it
 * should at each step. Plus the three sets and how a cancellation moves between them, what a pipe
 * carries, and that {@code wakeup} gets a parked {@code select()} out.
 *
 * <p>What is not compared is how long anything takes. The answers are the same; the readiness comes
 * from the same system call on both sides.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class SEL1 {

    static final String[] EXPECTED = {
        "open|true|true|true",
        "empty|0|0|0",
        "blocking|IllegalBlockingModeException",
        "registered|1|true|true|true|16|0",
        "keyFor|true|true",
        "bad-ops|IllegalArgumentException",
        "quiet|0|0",
        "accepting|1|1",
        "ready-key|true|true|false|false",
        "drained|0",
        "accepted|true",
        "two-keys|2",
        "nothing-yet|0|0",
        "readable|1",
        "read|3|789",
        "writable|1|true",
        "cancelled|false|CancelledKeyException",
        "after-cancel|1|false",
        "closing-channels|0",
        "pipe|true|true|1|4",
        "pipe-quiet|0",
        "pipe-ready|1|true",
        "pipe-byte|42",
        "wakeup|0|true",
        "wakeup-ahead|true",
        "closed|false|ClosedSelectorException|ClosedSelectorException",
    };

    /** How many checks got through before something threw. The same trick as in LCK1: a bare 9000 says "it threw" and nothing else. */
    static int done;

    /** Records one answer and counts it. */
    static void add(java.util.List<String> a, String s) {
        a.add(s);
        done++;
    }

    /** What the selector does, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        final Selector sel = Selector.open();
        add(a, "open|" + (sel != null) + "|" + sel.isOpen() + "|" + (sel.provider() != null));
        add(a, "empty|" + sel.keys().size() + "|" + sel.selectedKeys().size()
                + "|" + sel.selectNow());

        // A blocking channel cannot be registered: that is the JDK's rule and the reason the
        // non-blocking mode exists.
        final ServerSocketChannel srv = ServerSocketChannel.open();
        srv.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        add(a, "blocking|" + attempt(new Register(srv, sel, SelectionKey.OP_ACCEPT)));
        srv.configureBlocking(false);

        final SelectionKey sk = srv.register(sel, SelectionKey.OP_ACCEPT);
        add(a, "registered|" + sel.keys().size() + "|" + sk.isValid()
                + "|" + (sk.channel() == srv) + "|" + (sk.selector() == sel)
                + "|" + sk.interestOps() + "|" + sk.readyOps());
        add(a, "keyFor|" + (srv.keyFor(sel) == sk) + "|" + srv.isRegistered());
        add(a, "bad-ops|" + attempt(new Interest(sk, SelectionKey.OP_READ)));

        // Nothing is knocking yet.
        add(a, "quiet|" + sel.selectNow() + "|" + sel.selectedKeys().size());

        // A client connects: the server becomes acceptable.
        final int port = ((InetSocketAddress) srv.getLocalAddress()).getPort();
        final SocketChannel client = SocketChannel.open();
        client.configureBlocking(false);
        client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        add(a, "accepting|" + waitFor(sel, 5000) + "|" + sel.selectedKeys().size());
        final Iterator<SelectionKey> it = sel.selectedKeys().iterator();
        final SelectionKey ready = it.next();
        add(a, "ready-key|" + (ready == sk) + "|" + ready.isAcceptable()
                + "|" + ready.isReadable() + "|" + ready.isWritable());
        it.remove();
        add(a, "drained|" + sel.selectedKeys().size());

        final SocketChannel served = srv.accept();
        add(a, "accepted|" + (served != null));
        served.configureBlocking(false);
        final SelectionKey rk = served.register(sel, SelectionKey.OP_READ);
        add(a, "two-keys|" + sel.keys().size());

        // Nothing has been sent, so nothing is readable.
        add(a, "nothing-yet|" + sel.selectNow() + "|" + sel.selectedKeys().size());

        // The client finishes connecting and sends.
        client.finishConnect();
        client.write(ByteBuffer.wrap(new byte[] {7, 8, 9}));
        add(a, "readable|" + waitFor(sel, 5000));
        final ByteBuffer buf = ByteBuffer.allocate(8);
        add(a, "read|" + served.read(buf) + "|" + buf.get(0) + buf.get(1) + buf.get(2));
        sel.selectedKeys().clear();

        // Writable is almost always true: the send buffer is empty.
        rk.interestOps(SelectionKey.OP_WRITE);
        add(a, "writable|" + waitFor(sel, 5000) + "|" + rk.isWritable());
        sel.selectedKeys().clear();

        // Cancelling takes the key out of every set, on the next select.
        rk.cancel();
        add(a, "cancelled|" + rk.isValid() + "|" + attempt(new ReadOps(rk)));
        sel.selectNow();
        add(a, "after-cancel|" + sel.keys().size() + "|" + served.isRegistered());

        client.close();
        served.close();
        srv.close();
        sel.selectNow();
        add(a, "closing-channels|" + sel.keys().size());

        // A pipe carries bytes and its source is selectable.
        final Pipe pipe = Pipe.open();
        add(a, "pipe|" + (pipe.source() != null) + "|" + (pipe.sink() != null)
                + "|" + pipe.source().validOps() + "|" + pipe.sink().validOps());
        pipe.source().configureBlocking(false);
        final SelectionKey pk = pipe.source().register(sel, SelectionKey.OP_READ);
        add(a, "pipe-quiet|" + sel.selectNow());
        pipe.sink().write(ByteBuffer.wrap(new byte[] {42}));
        add(a, "pipe-ready|" + waitFor(sel, 5000) + "|" + pk.isReadable());
        final ByteBuffer one = ByteBuffer.allocate(1);
        pipe.source().read(one);
        add(a, "pipe-byte|" + one.get(0));
        sel.selectedKeys().clear();
        pk.cancel();
        pipe.source().close();
        pipe.sink().close();
        sel.selectNow();

        // `wakeup` gets a parked select out.
        final Waker w = new Waker(sel);
        final Thread t = new Thread(w);
        t.start();
        final long before = System.currentTimeMillis();
        final int woke = sel.select(30000);
        final long spent = System.currentTimeMillis() - before;
        t.join(5000);
        add(a, "wakeup|" + woke + "|" + (spent < 20000));

        // A wakeup with nobody waiting makes the next select return at once.
        sel.wakeup();
        final long before2 = System.currentTimeMillis();
        sel.select(30000);
        add(a, "wakeup-ahead|" + (System.currentTimeMillis() - before2 < 20000));

        sel.close();
        add(a, "closed|" + sel.isOpen() + "|" + attempt(new Keys(sel))
                + "|" + attempt(new SelectNow(sel)));
        return a.toArray(new String[a.size()]);
    }

    /** Selects until something turns up or the deadline passes; returns what select returned. */
    static int waitFor(Selector sel, long millis) throws IOException {
        final long deadline = System.currentTimeMillis() + millis;
        while (true) {
            final int n = sel.selectNow();
            if (n > 0 || !sel.selectedKeys().isEmpty()) {
                return n > 0 ? n : sel.selectedKeys().size();
            }
            if (System.currentTimeMillis() > deadline) {
                return -1;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
    }

    /** Wakes the selector up after a moment, from another thread. */
    static class Waker implements Runnable {
        private final Selector sel;

        Waker(Selector sel) {
            this.sel = sel;
        }

        public void run() {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.sel.wakeup();
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

    static class Register implements Throwing {
        private final ServerSocketChannel c;
        private final Selector s;
        private final int ops;

        Register(ServerSocketChannel c, Selector s, int ops) {
            this.c = c;
            this.s = s;
            this.ops = ops;
        }

        public void run() throws Exception {
            c.register(this.s, this.ops);
        }
    }

    static class Interest implements Throwing {
        private final SelectionKey k;
        private final int ops;

        Interest(SelectionKey k, int ops) {
            this.k = k;
            this.ops = ops;
        }

        public void run() throws Exception {
            k.interestOps(this.ops);
        }
    }

    static class ReadOps implements Throwing {
        private final SelectionKey k;

        ReadOps(SelectionKey k) {
            this.k = k;
        }

        public void run() throws Exception {
            k.readyOps();
        }
    }

    static class Keys implements Throwing {
        private final Selector s;

        Keys(Selector s) {
            this.s = s;
        }

        public void run() throws Exception {
            s.keys();
        }
    }

    static class SelectNow implements Throwing {
        private final Selector s;

        SelectNow(Selector s) {
            this.s = s;
        }

        public void run() throws Exception {
            s.selectNow();
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
