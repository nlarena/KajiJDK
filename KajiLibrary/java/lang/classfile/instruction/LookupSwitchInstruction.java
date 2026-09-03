package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.util.List;
import jdk.internal.classfile.impl.Instructions;

// `lookupswitch`: los valores son arbitrarios y van con su destino en una tabla que el formato
// obliga a mantener ordenada por valor, porque la JVM la busca en binario.
public interface LookupSwitchInstruction extends Instruction {

    /** A dónde va lo que no cae en ninguna rama. */
    Label defaultTarget();

    /** Las ramas, ordenadas por valor. */
    List<SwitchCase> cases();

    /** El `lookupswitch` con este destino por omisión y estas ramas. */
    public static LookupSwitchInstruction of(Label defaultTarget, List<SwitchCase> cases) {
        return Instructions.lookupSwitch(defaultTarget, cases);
    }
}
