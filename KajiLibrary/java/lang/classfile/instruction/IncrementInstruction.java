package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import jdk.internal.classfile.impl.Instructions;

// `iinc`: adding a constant to a local variable without going through the stack. The short form
// stores the slot in one byte and the constant in a signed byte; the widened one, both in two bytes.
// That `constant()` is an `int` and not a `byte` is because of that: the range depends on the
// encoding.
public interface IncrementInstruction extends Instruction {

    /** The local variable slot. */
    int slot();

    /** What is added to it. */
    int constant();

    /** The `iinc` of this slot and this constant, in the shortest encoding they fit into. */
    public static IncrementInstruction of(int slot, int constant) {
        return Instructions.increment(slot, constant);
    }
}
