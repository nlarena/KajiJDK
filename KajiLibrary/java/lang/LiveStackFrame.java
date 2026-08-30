package java.lang;

import java.util.Set;
import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;

/**
 * KajiLibrary's java.lang.LiveStackFrame -- a {@link StackWalker.StackFrame} that also exposes the
 * live monitors, locals and operand stack of the frame (the JDK's internal, "extended" frame view).
 *
 * <p>Package-private and internal, like the JDK's. It exists for the shape: reading a frame's live
 * values needs the VM's deep stack introspection (and the continuation-aware factories below need
 * Project Loom), neither of which KajiJDK has, so the factories throw {@link
 * UnsupportedOperationException}.
 */
interface LiveStackFrame extends StackWalker.StackFrame {

    /** The monitors held in this frame. */
    Object[] getMonitors();

    /** The local variables of this frame (a {@code PrimitiveSlot} for a primitive). */
    Object[] getLocals();

    /** The operand stack of this frame. */
    Object[] getStack();

    /** A walker that yields {@code LiveStackFrame}s. */
    static StackWalker getStackWalker() {
        throw new UnsupportedOperationException("la introspección profunda de pila no está soportada");
    }

    static StackWalker getStackWalker(Set<StackWalker.Option> options) {
        throw new UnsupportedOperationException("la introspección profunda de pila no está soportada");
    }

    static StackWalker getStackWalker(Set<StackWalker.Option> options, ContinuationScope scope) {
        throw new UnsupportedOperationException("la introspección profunda de pila no está soportada");
    }

    static StackWalker getStackWalker(Continuation continuation) {
        throw new UnsupportedOperationException("la introspección profunda de pila no está soportada");
    }

    static StackWalker getStackWalker(
            Set<StackWalker.Option> options, ContinuationScope scope, Continuation continuation) {
        throw new UnsupportedOperationException("la introspección profunda de pila no está soportada");
    }
}
