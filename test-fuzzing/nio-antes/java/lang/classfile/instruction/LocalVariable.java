package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Instructions;

// A row of the `LocalVariableTable`: what name and what type the slot `slot()` has between
// `startScope()` and `endScope()`. The same slot may be different variables in different stretches,
// which is what makes the range part of the row's identity and not an ornament.
public interface LocalVariable extends PseudoInstruction {

    /** The local variable slot. */
    int slot();

    /** The variable's name. */
    Utf8Entry name();

    /** The type's descriptor, as a `Utf8`. */
    Utf8Entry type();

    /** The variable's type. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** Where the scope begins. */
    Label startScope();

    /** Where it ends, exclusive. */
    Label endScope();

    /** The row with these values. */
    public static LocalVariable of(int slot, Utf8Entry name, Utf8Entry descriptor,
            Label startScope, Label endScope) {
        return Instructions.localVariable(slot, name, descriptor, startScope, endScope);
    }

    /** The row with these values. */
    public static LocalVariable of(int slot, String name, ClassDesc descriptor, Label startScope,
            Label endScope) {
        return Instructions.localVariable(slot, name, descriptor, startScope, endScope);
    }
}
