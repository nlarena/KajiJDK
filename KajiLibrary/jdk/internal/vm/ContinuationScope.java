package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.ContinuationScope -- the name a group of continuations shares.
 *
 * <p>A minimal type. The note said it exists only so the signatures that mention it ({@code
 * StackWalker}/{@code LiveStackFrame}) can name it; {@link Continuation} uses it as well now, as
 * the scope a continuation is delimited by.
 */
public class ContinuationScope {

    public final String name;

    public ContinuationScope(String name) {
        this.name = name;
    }

    /**
     * For a subclass that is its **own** scope.
     *
     * <p>The JDK uses it like this: `ContinuationScope` is extended and the subclass **is** the
     * name, so the name comes from `getClass().getName()` instead of being passed. It is
     * `protected` because it only makes sense from inside a subclass -- a nameless scope created
     * from outside could not be told apart from another.
     */
    protected ContinuationScope() {
        this.name = null;
    }

    /** The name; for a subclass that used the no-argument constructor, that of its class. */
    public final String getName() {
        if (this.name == null) {
            return this.getClass().getName();
        }
        return this.name;
    }

    public final String toString() {
        return this.getName();
    }
}
