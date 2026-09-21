package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// A conditional or unconditional branch. The destination is a {@link Label} and not a number: the
// file stores it as an offset relative to the instruction's bci, but exposing it that way would force
// recomputing it every time code is inserted or removed before the branch.
public interface BranchInstruction extends Instruction {

    /** Where it jumps to. */
    Label target();

    /** The branch of this opcode to this label. */
    public static BranchInstruction of(Opcode op, Label target) {
        return Instructions.branch(op, target);
    }
}
