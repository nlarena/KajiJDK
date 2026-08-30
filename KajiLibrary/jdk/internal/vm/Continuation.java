package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.Continuation -- a delimited continuation (Project Loom).
 *
 * <p>A minimal stub: KajiJDK does not implement continuations, so this type exists only so the
 * signatures that mention it ({@code LiveStackFrame.getStackWalker(...)}) can name it.
 */
public class Continuation {

    private final ContinuationScope scope;

    public Continuation(ContinuationScope scope, Runnable target) {
        this.scope = scope;
    }

    public ContinuationScope getScope() {
        return this.scope;
    }
}
