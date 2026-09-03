package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.util.List;
import jdk.internal.classfile.impl.Instructions;

// `tableswitch`: los valores son un rango contiguo de `lowValue()` a `highValue()` y la tabla es un
// arreglo de destinos indexado por el valor menos `low`. Un hueco en el rango se escribe con el
// destino por omisión, así que `cases()` puede tener menos ramas que `high - low + 1`.
public interface TableSwitchInstruction extends Instruction {

    /** El valor más chico del rango. */
    int lowValue();

    /** El valor más grande del rango. */
    int highValue();

    /** A dónde va lo que no cae en ninguna rama. */
    Label defaultTarget();

    /** Las ramas cuyo destino no es el por omisión. */
    List<SwitchCase> cases();

    /** El `tableswitch` con este rango, este destino por omisión y estas ramas. */
    public static TableSwitchInstruction of(int lowValue, int highValue, Label defaultTarget,
            List<SwitchCase> cases) {
        return Instructions.tableSwitch(lowValue, highValue, defaultTarget, cases);
    }
}
