package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import jdk.internal.classfile.impl.Instructions;

// The instructions the JVM no longer accepts in new classes but that have to be READable: `jsr` and
// `ret`, which implemented `finally` before Java 6 and which the stack map verifier has forbidden
// since major version 51. A reading library that does not model them cannot open an old `.class`,
// which is precisely the case where it is needed most.
public interface DiscontinuedInstruction extends Instruction {

    /** `jsr` or `jsr_w`: it jumps storing the return address on the stack. */
    public interface JsrInstruction extends DiscontinuedInstruction {

        /** Where it jumps to. */
        Label target();

        /** The `jsr` of this opcode to this label. */
        public static JsrInstruction of(Opcode op, Label target) {
            return Instructions.jsr(op, target);
        }

        /** The three-byte `jsr` to this label. */
        public static JsrInstruction of(Label target) {
            return Instructions.jsr(Opcode.JSR, target);
        }
    }

    /** `ret`: it returns to the address stored in a local variable. */
    public interface RetInstruction extends DiscontinuedInstruction {

        /** The slot the return address is in. */
        int slot();

        /** The `ret` of this opcode over this slot. */
        public static RetInstruction of(Opcode op, int slot) {
            return Instructions.ret(op, slot);
        }

        /** The `ret` in the shortest encoding the slot fits into. */
        public static RetInstruction of(int slot) {
            return Instructions.ret(slot);
        }
    }
}
