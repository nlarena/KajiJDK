package java.lang.invoke;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

/**
 * KajiLibrary's java.lang.invoke.LayoutMethodHandle -- the {@link MethodHandle}s that
 * {@link java.lang.foreign.MemoryLayout} builds.
 *
 * <p>There are three, and all three compute the same thing wearing different faces: **an address**.
 * {@link java.lang.foreign.MemoryLayout#scaleHandle} scales an index by the layout's size;
 * `byteOffsetHandle` walks a path down and adds; `sliceHandle` does the same and slices the segment
 * as well.
 *
 * <p>They exist as an object and not as a loose call for the same reason a `VarHandle` does: the
 * computation is **kept**, and can be passed around and composed without repeating the path at each
 * place.
 *
 * <h2>Why a subclass with typed helpers</h2>
 *
 * <p>`MethodHandle.invoke`/`invokeExact` are signature polymorphic: the site's descriptor is the
 * real one, and the VM intercepts them. What it intercepts needs something concrete to call, and
 * that is {@link #aplicarLong} and {@link #aplicarSegmento} -- one per **return shape**, which is
 * the only thing the call site says. Which of the three computations to do is decided by
 * {@link #mode}, which is a datum of the object and not of the site.
 *
 * <p>Those two helper names stay in Spanish for the same reason as `SegmentVarHandle`'s: the VM
 * names them as string literals in `invokevirtual.rs`, so they move when the Rust side moves.
 *
 * <p>The `MethodHandle` intrinsic the VM already had was no good: it reads off the instance the
 * fields of a **direct** handle (`owner`/`name`/`descriptor`/`kind`), and these point at no method
 * -- they carry a layout and a path inside.
 */
final class LayoutMethodHandle extends MethodHandle {

    /** `scaleHandle()`: `(base, index) -> base + index * byteSize()`. */
    static final int SCALE = 0;
    /** `byteOffsetHandle(path)`: `(base, indices...) -> base + offset + sum(i*stride)`. */
    static final int OFFSET = 1;
    /** `sliceHandle(path)`: the above, and it slices as well. */
    static final int SLICE = 2;

    private final int mode;
    private final MemoryLayout root;
    // The layout **the path arrives at**: what the slice measures. Equal to `root` for SCALE.
    private final MemoryLayout target;
    private final long fixedOffset;
    private final long[] strides;

    LayoutMethodHandle(MethodType type, int mode, MemoryLayout root, MemoryLayout target,
            long fixedOffset, long[] strides) {
        super(type);
        this.mode = mode;
        this.root = root;
        this.target = target;
        this.fixedOffset = fixedOffset;
        this.strides = strides == null ? new long[0] : strides;
    }

    /**
     * The address, which is what {@link #SCALE} and {@link #OFFSET} return.
     *
     * <p>It demands as many indices as the path has open steps, for the same reason as in a
     * `VarHandle`: one short would silently give another element's address.
     */
    long applyLong(long base, long[] indices) {
        int n = indices == null ? 0 : indices.length;
        if (this.mode == SCALE) {
            if (n != 1) {
                throw new IllegalArgumentException("scaleHandle takes one index, not " + n);
            }
            return this.root.scale(base, indices[0]);
        }
        return this.total(base, indices);
    }

    /** The sliced segment, which is what {@link #SLICE} returns. */
    MemorySegment applySegment(Object segment, long base, long[] indices) {
        long t = this.total(base, indices);
        return ((MemorySegment) segment).asSlice(t, this.target.byteSize());
    }

    private long total(long base, long[] indices) {
        int n = indices == null ? 0 : indices.length;
        if (n != this.strides.length) {
            throw new IllegalArgumentException(
                    "this handle takes " + this.strides.length + " indices, not " + n);
        }
        long t = base + this.fixedOffset;
        int i = 0;
        while (i < n) {
            t = t + indices[i] * this.strides[i];
            i = i + 1;
        }
        return t;
    }
}
