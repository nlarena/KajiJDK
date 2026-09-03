package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.impl.Instructions;

// `multianewarray`. La entrada del pool es el tipo del arreglo COMPLETO (`[[[I`), y `dimensions()`
// dice cuántas de esas dimensiones se crean de verdad — las de más quedan en `null`, y por eso el
// número puede ser menor que los corchetes del descriptor.
public interface NewMultiArrayInstruction extends Instruction {

    /** El tipo del arreglo. */
    ClassEntry arrayType();

    /** Cuántas dimensiones se crean. */
    int dimensions();

    /** El `multianewarray` de este tipo y estas dimensiones. */
    public static NewMultiArrayInstruction of(ClassEntry arrayType, int dimensions) {
        return Instructions.newMultiArray(arrayType, dimensions);
    }
}
