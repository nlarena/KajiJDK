package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.StackChunk -- a piece of stack kept in the heap.
 *
 * <p>It is the object into which the VM copies the frames of a continuation when it suspends it.
 * They chain backwards with {@link #parent()}, so a deep stack ends up split into several.
 *
 * <p><strong>The VM is the only one that fills them.</strong> There are no fields Java can write:
 * the runtime copies the frames with knowledge of the frame layout, and that is why in the JDK all
 * the accesses go through intrinsics. This VM has no continuations --see {@link
 * ContinuationSupport}--, so none of these objects is ever filled.
 *
 * <p>Hence {@link #isEmpty()} returns `true` and {@link #parent()} `null`. **It is not a
 * simulation: it is the truth about this object.** A freshly built `StackChunk` is empty in the JDK
 * too; what changes over there is that the VM fills it afterwards, and here it does not.
 */
public final class StackChunk {

    private final StackChunk parent;

    public StackChunk() {
        this.parent = null;
    }

    /**
     * It prepares the class.
     *
     * <p>In the JDK it leaves the offsets of the fields where the runtime is going to look for
     * them. Here there is nothing to prepare; it exists because the VM names it at start-up.
     */
    public static void init() {
    }

    /** The previous piece of the chain, or `null` if this one is the last. */
    public StackChunk parent() {
        return this.parent;
    }

    /** Whether it has no frames. */
    public boolean isEmpty() {
        return true;
    }
}
