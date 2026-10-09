package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.constantpool.ClassEntry;
import java.util.Optional;

// A row of the `Code` attribute's `exception_table` (JVMS §4.7.3): the protected range, the
// handler's destination and the caught type. The empty type is the catch-all -- what the compiler
// emits for a `finally`.
//
// It is a pseudo-instruction and not an instruction because it takes no bytes in the `code` array: it
// lives in a table apart from the `Code` attribute and only refers to positions of it.
public interface ExceptionCatch extends PseudoInstruction {

    /** Where the handler begins. */
    Label handler();

    /** Where the protected range begins. */
    Label tryStart();

    /** Where the protected range ends, exclusive. */
    Label tryEnd();

    /** The caught type; empty if it catches everything. */
    Optional<ClassEntry> catchType();

    /** A row with these values. */
    public static ExceptionCatch of(Label handler, Label tryStart, Label tryEnd,
            Optional<ClassEntry> catchType) {
        return new jdk.internal.classfile.impl.ExceptionCatchImpl(handler, tryStart, tryEnd,
                catchType);
    }

    /** A catch-all row, that is, the one the compiler emits for a `finally`. */
    public static ExceptionCatch of(Label handler, Label tryStart, Label tryEnd) {
        return new jdk.internal.classfile.impl.ExceptionCatchImpl(handler, tryStart, tryEnd,
                Optional.<ClassEntry>empty());
    }
}
