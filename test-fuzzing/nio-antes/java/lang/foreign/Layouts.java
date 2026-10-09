package java.lang.foreign;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// The layout factory and the path walker. Package-private: it is where the shared validations live,
// so the fifteen implementations do not each repeat them their own way -- which is how they end up
// diverging.
final class Layouts {

    private Layouts() {
    }

    // ---- shared validations ----------------------------------------------------------------------

    static String requireName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("the name cannot be null");
        }
        return name;
    }

    // An alignment has to be a positive power of two. The check is the usual trick: a number is a
    // power of two if it has exactly one bit set, and `n & (n-1)` clears the lowest bit.
    static long requireAlignment(long alignment) {
        if (alignment <= 0L || (alignment & (alignment - 1L)) != 0L) {
            throw new IllegalArgumentException(
                    "the alignment has to be a positive power of two: " + alignment);
        }
        return alignment;
    }

    static ByteOrder requireOrder(ByteOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("the order cannot be null");
        }
        return order;
    }

    static void appendName(StringBuilder sb, String name) {
        if (name != null) {
            sb.append('(');
            sb.append(name);
            sb.append(')');
        }
    }

    // ---- ValueLayout's constants ------------------------------------------------------------------
    //
    // The default order is **fixed** little-endian, and not the machine's. The JDK uses
    // `ByteOrder.nativeOrder()`; here there is no way of asking the VM, and choosing wrong would be
    // worse than choosing fixed -- every platform this runs on today is little-endian, and whoever
    // needs the other one says so with `withOrder`.

    static ValueLayout.OfBoolean ofBoolean() {
        return new ValueBoolean(1L, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfByte ofByte() {
        return new ValueByte(1L, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfChar ofChar(long alignment) {
        return new ValueChar(alignment, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfShort ofShort(long alignment) {
        return new ValueShort(alignment, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfInt ofInt(long alignment) {
        return new ValueInt(alignment, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfLong ofLong(long alignment) {
        return new ValueLong(alignment, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfFloat ofFloat(long alignment) {
        return new ValueFloat(alignment, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfDouble ofDouble(long alignment) {
        return new ValueDouble(alignment, null, ByteOrder.LITTLE_ENDIAN);
    }

    static AddressLayout address(long alignment) {
        return new ValueAddress(alignment, null, ByteOrder.LITTLE_ENDIAN, null);
    }

    // ---- the four composite factories -------------------------------------------------------------

    static PaddingLayout padding(long size) {
        if (size <= 0L) {
            throw new IllegalArgumentException("padding has to take up something: " + size);
        }
        return new Padding(size, 1L, null);
    }

    static SequenceLayout sequence(long count, MemoryLayout element) {
        if (element == null) {
            throw new IllegalArgumentException("the element cannot be null");
        }
        if (count < 0L) {
            throw new IllegalArgumentException("negative count: " + count);
        }
        long sz = element.byteSize();
        // Overflow is checked **before** building: a sequence whose size does not fit in a `long`
        // is not an invalid layout to be discovered later, it is one that does not exist.
        if (sz != 0L && count > Long.MAX_VALUE / sz) {
            throw new IllegalArgumentException("the total size does not fit in a long");
        }
        return new Sequence(count, element, element.byteAlignment(), null);
    }

    static StructLayout struct(MemoryLayout... elements0) {
        List<MemoryLayout> ms = asList(elements0);
        long alignment = 1L;
        long offset = 0L;
        int i = 0;
        while (i < ms.size()) {
            MemoryLayout m = ms.get(i);
            long a = m.byteAlignment();
            // The struct's rule, and the most surprising one: each member has to fall at an offset
            // that is a multiple of **its** alignment. It does not arrange itself -- padding goes in
            // explicitly, and without it the construction fails. See `StructLayout`'s note.
            if (offset % a != 0L) {
                throw new IllegalArgumentException(
                        "member " + m + " falls at offset " + offset
                                + ", which is not a multiple of its alignment " + a
                                + "; add MemoryLayout.paddingLayout(" + (a - offset % a) + ")");
            }
            if (a > alignment) {
                alignment = a;
            }
            offset = offset + m.byteSize();
            i = i + 1;
        }
        return new Struct(ms, alignment, null);
    }

    static UnionLayout union(MemoryLayout... elements0) {
        List<MemoryLayout> ms = asList(elements0);
        long alignment = 1L;
        int i = 0;
        while (i < ms.size()) {
            long a = ms.get(i).byteAlignment();
            if (a > alignment) {
                alignment = a;
            }
            i = i + 1;
        }
        // There is no offset rule to check: they all start at zero, and with the maximum alignment
        // they all fall right by construction.
        return new Union(ms, alignment, null);
    }

    private static List<MemoryLayout> asList(MemoryLayout[] elements0) {
        if (elements0 == null) {
            throw new IllegalArgumentException("the members cannot be null");
        }
        List<MemoryLayout> ms = new ArrayList<MemoryLayout>();
        int i = 0;
        while (i < elements0.length) {
            if (elements0[i] == null) {
                throw new IllegalArgumentException("a member is null");
            }
            ms.add(elements0[i]);
            i = i + 1;
        }
        return ms;
    }

    // ---- a path's steps ----------------------------------------------------------------------------

    static final int BY_NAME = 0;
    static final int BY_POSITION = 1;
    static final int ELEMENT = 2;
    static final int ALL = 3;
    static final int RANGE = 4;
    static final int DEREFERENCE = 5;

    static MemoryLayout.PathElement byName(String name) {
        return new Step(BY_NAME, requireName(name), 0L, 0L);
    }

    static MemoryLayout.PathElement byPosition(long index) {
        return new Step(BY_POSITION, null, index, 0L);
    }

    static MemoryLayout.PathElement element(long index) {
        return new Step(ELEMENT, null, index, 0L);
    }

    static MemoryLayout.PathElement allElements() {
        return new Step(ALL, null, 0L, 0L);
    }

    static MemoryLayout.PathElement elements0(long from, long step) {
        return new Step(RANGE, null, from, step);
    }

    static MemoryLayout.PathElement dereference() {
        return new Step(DEREFERENCE, null, 0L, 0L);
    }

    // ---- walking a path ----------------------------------------------------------------------------
    //
    // The two walks --the one adding offsets up and the one returning the layout-- share the same
    // stroll and differ only in what they take away. They are written together so they cannot fall
    // out of step: a `select` going down differently from how `byteOffset` goes down would give a
    // layout that is not where the offset says.

    static long offsetByPath(MemoryLayout root, MemoryLayout.PathElement... path) {
        return walk(root, path, true).offset;
    }

    static MemoryLayout selectByPath(MemoryLayout root, MemoryLayout.PathElement... path) {
        return walk(root, path, false).layout;
    }

    /**
     * That path's {@link java.lang.invoke.VarHandle}.
     *
     * <p>It is the third walk, and the only one accepting **open** steps: where `byteOffset` stops
     * short --a step over all the elements designates no position-- this one notes how much the
     * element measures and carries on. That number is the **stride** the index the caller gives on
     * access is later multiplied by, and it is exactly what the open step exists for.
     *
     * @param arrayStride an extra stride at the **front**, for `arrayElementVarHandle`; zero if not
     */
    static java.lang.invoke.VarHandle pathHandle(MemoryLayout root, long arrayStride,
            MemoryLayout.PathElement... path) {
        java.util.ArrayList<Long> open = new java.util.ArrayList<Long>();
        if (arrayStride > 0L) {
            open.add(Long.valueOf(arrayStride));
        }
        Stop end = walk(root, path, true, open);
        long[] steps = new long[open.size()];
        int i = 0;
        while (i < steps.length) {
            steps[i] = open.get(i).longValue();
            i = i + 1;
        }
        return java.lang.invoke.VarHandles.ofSegment(end.layout, end.offset, steps);
    }

    /** `MemoryLayout`'s three `MethodHandle`s, which share the same walk. */
    static java.lang.invoke.MethodHandle offsetHandle(MemoryLayout root,
            MemoryLayout.PathElement... path) {
        java.util.ArrayList<Long> open = new java.util.ArrayList<Long>();
        Stop end = walk(root, path, true, open);
        return java.lang.invoke.VarHandles.pathOffsetHandle(root, end.layout, end.offset,
                toLongs(open));
    }

    static java.lang.invoke.MethodHandle sliceHandle0(MemoryLayout root,
            MemoryLayout.PathElement... path) {
        java.util.ArrayList<Long> open = new java.util.ArrayList<Long>();
        Stop end = walk(root, path, true, open);
        return java.lang.invoke.VarHandles.pathSliceHandle(root, end.layout, end.offset,
                toLongs(open));
    }

    private static long[] toLongs(java.util.List<Long> xs) {
        long[] out = new long[xs.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = xs.get(i).longValue();
            i = i + 1;
        }
        return out;
    }

    private static Stop walk(MemoryLayout root, MemoryLayout.PathElement[] path,
            boolean wantOffset) {
        return walk(root, path, wantOffset, null);
    }

    // A non-null `open` = a `VarHandle` is being built, and then a step over all the elements is not
    // an error but a free index: its size is noted and the walk goes down.
    private static Stop walk(MemoryLayout root, MemoryLayout.PathElement[] path,
            boolean wantOffset, java.util.List<Long> open) {
        if (path == null) {
            throw new IllegalArgumentException("the path cannot be null");
        }
        MemoryLayout current = root;
        long offset = 0L;
        int i = 0;
        while (i < path.length) {
            if (!(path[i] instanceof Step)) {
                throw new IllegalArgumentException("unknown path step: " + path[i]);
            }
            Step p = (Step) path[i];
            if (p.kind == BY_NAME || p.kind == BY_POSITION) {
                if (!(current instanceof GroupLayout)) {
                    throw new IllegalArgumentException(
                            "not a group, it has no members: " + current);
                }
                GroupLayout g = (GroupLayout) current;
                List<MemoryLayout> ms = g.memberLayouts();
                int pos = p.kind == BY_NAME ? findByName(ms, p.name) : (int) p.index;
                if (pos < 0 || pos >= ms.size()) {
                    throw new IllegalArgumentException(
                            "there is no member " + (p.name != null ? p.name : String.valueOf(p.index))
                                    + " en " + current);
                }
                // In a struct the offset is the sum of the previous ones; in a union they all
                // start at zero, which is what a union **is**.
                if (g instanceof StructLayout) {
                    int k = 0;
                    while (k < pos) {
                        offset = offset + ms.get(k).byteSize();
                        k = k + 1;
                    }
                }
                current = ms.get(pos);
            } else if (p.kind == ELEMENT) {
                SequenceLayout s = asSequence(current);
                if (!wantOffset) {
                    // `select` **refuses** an index, and the reason is a good one: the index does
                    // not change the layout that is there --all the elements are alike-- so
                    // accepting it would suggest that it does. To ask what is there, the open step.
                    throw new IllegalArgumentException(
                            "select does not accept an indexed element; use sequenceElement()");
                }
                if (p.index < 0L || p.index >= s.elementCount()) {
                    throw new IndexOutOfBoundsException("element " + p.index + " of " + current);
                }
                offset = offset + p.index * s.elementLayout().byteSize();
                current = s.elementLayout();
            } else if (p.kind == RANGE) {
                SequenceLayout s = asSequence(current);
                // A range designates neither a position nor a different layout: refused in both.
                throw new IllegalArgumentException(
                        "a range of elements designates neither an offset nor a layout of its own");
            } else if (p.kind == ALL) {
                SequenceLayout s = asSequence(current);
                if (open != null) {
                    open.add(Long.valueOf(s.elementLayout().byteSize()));
                    current = s.elementLayout();
                    i = i + 1;
                    continue;
                }
                if (wantOffset) {
                    // A step opening **all** the elements designates no position, and therefore has
                    // no offset. Its other use is building a `VarHandle` with a free index, which is
                    // the branch just above; here the walk was asked for an offset, so there is
                    // nothing to answer.
                    throw new IllegalArgumentException(
                            "a step over all the elements designates no offset; use"
                                    + " sequenceElement(index)");
                }
                current = s.elementLayout();
            } else {
                // DEREFERENCE. Refused in the offset and the layout walks, and for the same reason:
                // following a pointer **leaves** this layout, so neither is the offset measured from
                // here nor is the target layout a part of this one.
                //
                // In the JDK the step is also good for the methods that build a `VarHandle`, which
                // there can dereference on access. Those methods do exist here, but this walk does
                // not implement the dereferencing access they would need, so the step is refused for
                // them too -- said to the caller's face instead of failing further along.
                if (!(current instanceof AddressLayout)) {
                    throw new IllegalArgumentException("not an address: " + current);
                }
                throw new IllegalArgumentException(
                        "a dereference step is not supported by this implementation's path"
                                + " walk");
            }
            i = i + 1;
        }
        return new Stop(offset, current);
    }

    private static SequenceLayout asSequence(MemoryLayout l) {
        if (!(l instanceof SequenceLayout)) {
            throw new IllegalArgumentException("not a sequence: " + l);
        }
        return (SequenceLayout) l;
    }

    private static int findByName(List<MemoryLayout> ms, String name) {
        int i = 0;
        while (i < ms.size()) {
            if (ms.get(i).name().isPresent() && ms.get(i).name().get().equals(name)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    static long scaled(MemoryLayout layout, long offset, long index) {
        if (offset < 0L) {
            throw new IllegalArgumentException("negative offset: " + offset);
        }
        if (index < 0L) {
            throw new IllegalArgumentException("negative index: " + index);
        }
        return offset + index * layout.byteSize();
    }
}

// One step of the path. The kind is kept as an `int` and not as an enum because the six variants
// have no behaviour of their own: the one deciding what to do is the walker.
final class Step implements MemoryLayout.PathElement {

    final int kind;
    final String name;
    final long index;
    final long stride;

    Step(int kind, String name, long index, long stride) {
        this.kind = kind;
        this.name = name;
        this.index = index;
        this.stride = stride;
    }

    public String toString() {
        if (this.kind == Layouts.BY_NAME) {
            return "groupElement(" + this.name + ")";
        }
        if (this.kind == Layouts.BY_POSITION) {
            return "groupElement(" + this.index + ")";
        }
        if (this.kind == Layouts.ELEMENT) {
            return "sequenceElement(" + this.index + ")";
        }
        if (this.kind == Layouts.ALL) {
            return "sequenceElement()";
        }
        if (this.kind == Layouts.RANGE) {
            return "sequenceElement(" + this.index + ", " + this.stride + ")";
        }
        return "dereferenceElement()";
    }
}

// Where a walk ended up: the accumulated offset and the layout reached.
final class Stop {

    final long offset;
    final MemoryLayout layout;

    Stop(long offset, MemoryLayout layout) {
        this.offset = offset;
        this.layout = layout;
    }
}
