package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// `newarray`. El tipo del componente va en un byte `atype` propio del opcode y no en el pool, que es
// lo que la separa de `anewarray`.
public interface NewPrimitiveArrayInstruction extends Instruction {

    /** El tipo del componente. */
    TypeKind typeKind();

    /** El `newarray` de este tipo. Tira `IllegalArgumentException` si no es primitivo. */
    public static NewPrimitiveArrayInstruction of(TypeKind typeKind) {
        return Instructions.newPrimitiveArray(typeKind);
    }
}
