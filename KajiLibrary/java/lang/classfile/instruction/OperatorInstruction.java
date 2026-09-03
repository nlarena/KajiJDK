package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// Una operación aritmética, lógica o de comparación (`iadd`, `lxor`, `dcmpg`, `arraylength`). El
// tipo sale de la primera letra del opcode; `arraylength`, que no tiene ninguna, es `INT`.
public interface OperatorInstruction extends Instruction {

    /** El tipo sobre el que opera. */
    TypeKind typeKind();

    /** La instrucción de este opcode. Tira `IllegalArgumentException` si no es un operador. */
    public static OperatorInstruction of(Opcode op) {
        return Instructions.operator(op);
    }
}
