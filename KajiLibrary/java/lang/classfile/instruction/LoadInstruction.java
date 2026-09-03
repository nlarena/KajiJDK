package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// La carga de una variable local a la pila. Las tres formas del formato —`aload_0`, `aload` con un
// byte, y `wide aload` con dos— son la MISMA operación con distinta codificación, y esta interfaz
// las unifica: `slot()` da el número de ranura sin importar cómo estaba escrito.
public interface LoadInstruction extends Instruction {

    /** La ranura de variable local. */
    int slot();

    /** El tipo que carga. */
    TypeKind typeKind();

    /** La carga de este tipo desde esta ranura, en la codificación más corta que le entre. */
    public static LoadInstruction of(TypeKind typeKind, int slot) {
        return Instructions.load(typeKind, slot);
    }

    /** La carga de este opcode desde esta ranura. */
    public static LoadInstruction of(Opcode op, int slot) {
        return Instructions.load(op, slot);
    }
}
