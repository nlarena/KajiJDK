package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import jdk.internal.classfile.impl.Instructions;

// `iinc`: sumarle una constante a una variable local sin pasar por la pila. La forma corta guarda la
// ranura en un byte y la constante en un byte con signo; la ensanchada, los dos en dos bytes. Que
// `constant()` sea `int` y no `byte` es por eso: el rango depende de la codificación.
public interface IncrementInstruction extends Instruction {

    /** La ranura de variable local. */
    int slot();

    /** Lo que se le suma. */
    int constant();

    /** El `iinc` de esta ranura y esta constante, en la codificación más corta que les entre. */
    public static IncrementInstruction of(int slot, int constant) {
        return Instructions.increment(slot, constant);
    }
}
