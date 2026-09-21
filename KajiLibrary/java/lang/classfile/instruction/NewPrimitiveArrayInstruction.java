package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// `newarray`. The component's type goes in an `atype` byte of the opcode's own and not in the pool,
// which is what sets it apart from `anewarray`.
public interface NewPrimitiveArrayInstruction extends Instruction {

    /** The component's type. */
    TypeKind typeKind();

    /** The `newarray` of this type. It throws `IllegalArgumentException` if it is not
     * primitive. */
    public static NewPrimitiveArrayInstruction of(TypeKind typeKind) {
        return Instructions.newPrimitiveArray(typeKind);
    }
}
