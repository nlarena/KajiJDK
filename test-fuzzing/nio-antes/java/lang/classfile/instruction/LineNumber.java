package java.lang.classfile.instruction;

import java.lang.classfile.PseudoInstruction;
import jdk.internal.classfile.impl.Instructions;

// A row of the `LineNumberTable` seen from the method's body: from here on, the instructions come
// from this line of the source.
public interface LineNumber extends PseudoInstruction {

    /** The line number. */
    int line();

    /** This line's mark. */
    public static LineNumber of(int line) {
        return Instructions.lineNumber(line);
    }
}
