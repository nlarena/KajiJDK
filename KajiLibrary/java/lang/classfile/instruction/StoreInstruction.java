package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// La guarda de la cima de la pila en una variable local. Vale la misma nota que en
// {@link LoadInstruction}: las tres codificaciones son una sola operación.
public interface StoreInstruction extends Instruction {

    /** La ranura de variable local. */
    int slot();

    /** El tipo que guarda. */
    TypeKind typeKind();

    /** La guarda de este tipo en esta ranura, en la codificación más corta que le entre. */
    public static StoreInstruction of(TypeKind typeKind, int slot) {
        return Instructions.store(typeKind, slot);
    }

    /** La guarda de este opcode en esta ranura. */
    public static StoreInstruction of(Opcode op, int slot) {
        return Instructions.store(op, slot);
    }
}
