package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.nio.channels.spi.SelectorProvider;

// A pipe: two channels joined, one that writes and one that reads.
//
// Underneath it is a loopback socket pair -- see `LoopbackPair` for why that, and not a queue in
// memory. Each end wraps its own `SocketChannel` and shuts the direction it does not use, so the
// pipe is one-way in fact and not only by contract.
//
// Package-private on purpose: `Pipe.open()` is the way in.
final class KajiPipe extends Pipe {

    private final Source source;
    private final Sink sink;

    KajiPipe(SelectorProvider provider) throws IOException {
        final SocketChannel[] pair = LoopbackPair.open();
        // The accepted end reads and the connecting end writes; which is which does not matter,
        // only that each one gives up the direction it will not use.
        pair[0].shutdownOutput();
        pair[1].shutdownInput();
        this.source = new Source(provider, pair[0]);
        this.sink = new Sink(provider, pair[1]);
    }

    @Override
    public SourceChannel source() {
        return this.source;
    }

    @Override
    public SinkChannel sink() {
        return this.sink;
    }

    /** The reading end. */
    static final class Source extends Pipe.SourceChannel {

        private final SocketChannel inner;

        Source(SelectorProvider provider, SocketChannel inner) {
            super(provider);
            this.inner = inner;
        }

        /** The socket underneath, which is what the selector polls. */
        SocketChannel inner() {
            return this.inner;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return this.inner.read(dst);
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return this.inner.read(dsts, offset, length);
        }

        @Override
        public long read(ByteBuffer[] dsts) throws IOException {
            return this.inner.read(dsts, 0, dsts.length);
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            this.inner.close();
        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            this.inner.configureBlocking(block);
        }
    }

    /** The writing end. */
    static final class Sink extends Pipe.SinkChannel {

        private final SocketChannel inner;

        Sink(SelectorProvider provider, SocketChannel inner) {
            super(provider);
            this.inner = inner;
        }

        /** The socket underneath, which is what the selector polls. */
        SocketChannel inner() {
            return this.inner;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return this.inner.write(src);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return this.inner.write(srcs, offset, length);
        }

        @Override
        public long write(ByteBuffer[] srcs) throws IOException {
            return this.inner.write(srcs, 0, srcs.length);
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            this.inner.close();
        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            this.inner.configureBlocking(block);
        }
    }
}
