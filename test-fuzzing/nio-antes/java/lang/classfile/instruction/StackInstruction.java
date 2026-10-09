package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// A manipulation of the operand stack: `pop`, `dup`, `swap` and their variants. None of them looks
// at the type of what it moves --only at its category, of one or two slots-- and that is why there is
// no `typeKind()`.
public interface StackInstruction extends Instruction {

    /** The instruction for this opcode. It throws `IllegalArgumentException` if it is not a stack
     * one. */
    public static StackInstruction of(Opcode op) {
        return Instructions.stack(op);
    }
}
