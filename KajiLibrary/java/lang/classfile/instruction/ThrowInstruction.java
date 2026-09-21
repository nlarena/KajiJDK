package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import jdk.internal.classfile.impl.Instructions;

// `athrow`. No operands: what is thrown is at the top of the stack.
public interface ThrowInstruction extends Instruction {

    /** The `athrow`. */
    public static ThrowInstruction of() {
        return Instructions.throwInstruction();
    }
}
