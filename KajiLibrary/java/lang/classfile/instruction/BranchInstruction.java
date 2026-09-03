package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// Un salto condicional o incondicional. El destino es una {@link Label} y no un número: el archivo
// lo guarda como un desplazamiento relativo al bci de la instrucción, pero exponerlo así obligaría a
// recalcularlo cada vez que se inserta o se saca código antes del salto.
public interface BranchInstruction extends Instruction {

    /** A dónde salta. */
    Label target();

    /** El salto de este opcode a esta etiqueta. */
    public static BranchInstruction of(Opcode op, Label target) {
        return Instructions.branch(op, target);
    }
}
