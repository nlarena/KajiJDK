package java.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.Channels — the bridge between `java.io` and `java.nio.channels`.
 *
 * <p>It exists because the two hierarchies were never unified: half the library speaks
 * `InputStream` and the other half speaks `ReadableByteChannel`, and without this bridge everybody
 * would copy the same loop from `byte[]` to `ByteBuffer` by hand. Everything here is pure
 * adaptation --it opens nothing, it creates no resources-- and that is why it is **implemented
 * whole and for real** without a single native: it receives something that works already and gives
 * it back with the other face on.
 *
 * <h2>The only thing to know before using it</h2>
 *
 * <p>The adapters **do not buffer**: a read over the `InputStream` that wraps a channel is a read
 * over the channel. It is on purpose --putting a buffer in would change how many bytes the one
 * underneath asks for and when, and over a channel that shows-- but it means that reading one byte
 * at a time from one of these is as expensive as reading one byte at a time from the channel.
 * Wrapping in `BufferedInputStream` is the answer, and it is the caller's decision, not this
 * class's.
 *
 * <p>Closing the adapter **closes what was adapted**. It is what the JDK does and what has to be
 * done: the adapter has no resources of its own, so if its `close()` did not close the one
 * underneath it would close absolutely nothing and would be a silent trap.
 *
 * <h2>Exceptions: the same old twist</h2>
 *
 * <p>This library's `java.io.InputStream.read()` **does not declare `IOException`** (see the note
 * of `java.io.IOException`), but {@link ReadableByteChannel#read} does. Going from channel to
 * stream the exception has no way out, so it comes out wrapped in {@link UncheckedIOException}: the
 * reason is not lost, it changes type. In the other direction --from stream to channel-- there is
 * no problem, because narrowing what is declared is always legal.
 *
 * <h2>What was left out</h2>
 *
 * <p><strong>Nothing.</strong> The twelve public methods of JDK 25 are here. The two that take an
 * {@link AsynchronousByteChannel} as well, and they never depended on this package being able to
 * **make** asynchronous channels: they make none, they adapt the one they are given. It can make
 * them now —see {@link AsynchronousFileChannel#open}—, and wrapping one works just the same.
 */
public final class Channels {

    // No instances: it is a drawer of statics.
    private Channels() {
        throw new AssertionError("do not instantiate");
    }

    // ---- from channel to stream ------------------------------------------------------------------

    /**
     * An {@link InputStream} that reads from `ch`.
     *
     * <p>With no buffer; see the note of the class.
     */
    public static InputStream newInputStream(ReadableByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new ChannelInputStream(ch);
    }

    /** An {@link OutputStream} that writes into `ch`. With no buffer; see the note of the class. */
    public static OutputStream newOutputStream(WritableByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new ChannelOutputStream(ch);
    }

    /**
     * An {@link InputStream} that reads from an asynchronous channel, **blocking**.
     *
     * <p>That the source is asynchronous does not change that an `InputStream` is synchronous: each
     * `read` launches the operation and waits for its result. It serves for passing an asynchronous
     * channel to code that only knows about streams, not for gaining concurrency.
     */
    public static InputStream newInputStream(AsynchronousByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new AsyncChannelInputStream(ch);
    }

    /**
     * An {@link OutputStream} that writes into an asynchronous channel, blocking on each `write`.
     */
    public static OutputStream newOutputStream(AsynchronousByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new AsyncChannelOutputStream(ch);
    }

    // ---- from stream to channel ------------------------------------------------------------------

    /**
     * A {@link ReadableByteChannel} that reads from `in`.
     *
     * <p>The channel that comes out **is neither interruptible nor selectable**, and not by
     * omission: an `InputStream` offers no way of aborting a started read, so promising {@link
     * InterruptibleChannel} would be promising something the one underneath cannot give.
     */
    public static ReadableByteChannel newChannel(InputStream in) {
        if (in == null) {
            throw new NullPointerException();
        }
        return new StreamReadableChannel(in);
    }

    /**
     * A {@link WritableByteChannel} that writes into `out`. The same caveats as the reading one.
     */
    public static WritableByteChannel newChannel(OutputStream out) {
        if (out == null) {
            throw new NullPointerException();
        }
        return new StreamWritableChannel(out);
    }

    // ---- from channel to text --------------------------------------------------------------------

    /**
     * A {@link Reader} that decodes what comes out of `ch` with `dec`.
     *
     * <p>`minBufferCap` is a **suggested floor** for the internal buffer, not an exact size and not
     * a guarantee; `-1` asks for the one the implementation prefers. Here it is accepted and
     * ignored: the buffer is chosen by `InputStreamReader`, and pretending to respect it would
     * change nothing except the belief of whoever reads the code.
     */
    public static Reader newReader(ReadableByteChannel ch, CharsetDecoder dec, int minBufferCap) {
        if (ch == null || dec == null) {
            throw new NullPointerException();
        }
        return new InputStreamReader(newInputStream(ch), dec);
    }

    /** Like the other one, with the named character set. */
    public static Reader newReader(ReadableByteChannel ch, String csName) {
        if (csName == null) {
            throw new NullPointerException();
        }
        return newReader(ch, Charset.forName(csName));
    }

    /**
     * Like the other one, with `charset`.
     *
     * <p>Malformed bytes are **replaced**, they do not make the reading fail: it is what the
     * default decoder does and what the JDK specifies for this form.
     */
    public static Reader newReader(ReadableByteChannel ch, Charset charset) {
        if (ch == null || charset == null) {
            throw new NullPointerException();
        }
        return new InputStreamReader(newInputStream(ch), charset);
    }

    /** A {@link Writer} that encodes with `enc` towards `ch`. `minBufferCap`, as in `newReader`. */
    public static Writer newWriter(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) {
        if (ch == null || enc == null) {
            throw new NullPointerException();
        }
        return new OutputStreamWriter(newOutputStream(ch), enc);
    }

    /** Like the other one, with the named character set. */
    public static Writer newWriter(WritableByteChannel ch, String csName) {
        if (csName == null) {
            throw new NullPointerException();
        }
        return newWriter(ch, Charset.forName(csName));
    }

    /** Like the other one, with `cs`. */
    public static Writer newWriter(WritableByteChannel ch, Charset cs) {
        if (ch == null || cs == null) {
            throw new NullPointerException();
        }
        return new OutputStreamWriter(newOutputStream(ch), cs);
    }

    // ---- the adapters ----------------------------------------------------------------------------

    // A stream over a channel. `read(byte[],int,int)` is the one that does the work and `read()`
    // leans on it: the other way round --one by one-- each byte would cost a call to the channel.
    private static final class ChannelInputStream extends InputStream {

        private final ReadableByteChannel channel;
        private final byte[] one = new byte[1];

        ChannelInputStream(ReadableByteChannel channel) {
            this.channel = channel;
        }

        public int read() {
            int n = this.read(this.one, 0, 1);
            if (n <= 0) {
                return -1;
            }
            return this.one[0] & 0xff;
        }

        public int read(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            try {
                return this.channel.read(ByteBuffer.wrap(b, off, len));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public int available() {
            // A channel cannot say how much it has ready, and `0` is the honest answer: the
            // contract of `available` is "how many can be read without blocking", and of that there
            // is no datum here.
            return 0;
        }

        public void close() throws java.io.IOException {
            this.channel.close();
        }
    }

    private static final class ChannelOutputStream extends OutputStream {

        private final WritableByteChannel channel;
        private final byte[] one = new byte[1];

        ChannelOutputStream(WritableByteChannel channel) {
            this.channel = channel;
        }

        public void write(int b) {
            this.one[0] = (byte) b;
            this.write(this.one, 0, 1);
        }

        public void write(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            ByteBuffer bb = ByteBuffer.wrap(b, off, len);
            try {
                // The loop is not over-defensive: a channel can write less than it is given, and
                // `OutputStream.write` promises that it writes everything. Without the loop, that
                // promise is false exactly in the rare case, which is the worst place for it to be.
                while (bb.hasRemaining()) {
                    int n = this.channel.write(bb);
                    if (n <= 0 && bb.hasRemaining()) {
                        throw new IOException("the channel accepts no more bytes");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void close() throws java.io.IOException {
            this.channel.close();
        }
    }

    // A channel over a stream. The buffer's array is taken advantage of when it has one: without
    // that there would be two copies --stream to temporary, temporary to buffer-- for nothing.
    private static final class StreamReadableChannel implements ReadableByteChannel {

        private final InputStream source;
        private boolean openFlag = true;

        StreamReadableChannel(InputStream source) {
            this.source = source;
        }

        public int read(ByteBuffer dst) throws IOException {
            if (!this.openFlag) {
                throw new ClosedChannelException();
            }
            int room = dst.remaining();
            if (room == 0) {
                return 0;
            }
            if (dst.hasArray() && !dst.isReadOnly()) {
                int base = dst.arrayOffset() + dst.position();
                int n = this.source.read(dst.array(), base, room);
                if (n > 0) {
                    dst.position(dst.position() + n);
                }
                return n;
            }
            byte[] tmp = new byte[room];
            int n = this.source.read(tmp, 0, room);
            if (n > 0) {
                dst.put(tmp, 0, n);
            }
            return n;
        }

        public boolean isOpen() {
            return this.openFlag;
        }

        public void close() throws java.io.IOException {
            if (this.openFlag) {
                this.openFlag = false;
                this.source.close();
            }
        }
    }

    private static final class StreamWritableChannel implements WritableByteChannel {

        private final OutputStream sink;
        private boolean openFlag = true;

        StreamWritableChannel(OutputStream sink) {
            this.sink = sink;
        }

        public int write(ByteBuffer src) throws IOException {
            if (!this.openFlag) {
                throw new ClosedChannelException();
            }
            int n = src.remaining();
            if (n == 0) {
                return 0;
            }
            if (src.hasArray()) {
                int base = src.arrayOffset() + src.position();
                this.sink.write(src.array(), base, n);
            } else {
                byte[] tmp = new byte[n];
                src.get(src.position(), tmp, 0, n);
                this.sink.write(tmp, 0, n);
            }
            // An `OutputStream` writes everything or throws, so getting here means the `n` went in:
            // the position advances whole and there is no partial write to report.
            src.position(src.position() + n);
            return n;
        }

        public boolean isOpen() {
            return this.openFlag;
        }

        public void close() throws java.io.IOException {
            if (this.openFlag) {
                this.openFlag = false;
                this.sink.close();
            }
        }
    }

    // The two asynchronous ones share the way of waiting, which is the delicate part: an
    // `ExecutionException` hides the real cause and returning it as it is would lose the reason.
    private static int await(Future<Integer> f) throws IOException {
        try {
            Integer n = f.get();
            if (n == null) {
                return -1;
            }
            return n.intValue();
        } catch (InterruptedException e) {
            // The mark is put back before leaving: swallowing an interruption leaves the thread
            // believing it was never interrupted, and whoever decides what to do about that is
            // further up.
            Thread.currentThread().interrupt();
            throw new ClosedByInterruptException();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IOException(cause);
        }
    }

    private static final class AsyncChannelInputStream extends InputStream {

        private final AsynchronousByteChannel channel;
        private final byte[] one = new byte[1];

        AsyncChannelInputStream(AsynchronousByteChannel channel) {
            this.channel = channel;
        }

        public int read() {
            int n = this.read(this.one, 0, 1);
            if (n <= 0) {
                return -1;
            }
            return this.one[0] & 0xff;
        }

        public int read(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            try {
                return await(this.channel.read(ByteBuffer.wrap(b, off, len)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void close() throws java.io.IOException {
            this.channel.close();
        }
    }

    private static final class AsyncChannelOutputStream extends OutputStream {

        private final AsynchronousByteChannel channel;
        private final byte[] one = new byte[1];

        AsyncChannelOutputStream(AsynchronousByteChannel channel) {
            this.channel = channel;
        }

        public void write(int b) {
            this.one[0] = (byte) b;
            this.write(this.one, 0, 1);
        }

        public void write(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            ByteBuffer bb = ByteBuffer.wrap(b, off, len);
            try {
                while (bb.hasRemaining()) {
                    int n = await(this.channel.write(bb));
                    if (n <= 0 && bb.hasRemaining()) {
                        throw new IOException("the channel accepts no more bytes");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void close() throws java.io.IOException {
            this.channel.close();
        }
    }
}
