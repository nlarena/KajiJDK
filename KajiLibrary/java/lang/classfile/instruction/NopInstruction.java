package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import jdk.internal.classfile.impl.Instructions;

// `nop`. No tiene operandos ni variantes, así que la interfaz no declara nada propio.
public interface NopInstruction extends Instruction {

    /** El `nop`. */
    public static NopInstruction of() {
        return Instructions.nop();
    }
}
