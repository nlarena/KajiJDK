package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import jdk.internal.classfile.impl.Instructions;

// A branch of a `tableswitch` or of a `lookupswitch`: the value and where it goes. It is NOT an
// `Instruction` nor a `CodeElement` -- it is a part of one, and that is why it does not turn up on
// its own while walking a method's body.
public interface SwitchCase {

    /** The value that selects it. */
    int caseValue();

    /** Where it jumps to. */
    Label target();

    /** The branch with this value and this destination. */
    public static SwitchCase of(int caseValue, Label target) {
        return Instructions.switchCase(caseValue, target);
    }
}
