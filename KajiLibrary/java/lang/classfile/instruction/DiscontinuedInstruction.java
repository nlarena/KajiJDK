package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// Las instrucciones que la JVM ya no acepta en clases nuevas pero que hay que poder LEER: `jsr` y
// `ret`, que implementaban el `finally` antes de Java 6 y que el verificador de mapas de pila
// prohíbe desde la versión mayor 51. Una biblioteca de lectura que no las modele no puede abrir un
// `.class` viejo, que es justamente el caso donde más falta hace.
public interface DiscontinuedInstruction extends Instruction {

    /** `jsr` o `jsr_w`: salta guardando la dirección de retorno en la pila. */
    public interface JsrInstruction extends DiscontinuedInstruction {

        /** A dónde salta. */
        Label target();

        /** El `jsr` de este opcode a esta etiqueta. */
        public static JsrInstruction of(Opcode op, Label target) {
            return Instructions.jsr(op, target);
        }

        /** El `jsr` de tres bytes a esta etiqueta. */
        public static JsrInstruction of(Label target) {
            return Instructions.jsr(Opcode.JSR, target);
        }
    }

    /** `ret`: vuelve a la dirección guardada en una variable local. */
    public interface RetInstruction extends DiscontinuedInstruction {

        /** La ranura donde está la dirección de retorno. */
        int slot();

        /** El `ret` de este opcode sobre esta ranura. */
        public static RetInstruction of(Opcode op, int slot) {
            return Instructions.ret(op, slot);
        }

        /** El `ret` en la codificación más corta que le entre a la ranura. */
        public static RetInstruction of(int slot) {
            return Instructions.ret(slot);
        }
    }
}
