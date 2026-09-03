package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// Un retorno. `typeKind()` es `VOID` para `return` y el tipo devuelto para los otros cinco; los
// cuatro tipos angostos vuelven con `ireturn`, así que nunca aparecen acá.
public interface ReturnInstruction extends Instruction {

    /** El tipo que devuelve, o `VOID`. */
    TypeKind typeKind();

    /** El retorno de este tipo. */
    public static ReturnInstruction of(TypeKind typeKind) {
        return Instructions.returnInstruction(typeKind);
    }

    /** El retorno de este opcode. */
    public static ReturnInstruction of(Opcode op) {
        return Instructions.returnInstruction(op);
    }
}
