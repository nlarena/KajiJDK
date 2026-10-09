package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// One of the eight array loads (`iaload`, `baload`, ...). The opcode already says the component's
// type, and that is why there is no operand: `typeKind()` is a reading of the opcode, not of the
// file.
public interface ArrayLoadInstruction extends Instruction {

    /** The type of the component being read. */
    TypeKind typeKind();

    /** The instruction for this opcode. It throws `IllegalArgumentException` if it is not an
     * `xaload`. */
    public static ArrayLoadInstruction of(Opcode op) {
        return Instructions.arrayLoad(op);
    }
}
