package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// An arithmetic, logical or comparison operation (`iadd`, `lxor`, `dcmpg`, `arraylength`). The type
// comes from the opcode's first letter; `arraylength`, which has none, is `INT`.
public interface OperatorInstruction extends Instruction {

    /** The type it operates on. */
    TypeKind typeKind();

    /** The instruction for this opcode. It throws `IllegalArgumentException` if it is not an
     * operator. */
    public static OperatorInstruction of(Opcode op) {
        return Instructions.operator(op);
    }
}
