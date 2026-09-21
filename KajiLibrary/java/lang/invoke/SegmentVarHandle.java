package java.lang.invoke;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * KajiLibrary's java.lang.invoke.SegmentVarHandle -- the {@link VarHandle} that
 * {@link java.lang.foreign.MemoryLayout#varHandle} builds.
 *
 * <h2>Why this class exists instead of some methods on `VarHandle`</h2>
 *
 * <p>`VarHandle`'s 31 accessors are `public final native`: they cannot be overridden and their body
 * is put in by the VM at linking time. That is no whim of the JDK's -- they are **signature
 * polymorphic** (JLS §15.12.3), meaning one single `(Object...)` declaration serves call sites with
 * different descriptors, and no Java body can have that shape.
 *
 * <p>So the VM has to intercept them, and what it intercepts needs something concrete to call. That
 * is the `leerXxx`/`escribirXxx` methods below: one per **carrier**, with a fixed descriptor the VM
 * can name without inventing anything. The mapping is direct -- `get` returning `I` goes to
 * `leerInt`, `set` whose last parameter is `J` goes to `escribirLong`-- and the indices of the path's
 * open steps travel in a `long[]` the VM builds out of the leftover arguments. An array and not one
 * arity per index count: that is 18 methods instead of seventy-odd, and the VM already knows how to
 * build arrays.
 *
 * <p><strong>Those helper names stay in Spanish, and that is not an oversight.</strong>
 * `invokevirtual.rs` builds them as strings --`format!("leer{carrier}")`-- so the name in this file
 * and the name in the VM have to match character for character. Renaming them here alone would
 * break the bridge with nothing to catch it: the class still compiles, and the failure only shows up
 * at run time as an `UnsatisfiedLinkError` on a call site that used to work. They move when the Rust
 * side moves, in the same change.
 *
 * <h2>What it holds</h2>
 *
 * <p>A layout `VarHandle` is a **deferred address**: the value's layout, how far to move from the
 * start of the root layout, and how much each open step measures. The final address is
 *
 * <pre>  caller's displacement + the path's fixed displacement + sum(indices[i] * strides[i])</pre>
 *
 * and the access is done by {@link MemorySegment#get}, which already checks bounds, alignment and
 * read-only. None of that is reimplemented here.
 *
 * <h2>The memory modes</h2>
 *
 * <p>The **plain**, `Opaque`, `Acquire`, `Release` and `Volatile` modes of reading and writing are
 * served, and all five do the same thing. In an interpreter with a single thread per access that is
 * correct and not a simplification: there is no reordering to prevent and no cache to synchronise, so
 * the strongest guarantee is met by doing nothing. The **read-modify-write** modes
 * --`compareAndSet`, `getAndAdd` and company-- are **not** served: those do need real atomicity, and
 * faking it would be exactly the kind of lie this project does not allow. A site using them falls
 * into the bridgeless `native` and gets an `UnsatisfiedLinkError`.
 */
final class SegmentVarHandle extends VarHandle {

    // The **value**'s layout, already walked down the path. It is a `ValueLayout` or an
    // `AddressLayout`; the cast in each helper is what picks `MemorySegment.get`'s overload.
    private final java.lang.foreign.MemoryLayout layout;
    // How far the path moves from the start of the root layout. Fixed: it comes from
    // `byteOffset`.
    private final long fixedOffset;
    // How much each **open** step of the path measures, in bytes. Empty if the path was closed.
    private final long[] strides;

    SegmentVarHandle(java.lang.foreign.MemoryLayout layout, long fixedOffset,
            long[] strides) {
        this.layout = layout;
        this.fixedOffset = fixedOffset;
        this.strides = strides == null ? new long[0] : strides;
    }

    // The two behaviour switches. This handle does not carry the exact/inexact flag --nothing here
    // reads it-- so there is no sibling handle to produce. Returning `this` would be the lie: it
    // says the switch happened and it did not, and the caller finds out at the first call that was
    // supposed to be checked differently. `hasInvokeExactBehavior` in the base class refuses for the
    // same reason.

    public VarHandle withInvokeExactBehavior() {
        throw new UnsupportedOperationException("no VarHandle without VM support");
    }

    public VarHandle withInvokeBehavior() {
        throw new UnsupportedOperationException("no VarHandle without VM support");
    }

    /**
     * The final address.
     *
     * <p>It demands **exactly** as many indices as the path has open steps. One index short would
     * silently give an access to another element; one too many is a caller who believes they are
     * accessing something this handle does not designate.
     */
    private long total(long displacement, long[] indices) {
        int n = indices == null ? 0 : indices.length;
        if (n != this.strides.length) {
            throw new IllegalArgumentException(
                    "this VarHandle takes " + this.strides.length + " indices, not " + n);
        }
        long t = displacement + this.fixedOffset;
        int i = 0;
        while (i < n) {
            t = t + indices[i] * this.strides[i];
            i = i + 1;
        }
        return t;
    }

    // ---- the helpers the VM names --------------------------------------------------------------------
    //
    // Package-private and not `public`: they are the VM's entry point, not API. That they are not
    // private is deliberate -- a `private` would force the VM to skip access control, and the
    // mechanism being visible from `java.lang.invoke` beats it being invisible and magical.
    //
    // Their NAMES are fixed by the Rust side; see the class's note before renaming any of them.

    /**
     * A read whose value is **discarded**: `vh.get(seg, off);` as a statement.
     *
     * <p>It exists because the return of a signature-polymorphic call is fixed by the context (JLS
     * §15.12.3), and in a statement that context is `void`. The JDK does the read all the same, and
     * it has to be done: the bounds, alignment and index-count checks are **effects** of accessing,
     * and skipping them would turn an invalid access into a line that does nothing.
     *
     * <p>The cast goes by the layout's class because the carrier is not known from the call site:
     * there the descriptor says `V`, which does not tell an `int` from a `double`.
     */
    void readAndDiscard(Object segment, long displacement, long[] indices) {
        MemorySegment m = (MemorySegment) segment;
        long t = this.total(displacement, indices);
        if (this.layout instanceof ValueLayout.OfInt) {
            m.get((ValueLayout.OfInt) this.layout, t);
        } else if (this.layout instanceof ValueLayout.OfLong) {
            m.get((ValueLayout.OfLong) this.layout, t);
        } else if (this.layout instanceof ValueLayout.OfDouble) {
            m.get((ValueLayout.OfDouble) this.layout, t);
        } else if (this.layout instanceof ValueLayout.OfFloat) {
            m.get((ValueLayout.OfFloat) this.layout, t);
        } else if (this.layout instanceof ValueLayout.OfBoolean) {
            m.get((ValueLayout.OfBoolean) this.layout, t);
        } else if (this.layout instanceof ValueLayout.OfByte) {
            m.get((ValueLayout.OfByte) this.layout, t);
        } else if (this.layout instanceof ValueLayout.OfChar) {
            m.get((ValueLayout.OfChar) this.layout, t);
        } else if (this.layout instanceof ValueLayout.OfShort) {
            m.get((ValueLayout.OfShort) this.layout, t);
        } else {
            m.get((AddressLayout) this.layout, t);
        }
    }

    boolean readBoolean(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfBoolean) this.layout, this.total(displacement, indices));
    }

    void writeBoolean(Object segment, long displacement, long[] indices, boolean value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfBoolean) this.layout, this.total(displacement, indices), value);
    }

    byte readByte(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfByte) this.layout, this.total(displacement, indices));
    }

    void writeByte(Object segment, long displacement, long[] indices, byte value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfByte) this.layout, this.total(displacement, indices), value);
    }

    char readChar(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfChar) this.layout, this.total(displacement, indices));
    }

    void writeChar(Object segment, long displacement, long[] indices, char value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfChar) this.layout, this.total(displacement, indices), value);
    }

    short readShort(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfShort) this.layout, this.total(displacement, indices));
    }

    void writeShort(Object segment, long displacement, long[] indices, short value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfShort) this.layout, this.total(displacement, indices), value);
    }

    int readInt(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfInt) this.layout, this.total(displacement, indices));
    }

    void writeInt(Object segment, long displacement, long[] indices, int value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfInt) this.layout, this.total(displacement, indices), value);
    }

    long readLong(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfLong) this.layout, this.total(displacement, indices));
    }

    void writeLong(Object segment, long displacement, long[] indices, long value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfLong) this.layout, this.total(displacement, indices), value);
    }

    float readFloat(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfFloat) this.layout, this.total(displacement, indices));
    }

    void writeFloat(Object segment, long displacement, long[] indices, float value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfFloat) this.layout, this.total(displacement, indices), value);
    }

    double readDouble(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (ValueLayout.OfDouble) this.layout, this.total(displacement, indices));
    }

    void writeDouble(Object segment, long displacement, long[] indices, double value) {
        ((MemorySegment) segment).set(
                (ValueLayout.OfDouble) this.layout, this.total(displacement, indices), value);
    }

    MemorySegment readRef(Object segment, long displacement, long[] indices) {
        return ((MemorySegment) segment).get(
                (AddressLayout) this.layout, this.total(displacement, indices));
    }

    void writeRef(Object segment, long displacement, long[] indices, MemorySegment value) {
        ((MemorySegment) segment).set(
                (AddressLayout) this.layout, this.total(displacement, indices), value);
    }
}
