package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.impl.Instructions;

// `anewarray`. La entrada del pool nombra al COMPONENTE, no al arreglo: `anewarray String` crea un
// `String[]`. Es lo contrario de `multianewarray`, donde la entrada es el arreglo entero.
public interface NewReferenceArrayInstruction extends Instruction {

    /** El tipo del componente. */
    ClassEntry componentType();

    /** El `anewarray` de este componente. */
    public static NewReferenceArrayInstruction of(ClassEntry componentType) {
        return Instructions.newReferenceArray(componentType);
    }
}
