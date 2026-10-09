package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import jdk.internal.classfile.impl.Instructions;

// `nop`. It has neither operands nor variants, so the interface declares nothing of its own.
public interface NopInstruction extends Instruction {

    /** The `nop`. */
    public static NopInstruction of() {
        return Instructions.nop();
    }
}
