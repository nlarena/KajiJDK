package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// One of the eight array stores (`iastore`, `bastore`, ...). As with the load, the component's type
// comes from the opcode and not from an operand.
public interface ArrayStoreInstruction extends Instruction {

    /** The type of the component being written. */
    TypeKind typeKind();

    /** The instruction for this opcode. It throws `IllegalArgumentException` if it is not an
     * `xastore`. */
    public static ArrayStoreInstruction of(Opcode op) {
        return Instructions.arrayStore(op);
    }
}
