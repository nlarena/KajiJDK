package jdk.jshell.execution;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The one that hands out what arrives over a shared channel; see {@link MultiplexingOutputStream}.
 *
 * <h2>How it works</h2>
 *
 * <p>It is a thread that reads block after block, looks at each one's name and writes its data into
 * the matching stream. A block whose name is not in the map is dropped: it may come from a stream the
 * far end opened and this one does not know about, and cutting the connection for that would also
 * lose the streams that are understood.
 *
 * <h2>The close</h2>
 *
 * <p>When the channel ends, everything it was asked to close is closed. That is what makes the side
 * waiting in a {@code read} learn that nothing more is coming, instead of hanging forever.
 */
final class DemultiplexInput extends Thread {

    private final DataInputStream source;
    private final Map<String, OutputStream> targets;
    private final Iterable<? extends Closeable> onClose;

    DemultiplexInput(InputStream source, Map<String, OutputStream> targets,
            Iterable<? extends Closeable> onClose) {
        super("output reader");
        this.source = new DataInputStream(source);
        this.targets = targets;
        this.onClose = onClose;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                final int nameLength = source.read();
                if (nameLength == -1) {
                    break;
                }
                final byte[] name = new byte[nameLength];
                source.readFully(name);
                final int dataLength = source.read();
                if (dataLength == -1) {
                    break;
                }
                final byte[] data = new byte[dataLength];
                source.readFully(data);
                final OutputStream target =
                        targets.get(new String(name, StandardCharsets.UTF_8));
                if (target != null) {
                    target.write(data);
                }
            }
        } catch (IOException e) {
            // The channel was cut. It is the only normal way for this loop to end.
        } finally {
            for (final Closeable c : onClose) {
                try {
                    c.close();
                } catch (IOException e) {
                    // Everything is closing already; one refusing changes nothing.
                }
            }
        }
    }
}
