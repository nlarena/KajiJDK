package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Instructions;

// `checkcast` o `instanceof`. Las dos preguntan lo mismo; lo que cambia es qué hacen con la
// respuesta, y eso lo dice `opcode()`.
public interface TypeCheckInstruction extends Instruction {

    /** El tipo contra el que se compara. */
    ClassEntry type();

    /** La instrucción de este opcode contra este tipo. */
    public static TypeCheckInstruction of(Opcode op, ClassEntry type) {
        return Instructions.typeCheck(op, type);
    }

    /** La instrucción de este opcode contra este tipo. */
    public static TypeCheckInstruction of(Opcode op, ClassDesc type) {
        return Instructions.typeCheck(op, type);
    }
}
