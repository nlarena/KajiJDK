package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// `monitorenter` or `monitorexit`. Which of the two is said by `opcode()`, which is already in
// {@link Instruction}; that is why this interface adds no accessor.
public interface MonitorInstruction extends Instruction {

    /** The instruction for this opcode. It throws `IllegalArgumentException` if it is not a
     * monitor one. */
    public static MonitorInstruction of(Opcode op) {
        return Instructions.monitor(op);
    }
}
