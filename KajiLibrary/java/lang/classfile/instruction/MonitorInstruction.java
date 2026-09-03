package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// `monitorenter` o `monitorexit`. Cuál de los dos lo dice `opcode()`, que ya está en
// {@link Instruction}; por eso esta interfaz no agrega ningún accesor.
public interface MonitorInstruction extends Instruction {

    /** La instrucción de este opcode. Tira `IllegalArgumentException` si no es de monitor. */
    public static MonitorInstruction of(Opcode op) {
        return Instructions.monitor(op);
    }
}
