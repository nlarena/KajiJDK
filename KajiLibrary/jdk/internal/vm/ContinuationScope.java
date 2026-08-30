package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.ContinuationScope -- the name a group of continuations shares.
 *
 * <p>A minimal stub: KajiJDK has no continuations (Project Loom), and this type exists only so the
 * signatures that mention it ({@code StackWalker}/{@code LiveStackFrame}) can name it.
 */
public class ContinuationScope {

    public final String name;

    public ContinuationScope(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }
}
