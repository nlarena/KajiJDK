package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Instructions;

// `checkcast` or `instanceof`. Both ask the same thing; what changes is what they do with the
// answer, and `opcode()` is what says that.
public interface TypeCheckInstruction extends Instruction {

    /** The type it is compared against. */
    ClassEntry type();

    /** The instruction of this opcode against this type. */
    public static TypeCheckInstruction of(Opcode op, ClassEntry type) {
        return Instructions.typeCheck(op, type);
    }

    /** The instruction of this opcode against this type. */
    public static TypeCheckInstruction of(Opcode op, ClassDesc type) {
        return Instructions.typeCheck(op, type);
    }
}
