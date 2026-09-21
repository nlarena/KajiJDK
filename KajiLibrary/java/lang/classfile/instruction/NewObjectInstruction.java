package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.impl.Instructions;

// `new`. It creates the instance without initializing it: the `invokespecial` to the constructor is
// another instruction, and between the two the verifier treats the object as a type of its own that
// cannot be used.
public interface NewObjectInstruction extends Instruction {

    /** The class being instantiated. */
    ClassEntry className();

    /** This class's `new`. */
    public static NewObjectInstruction of(ClassEntry className) {
        return Instructions.newObject(className);
    }
}
