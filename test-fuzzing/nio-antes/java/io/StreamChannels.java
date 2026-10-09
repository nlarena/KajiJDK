package java.io;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;

/**
 * The channels {@link FileInputStream#getChannel} and {@link FileOutputStream#getChannel} return.
 *
 * <p>Package-private and with no usable name: nobody should be writing it. They are here and not in
 * `java.nio.channels` because the only thing setting them apart from any other channel is that they
 * **share the position with their stream**, and that position lives in the stream.
 *
 * <h2>The only thing to understand about these two classes</h2>
 *
 * <p>{@code getChannel()}'s contract does not say "a channel over the same file": it says the
 * channel's position and the stream's are **the same number**. Reading from the stream moves the
 * channel; moving the channel changes where the stream reads from. It is the part that goes wrong
 * by itself if each keeps its own count, because nothing fails: one simply reads from the wrong
 * place, and the caller receives perfectly believable bytes.
 *
 * <p>That is why neither of the two keeps a position of its own. There is a single number, and it
 * is in the stream.
 */
final class StreamChannels {

    private StreamChannels() {
    }

    /**
     * A {@link FileInputStream}'s channel.
     *
     * <p>It reads from **the same snapshot** as the stream, not from the disk. It is the direct
     * consequence of the stream reading the whole file on construction (see its header): if this
     * channel went to the disk for the contents, the two would share the position but not what is
     * at it, and a {@code read} through the channel could return something different from the same
     * {@code read} through the stream. Sharing the position and not the contents would be worse
     * than sharing nothing.
     *
     * <p>It is read-only, like the JDK's: everything that would write raises
     * {@link NonWritableChannelException}.
     */
    static final class ForInput extends FileChannel {

        private final FileInputStream owner;

        ForInput(FileInputStream owner) {
            this.owner = owner;
        }

        private byte[] requireOpen() throws IOException {
            if (this.owner.closed || !this.isOpen()) {
                throw new ClosedChannelException();
            }
            return this.owner.data;
        }

        public int read(ByteBuffer dst) throws IOException {
            byte[] data = this.requireOpen();
            int n = readInto(data, this.owner.pos, dst);
            if (n > 0) {
                this.owner.pos = this.owner.pos + n;
            }
            return n;
        }

        public int read(ByteBuffer dst, long position) throws IOException {
            if (position < 0) {
                throw new IllegalArgumentException("pos negativa");
            }
            return readInto(this.requireOpen(), position, dst);
        }

        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            checkRange(dsts, offset, length);
            this.requireOpen();
            long total = 0;
            int i = offset;
            while (i < offset + length) {
                int n = this.read(dsts[i]);
                if (n < 0) {
                    // End of file: if something had been read already, the total counts; if not,
                    // -1.
                    return total > 0 ? total : -1L;
                }
                total = total + n;
                if (dsts[i].hasRemaining()) {
                    return total;
                }
                i = i + 1;
            }
            return total;
        }

        public int write(ByteBuffer src) throws IOException {
            throw new NonWritableChannelException();
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new NonWritableChannelException();
        }

        public int write(ByteBuffer src, long position) throws IOException {
            throw new NonWritableChannelException();
        }

        public long position() throws IOException {
            this.requireOpen();
            return this.owner.pos;
        }

        public FileChannel position(long newPosition) throws IOException {
            if (newPosition < 0) {
                throw new IllegalArgumentException("pos negativa");
            }
            this.requireOpen();
            // Going past the end is legal and is no error: what follows is that the reads give -1.
            // That is why it is not clamped -- clamping would make `position()` not return what was
            // set.
            this.owner.pos = newPosition;
            return this;
        }

        public long size() throws IOException {
            return this.requireOpen().length;
        }

        public FileChannel truncate(long size) throws IOException {
            throw new NonWritableChannelException();
        }

        /**
         * There is no file to map: this channel is over the bytes a stream already read.
         *
         * <p>Answering with a buffer over those bytes would look right and be wrong -- a mapping is
         * a window onto the file, and writes through it are meant to reach it. This one would reach
         * nothing.
         */
        public java.nio.MappedByteBuffer map(MapMode mode, long position, long size)
                throws IOException {
            this.requireOpen();
            throw new UnsupportedOperationException("a stream channel has no file to map");
        }

        /** There is no file to lock, for the same reason {@link #map} has none to map. */
        public FileLock lock(long position, long size, boolean shared) throws IOException {
            this.requireOpen();
            throw new UnsupportedOperationException("a stream channel has no file to lock");
        }

        /** @see #lock */
        public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            this.requireOpen();
            throw new UnsupportedOperationException("a stream channel has no file to lock");
        }

        public void force(boolean metaData) throws IOException {
            // There is nothing in flight to force: this channel does not write.
            this.requireOpen();
        }

        public long transferTo(long position, long count, WritableByteChannel target)
                throws IOException {
            if (position < 0 || count < 0) {
                throw new IllegalArgumentException("pos o cuenta negativa");
            }
            if (target == null) {
                throw new NullPointerException();
            }
            byte[] data = this.requireOpen();
            if (!target.isOpen()) {
                throw new ClosedChannelException();
            }
            if (position >= data.length) {
                return 0L;
            }
            int n = (int) Math.min(count, (long) data.length - position);
            ByteBuffer bb = ByteBuffer.wrap(data, (int) position, n);
            long written = 0;
            while (bb.hasRemaining()) {
                int w = target.write(bb);
                if (w <= 0) {
                    break;
                }
                written = written + w;
            }
            return written;
        }

        public long transferFrom(ReadableByteChannel src, long position, long count)
                throws IOException {
            throw new NonWritableChannelException();
        }

        protected void implCloseChannel() throws IOException {
            // Closing the channel closes the stream, as in the JDK: they are the same thing seen
            // two ways, and leaving one open would suggest one could still read through it.
            this.owner.closed = true;
        }
    }

    /**
     * A {@link FileOutputStream}'s channel.
     *
     * <p>The stream gathers what is written in a buffer and dumps it in batches; the channel writes
     * at a position. For the two to be the same number, **every operation of this channel first
     * empties the stream's buffer**, and from the moment the channel exists the stream's dumping
     * goes through it. That way the position is a single one --the underlying channel's-- and there
     * is nothing to keep in step.
     *
     * <p>The price of entering that mode is in {@link java.nio.channels.FileChannel}: writing
     * through a channel is read-modify-write of the whole file, whereas the stream's normal dumping
     * is an append at the end. That is why the mode **is not turned on until somebody asks for the
     * channel**: whoever never calls {@code getChannel()} writes as before.
     */
    static final class ForOutput extends FileChannel {

        private final FileOutputStream owner;
        /** The real channel over the file. Lazy: opening it may fail and `getChannel` does not
         * throw. */
        private FileChannel underlying;

        ForOutput(FileOutputStream owner) {
            this.owner = owner;
        }

        private FileChannel underlying() throws IOException {
            if (this.owner.closed || !this.isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.owner.path == null) {
                // A stream constructed with a `FileDescriptor`. This VM does not model them, so
                // there is no file behind it and there is nothing this channel can do. That is
                // said, not simulated.
                throw new IOException("no file descriptor");
            }
            if (this.underlying == null) {
                this.underlying = FileChannel.open(new File(this.owner.path).toPath(),
                        StandardOpenOption.WRITE);
                // It starts where what has been written so far ends, which is what the contract
                // asks for and also the only thing compatible with `append` mode.
                this.underlying.position(this.underlying.size());
            }
            return this.underlying;
        }

        /**
         * It sends the channel whatever the stream has pending in its buffer.
         *
         * <p>It is what makes the position a single one: after this, what the stream wrote is
         * already counted in the channel's position, and there are no bytes living in two places.
         */
        void flushPending() throws IOException {
            FileChannel c = this.underlying();
            if (this.owner.used == 0) {
                return;
            }
            ByteBuffer bb = ByteBuffer.wrap(this.owner.buf, 0, this.owner.used);
            while (bb.hasRemaining()) {
                if (c.write(bb) <= 0) {
                    throw new IOException("Could not write to " + this.owner.path);
                }
            }
            this.owner.used = 0;
        }

        public int read(ByteBuffer dst) throws IOException {
            throw new NonReadableChannelException();
        }

        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            throw new NonReadableChannelException();
        }

        public int read(ByteBuffer dst, long position) throws IOException {
            throw new NonReadableChannelException();
        }

        public int write(ByteBuffer src) throws IOException {
            this.flushPending();
            return this.underlying().write(src);
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            checkRange(srcs, offset, length);
            this.flushPending();
            return this.underlying().write(srcs, offset, length);
        }

        public int write(ByteBuffer src, long position) throws IOException {
            this.flushPending();
            return this.underlying().write(src, position);
        }

        public long position() throws IOException {
            this.flushPending();
            return this.underlying().position();
        }

        public FileChannel position(long newPosition) throws IOException {
            this.flushPending();
            this.underlying().position(newPosition);
            return this;
        }

        public long size() throws IOException {
            this.flushPending();
            return this.underlying().size();
        }

        public FileChannel truncate(long size) throws IOException {
            this.flushPending();
            this.underlying().truncate(size);
            return this;
        }

        // These three go to the real channel underneath, unlike the read side --which has no file
        // at all--. The `flushPending` first is the same rule as everywhere else here: what the
        // stream still holds in its buffer is not in the file yet, and a mapping or a lock taken
        // before flushing would be looking at a file that is missing the last writes.

        public java.nio.MappedByteBuffer map(MapMode mode, long position, long size)
                throws IOException {
            this.flushPending();
            return this.underlying().map(mode, position, size);
        }

        public FileLock lock(long position, long size, boolean shared) throws IOException {
            this.flushPending();
            return this.underlying().lock(position, size, shared);
        }

        public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            this.flushPending();
            return this.underlying().tryLock(position, size, shared);
        }

        public void force(boolean metaData) throws IOException {
            this.flushPending();
            this.underlying().force(metaData);
        }

        public long transferTo(long position, long count, WritableByteChannel target)
                throws IOException {
            throw new NonReadableChannelException();
        }

        public long transferFrom(ReadableByteChannel src, long position, long count)
                throws IOException {
            this.flushPending();
            return this.underlying().transferFrom(src, position, count);
        }

        protected void implCloseChannel() throws IOException {
            // What is pending goes out **before** closing: losing it on the close would be the
            // worst way of losing it, because closing is precisely what one does to make sure it
            // went out.
            if (this.owner.path != null && !this.owner.closed) {
                this.flushPending();
            }
            if (this.underlying != null) {
                this.underlying.close();
            }
            this.owner.closed = true;
        }
    }

    private static void checkRange(ByteBuffer[] bufs, int offset, int length) {
        if (bufs == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > bufs.length - length) {
            throw new IndexOutOfBoundsException();
        }
    }

    /** It copies into `dst` whatever is in `data` from `from` on. -1 if `from` is already past the
     * end. */
    private static int readInto(byte[] data, long from, ByteBuffer dst) {
        if (dst == null) {
            throw new NullPointerException();
        }
        if (dst.isReadOnly()) {
            throw new java.nio.ReadOnlyBufferException();
        }
        if (from >= data.length) {
            // End of file is -1 even if the buffer was full; there being no room left is another
            // thing and goes below, as zero.
            return -1;
        }
        int room = dst.remaining();
        if (room == 0) {
            return 0;
        }
        int n = (int) Math.min((long) room, (long) data.length - from);
        dst.put(data, (int) from, n);
        return n;
    }
}
