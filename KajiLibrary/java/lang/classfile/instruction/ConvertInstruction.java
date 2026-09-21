package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// A numeric conversion (`i2l`, `d2f`, `i2b`, ...). The three that narrow to `byte`, `char` and
// `short` start from `INT` and come back to `INT` on the stack, but `toType()` says the narrow type:
// it is what tells `i2b` from a `nop`.
public interface ConvertInstruction extends Instruction {

    /** The type it starts from. */
    TypeKind fromType();

    /** The type it arrives at. */
    TypeKind toType();

    /** The conversion from `fromType` to `toType`. It throws `IllegalArgumentException` if it does
     * not exist. */
    public static ConvertInstruction of(TypeKind fromType, TypeKind toType) {
        return Instructions.convert(fromType, toType);
    }

    /** This opcode's conversion. */
    public static ConvertInstruction of(Opcode op) {
        return Instructions.convert(op);
    }
}
