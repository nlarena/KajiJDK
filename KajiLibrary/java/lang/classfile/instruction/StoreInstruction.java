package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// The store of the top of the stack into a local variable. The same note as in
// {@link LoadInstruction} holds: the three encodings are a single operation.
public interface StoreInstruction extends Instruction {

    /** The local variable slot. */
    int slot();

    /** The type it stores. */
    TypeKind typeKind();

    /** The store of this type into this slot, in the shortest encoding it fits into. */
    public static StoreInstruction of(TypeKind typeKind, int slot) {
        return Instructions.store(typeKind, slot);
    }

    /** The store of this opcode into this slot. */
    public static StoreInstruction of(Opcode op, int slot) {
        return Instructions.store(op, slot);
    }
}
