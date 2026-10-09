package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;

// The mark that a {@link Label} falls at this point of the body. It takes no bytes in the `code`
// array: it is what turns a position into an identity, and that is why it is a
// pseudo-instruction.
public interface LabelTarget extends PseudoInstruction {

    /** The label that resolves here. */
    Label label();
}
