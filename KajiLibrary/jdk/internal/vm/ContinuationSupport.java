package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.ContinuationSupport -- whether this VM knows how to suspend and
 * resume stacks.
 *
 * <p>Continuations are the substrate of virtual threads: the VM lifts the stack of a thread, keeps
 * it in the heap and puts it back later, perhaps on another system thread. That is VM support, not
 * library -- there is no way of writing it in Java.
 *
 * <p>This VM does not have it, and the four answers are consistent with that:
 *
 * <ul>
 * <li>{@link #isSupported()} gives `false`.</li>
 * <li>{@link #ensureSupported()} **throws**, because its reason for being is to cut before the
 *     caller builds on something that is not there.</li>
 * <li>{@link #pinIfSupported()} and {@link #unpinIfSupported()} do nothing, and that is the right
 *     thing and not a shortcut: "pinning" a continuation means *preventing it from being suspended
 *     while this section lasts*. Where nothing is suspended, there is nothing to prevent. The name
 *     says it: `ifSupported`.</li>
 * </ul>
 */
public class ContinuationSupport {

    private ContinuationSupport() {
    }

    /** Whether the VM supports continuations. Here, `false`. */
    public static boolean isSupported() {
        return false;
    }

    /**
     * It cuts if there is no support.
     *
     * @throws UnsupportedOperationException always, on this VM
     */
    public static void ensureSupported() {
        throw new UnsupportedOperationException(
                "this VM does not support continuations: it cannot lift nor restore stacks");
    }

    /**
     * It pins the current continuation, if there is one. Here there is none, so it does nothing.
     */
    public static void pinIfSupported() {
    }

    /**
     * It unpins it. The symmetric of the previous one, and for the same reason it does nothing
     * either.
     */
    public static void unpinIfSupported() {
    }
}
