package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// A return. `typeKind()` is `VOID` for `return` and the returned type for the other five; the four
// narrow types come back with `ireturn`, so they never turn up here.
public interface ReturnInstruction extends Instruction {

    /** The type it returns, or `VOID`. */
    TypeKind typeKind();

    /** The return of this type. */
    public static ReturnInstruction of(TypeKind typeKind) {
        return Instructions.returnInstruction(typeKind);
    }

    /** This opcode's return. */
    public static ReturnInstruction of(Opcode op) {
        return Instructions.returnInstruction(op);
    }
}
