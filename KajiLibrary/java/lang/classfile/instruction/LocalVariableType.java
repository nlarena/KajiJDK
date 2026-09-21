package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.Signature;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.Instructions;

// A row of the `LocalVariableTypeTable`. It is {@link LocalVariable}'s generic twin: it exists apart
// and not as one more field of that one because the format only emits it for the variables whose type
// canNOT be written as a descriptor, and a variable may be in both tables at once.
public interface LocalVariableType extends PseudoInstruction {

    /** The local variable slot. */
    int slot();

    /** The variable's name. */
    Utf8Entry name();

    /** The generic signature, as a `Utf8`. */
    Utf8Entry signature();

    /** The generic signature already parsed. */
    default Signature signatureSymbol() {
        return Signature.parseFrom(signature().stringValue());
    }

    /** Where the scope begins. */
    Label startScope();

    /** Where it ends, exclusive. */
    Label endScope();

    /** The row with these values. */
    public static LocalVariableType of(int slot, Utf8Entry name, Utf8Entry signature,
            Label startScope, Label endScope) {
        return Instructions.localVariableType(slot, name, signature, startScope, endScope);
    }

    /** The row with these values. */
    public static LocalVariableType of(int slot, String name, Signature signature, Label startScope,
            Label endScope) {
        return Instructions.localVariableType(slot, name, signature, startScope, endScope);
    }
}
