package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// The load of a local variable onto the stack. The format's three forms --`aload_0`, `aload` with
// one byte, and `wide aload` with two-- are the SAME operation with a different encoding, and this
// interface unifies them: `slot()` gives the slot number whatever way it was written.
public interface LoadInstruction extends Instruction {

    /** The local variable slot. */
    int slot();

    /** The type it loads. */
    TypeKind typeKind();

    /** The load of this type from this slot, in the shortest encoding it fits into. */
    public static LoadInstruction of(TypeKind typeKind, int slot) {
        return Instructions.load(typeKind, slot);
    }

    /** The load of this opcode from this slot. */
    public static LoadInstruction of(Opcode op, int slot) {
        return Instructions.load(op, slot);
    }
}
