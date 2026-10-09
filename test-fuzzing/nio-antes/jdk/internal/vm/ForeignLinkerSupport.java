package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.ForeignLinkerSupport -- whether this platform has a native linker.
 *
 * <p>One single question, and the answer here is **no**. `java.lang.foreign.Linker` needs to call
 * native code with the calling convention of the system, and this VM does not do it.
 *
 * <p>That the answer is negative is precisely what makes this class useful: it exists so that
 * whoever asks can take another road instead of crashing. Returning `true` would be the lie;
 * returning `false` is correct information, and it matches what `Linker.nativeLinker()` already
 * does in this library.
 */
public final class ForeignLinkerSupport {

    private ForeignLinkerSupport() {
    }

    /** Whether there is a native linker. On this VM, `false`. */
    public static boolean isSupported() {
        return false;
    }
}
