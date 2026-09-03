package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// Una de las ocho escrituras de arreglo (`iastore`, `bastore`, …). Como en la lectura, el tipo del
// componente sale del opcode y no de un operando.
public interface ArrayStoreInstruction extends Instruction {

    /** El tipo del componente que se escribe. */
    TypeKind typeKind();

    /** La instrucción de este opcode. Tira `IllegalArgumentException` si no es un `xastore`. */
    public static ArrayStoreInstruction of(Opcode op) {
        return Instructions.arrayStore(op);
    }
}
