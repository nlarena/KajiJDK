package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.Stack -- the frames of the call stack.
 *
 * <p>It is the seam that everything that needs to know **who called** was missing:
 * {@link SecurityManager#getClassContext()}, `StackWalker`, and the traces of `Throwable`. Until it
 * existed, none of those had anywhere to get the answer from, and that is why they were left out or
 * empty.
 *
 * <p>It is not a `native` of the bridge but an **intrinsic of the interpreter**, and the reason is
 * structural: the bridge of natives receives the metaspace and the heap, and **not the stack of
 * frames**. The only place where the frames are in view is inside the interpreter.
 *
 * <p>Each entry is `"class|method"`, with the class in binary form (`java/lang/String`). The order
 * is from the top down: the first is whoever called {@link #frames()}.
 *
 * <p><strong>Nothing is trimmed.</strong> The stack is returned as it is, including the frame of
 * the caller, and whoever uses it decides how many levels of their own to discard. Trimming here
 * would force guessing how many wrapper frames the caller put in, and the VM does not know that
 * number.
 */
public final class Stack {

    private Stack() {
    }

    /** The frames of the stack, from the top down, as `"class|method"`. */
    public static native String[] frames();
}
