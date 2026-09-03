package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import jdk.internal.classfile.impl.Instructions;

// Una rama de un `tableswitch` o de un `lookupswitch`: el valor y a dónde va. NO es una
// `Instruction` ni un `CodeElement` — es una parte de una, y por eso no aparece sola en el recorrido
// del cuerpo de un método.
public interface SwitchCase {

    /** El valor que la elige. */
    int caseValue();

    /** A dónde salta. */
    Label target();

    /** La rama con este valor y este destino. */
    public static SwitchCase of(int caseValue, Label target) {
        return Instructions.switchCase(caseValue, target);
    }
}
