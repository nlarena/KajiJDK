package java.nio.channels;

import java.nio.channels.spi.AbstractSelectionKey;

// One registration of a channel with a selector.
//
// A key is the only thing that ties the two together, and it is deliberately the *only* thing: a
// channel does not know its selectors and a selector does not reach into its channels. Everything
// either side needs about the other -- what is being watched for, what turned out to be ready -- is
// in here.
//
// `interestOps` is read by the selector on every turn and written by the program from any thread, so
// it is volatile. `readyOps` is written only by the selector, during a select, and read afterwards.
//
// Package-private on purpose: keys are made by `KajiSelector.register` and by nothing else.
final class KajiSelectionKey extends AbstractSelectionKey {

    private final SelectableChannel channel;
    private final KajiSelector selector;
    private volatile int interestOps;
    private int readyOps;

    KajiSelectionKey(SelectableChannel channel, KajiSelector selector, int interestOps) {
        this.channel = channel;
        this.selector = selector;
        this.interestOps = interestOps;
    }

    @Override
    public SelectableChannel channel() {
        return this.channel;
    }

    @Override
    public Selector selector() {
        return this.selector;
    }

    @Override
    public int interestOps() {
        ensureValid();
        return this.interestOps;
    }

    @Override
    public SelectionKey interestOps(int ops) {
        ensureValid();
        if ((ops & ~this.channel.validOps()) != 0) {
            throw new IllegalArgumentException("operation not supported by this channel");
        }
        this.interestOps = ops;
        return this;
    }

    @Override
    public int readyOps() {
        ensureValid();
        return this.readyOps;
    }

    /** What the last select found. Only the selector writes it. */
    void readyOps(int ops) {
        this.readyOps = ops;
    }

    /** The same without the validity check, for the selector's own bookkeeping. */
    int rawInterestOps() {
        return this.interestOps;
    }

    private void ensureValid() {
        if (!this.isValid()) {
            throw new CancelledKeyException();
        }
    }
}
