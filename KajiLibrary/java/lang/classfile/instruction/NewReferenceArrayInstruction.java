package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.impl.Instructions;

// `anewarray`. The pool entry names the COMPONENT, not the array: `anewarray String` creates a
// `String[]`. It is the opposite of `multianewarray`, where the entry is the whole array.
public interface NewReferenceArrayInstruction extends Instruction {

    /** The component's type. */
    ClassEntry componentType();

    /** This component's `anewarray`. */
    public static NewReferenceArrayInstruction of(ClassEntry componentType) {
        return Instructions.newReferenceArray(componentType);
    }
}
