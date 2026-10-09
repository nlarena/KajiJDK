package jdk.internal.classfile.impl;

import java.lang.classfile.Label;

// A label that already knows its position: it comes from reading a `.class`, where the targets are
// offsets and not unknowns. It is compared by identity, as the contract of `Label` demands.
public final class LabelImpl implements Label {

    private final int bci;

    public LabelImpl(int bci) {
        this.bci = bci;
    }

    /** The offset within the `code` array. */
    public int bci() {
        return this.bci;
    }

    public String toString() {
        return "Label@" + this.bci;
    }
}
