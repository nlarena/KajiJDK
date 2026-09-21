package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.impl.Instructions;

// `multianewarray`. The pool entry is the type of the WHOLE array (`[[[I`), and `dimensions()` says
// how many of those dimensions really get created -- the extra ones are left at `null`, and that is
// why the number may be smaller than the descriptor's brackets.
public interface NewMultiArrayInstruction extends Instruction {

    /** The array's type. */
    ClassEntry arrayType();

    /** How many dimensions get created. */
    int dimensions();

    /** The `multianewarray` of this type and these dimensions. */
    public static NewMultiArrayInstruction of(ClassEntry arrayType, int dimensions) {
        return Instructions.newMultiArray(arrayType, dimensions);
    }
}
