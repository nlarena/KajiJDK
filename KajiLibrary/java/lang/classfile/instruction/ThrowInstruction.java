package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import jdk.internal.classfile.impl.Instructions;

// `athrow`. Sin operandos: lo que se lanza está en la cima de la pila.
public interface ThrowInstruction extends Instruction {

    /** El `athrow`. */
    public static ThrowInstruction of() {
        return Instructions.throwInstruction();
    }
}
