package jdk.jshell.execution;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A stream that labels what it writes, so a channel can be shared.
 *
 * <h2>What for</h2>
 *
 * <p>Between JShell and the process that runs the code there is a single connection, and several
 * different streams have to travel through it: the user program's output, its error output, and --in
 * the other direction-- its input. Each one is wrapped in one of these with its name, and on the
 * other side {@link DemultiplexInput} hands them out.
 *
 * <h2>The format</h2>
 *
 * <p>Each block is: one byte with the name's length, the name, one byte with the data's length, and
 * the data. The lengths fit in a byte, so a block never carries more than 127 bytes of data and a
 * long write is split into several. The name is repeated in every block: it costs a few bytes and in
 * exchange the channel has no state, which is what allows two streams to be interleaved without
 * coordinating them.
 *
 * <p>Writing the block is synchronized on the stream underneath. Without that, two streams writing
 * at once would interleave their half-built blocks and the reader could not tell them apart.
 */
final class MultiplexingOutputStream extends OutputStream {

    /** The largest a data block can be: the length goes in a single byte. */
    private static final int MAX = 127;

    private final byte[] name;
    private final OutputStream target;

    MultiplexingOutputStream(String name, OutputStream target) {
        this.name = name.getBytes(StandardCharsets.UTF_8);
        this.target = target;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] {(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int i = 0;
        while (i < len) {
            final int howMuch = Math.min(len - i, MAX);
            final byte[] block = new byte[name.length + howMuch + 2];
            block[0] = (byte) name.length;
            System.arraycopy(name, 0, block, 1, name.length);
            block[name.length + 1] = (byte) howMuch;
            System.arraycopy(b, off + i, block, name.length + 2, howMuch);
            synchronized (target) {
                target.write(block);
            }
            i += howMuch;
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (target) {
            target.flush();
        }
    }
}
