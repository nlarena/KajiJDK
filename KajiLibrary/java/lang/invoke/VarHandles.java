package java.lang.invoke;

/**
 * KajiLibrary's java.lang.invoke.VarHandles -- the door through which `java.lang.foreign` builds its
 * {@link VarHandle}s.
 *
 * <p>It exists for a reason of visibility and not of design: {@link VarHandle}'s constructor is
 * package-private, so its subclass has to live in `java.lang.invoke`, and `java.lang.foreign`
 * --which is what builds it-- cannot name it.
 *
 * <h2>A divergence, said to your face</h2>
 *
 * <p>The JDK has a class with **this same name in this same package**, and has it package-private:
 * `final class java.lang.invoke.VarHandles`. It can afford that because it solves the seam with
 * **modules** -- `jdk.internal.foreign` reaches it through a qualified `opens`--. Without modules,
 * the only way for one package to reach another is for the member to be public.
 *
 * <p>So this is API the real JDK does not have, and it is worth knowing: code written against
 * `VarHandles.ofSegment` compiles here and does **not** compile against a real JDK. It is not a
 * member that lies --it does exactly what it says-- but it is a member too many, which is the other
 * way of not matching. It is chosen over the alternatives because both of those are worse: opening
 * `VarHandle`'s constructor would change a class that **is** API, and building the object from a
 * `native` would hide inside the VM a decision that deserves to be read in the source.
 */
public final class VarHandles {

    private VarHandles() {
    }

    /**
     * A `VarHandle` over a segment: the value's layout, the path's fixed displacement, and how much
     * each open step measures.
     */
    public static VarHandle ofSegment(java.lang.foreign.MemoryLayout layout,
            long fixedOffset, long[] strides) {
        return new SegmentVarHandle(layout, fixedOffset, strides);
    }

    /** `scaleHandle()`: `(long base, long index) -> long`. */
    public static MethodHandle scale(java.lang.foreign.MemoryLayout root) {
        return new LayoutMethodHandle(
                MethodType.methodType(Long.TYPE, new Class<?>[] {Long.TYPE, Long.TYPE}),
                LayoutMethodHandle.SCALE, root, root, 0L, new long[0]);
    }

    /** `byteOffsetHandle(path)`: `(long base, long... indices) -> long`. */
    public static MethodHandle pathOffsetHandle(java.lang.foreign.MemoryLayout root,
            java.lang.foreign.MemoryLayout target, long fixedOffset, long[] strides) {
        return new LayoutMethodHandle(longType(strides.length), LayoutMethodHandle.OFFSET,
                root, target, fixedOffset, strides);
    }

    /** `sliceHandle(path)`: `(MemorySegment, long base, long... indices) -> MemorySegment`. */
    public static MethodHandle pathSliceHandle(java.lang.foreign.MemoryLayout root,
            java.lang.foreign.MemoryLayout target, long fixedOffset, long[] strides) {
        return new LayoutMethodHandle(sliceType(strides.length), LayoutMethodHandle.SLICE,
                root, target, fixedOffset, strides);
    }

    // `(long, long…n) -> long`
    private static MethodType longType(int n) {
        Class<?>[] ps = new Class<?>[1 + n];
        int i = 0;
        while (i < ps.length) {
            ps[i] = Long.TYPE;
            i = i + 1;
        }
        return MethodType.methodType(Long.TYPE, ps);
    }

    // `(MemorySegment, long, long…n) -> MemorySegment`
    private static MethodType sliceType(int n) {
        Class<?>[] ps = new Class<?>[2 + n];
        ps[0] = java.lang.foreign.MemorySegment.class;
        int i = 1;
        while (i < ps.length) {
            ps[i] = Long.TYPE;
            i = i + 1;
        }
        return MethodType.methodType(java.lang.foreign.MemorySegment.class, ps);
    }
}
