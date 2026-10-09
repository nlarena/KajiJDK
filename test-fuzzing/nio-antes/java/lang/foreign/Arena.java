package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.Arena -- an allocator with a **lifetime**.
 *
 * <p>It is this package's central idea about handling memory, and it is worth understanding apart
 * from the implementation: instead of freeing each block on its own --which is where "use after
 * free" errors are born-- they are all grouped into an arena and the whole arena is closed. The
 * segments it handed out stop working all together, and the attempt to use one **fails** with an
 * exception instead of reading garbage.
 *
 * <p>That is why it extends `AutoCloseable`: the normal way of using it is a try-with-resources,
 * where the close cannot be forgotten.
 *
 * <h2>What changes in this library</h2>
 *
 * <p><strong>The segments it hands out are heap ones, not native.</strong> This VM reserves no
 * system memory, so the arena backs each reservation with a Java array, chosen by the alignment
 * asked for: a `byte[]` for 1, a `short[]` for 2, an `int[]` for 4, a `long[]` for 8. An alignment
 * greater than 8 **is rejected** instead of being faked.
 *
 * <p>The visible consequence is that {@link MemorySegment#isNative()} gives `false` where the JDK
 * gives `true`. Everything else --the size, the effective alignment, the slices, the reading, the
 * writing, and above all the closing of the scope-- behaves the same.
 *
 * <p>And there is something it gains: the memory of an arena nobody closed is not lost. The
 * collector picks it up like any array, whereas in the JDK an automatic arena is the only one that
 * cleans itself up.
 */
public interface Arena extends SegmentAllocator, AutoCloseable {

    /** The scope of the segments it hands out. */
    MemorySegment.Scope scope();

    /**
     * It closes the arena: none of its segments can be used any more.
     *
     * @throws IllegalStateException if it was already closed
     * @throws UnsupportedOperationException if it is the global arena, which does not close
     */
    void close();

    MemorySegment allocate(long byteSize, long byteAlignment);

    /**
     * The arena that **never** closes.
     *
     * <p>Its segments live for the whole program. It is the one used when the lifetime is "for ever"
     * and there is therefore nothing to manage.
     */
    static Arena global() {
        return HeapArena.theGlobal();
    }

    /**
     * An arena that cleans itself up when nobody is looking.
     *
     * <p>In the JDK it is the only one whose segments the collector frees. Here **all** of them are
     * like that --the memory is Java arrays-- so this one and the others differ only in whether they
     * can be closed by hand. The distinction is kept because code choosing one or the other is
     * saying something about its intent.
     */
    static Arena ofAuto() {
        return HeapArena.fresh();
    }

    /**
     * A closeable arena, for a single thread.
     *
     * <p>In the JDK, "confined" means its segments can **only** be used from the thread that created
     * it, and that is what lets it synchronise nothing. Here it does not confine: the segments are
     * Java arrays and using them from another thread breaks nothing, so
     * {@link MemorySegment#isAccessibleBy} always gives `true`. It is one restriction **fewer**, not
     * a false answer.
     */
    static Arena ofConfined() {
        return HeapArena.fresh();
    }

    /** A closeable arena, shared between threads. */
    static Arena ofShared() {
        return HeapArena.fresh();
    }
}
