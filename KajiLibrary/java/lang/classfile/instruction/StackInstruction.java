package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// Una manipulación de la pila de operandos: `pop`, `dup`, `swap` y sus variantes. Ninguna mira el
// tipo de lo que mueve —sólo su categoría, de una o dos ranuras—, y por eso no hay `typeKind()`.
public interface StackInstruction extends Instruction {

    /** La instrucción de este opcode. Tira `IllegalArgumentException` si no es de pila. */
    public static StackInstruction of(Opcode op) {
        return Instructions.stack(op);
    }
}
