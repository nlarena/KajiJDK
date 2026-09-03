package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// Una de las ocho lecturas de arreglo (`iaload`, `baload`, …). El opcode ya dice el tipo del
// componente, y por eso no hay operando: `typeKind()` es una lectura del opcode, no del archivo.
public interface ArrayLoadInstruction extends Instruction {

    /** El tipo del componente que se lee. */
    TypeKind typeKind();

    /** La instrucción de este opcode. Tira `IllegalArgumentException` si no es un `xaload`. */
    public static ArrayLoadInstruction of(Opcode op) {
        return Instructions.arrayLoad(op);
    }
}
