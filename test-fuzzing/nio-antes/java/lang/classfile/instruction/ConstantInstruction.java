package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.constant.ConstantDesc;
import jdk.internal.classfile.impl.Instructions;

// Putting a constant on the stack. The format has three ways of doing it and this interface keeps
// them in three subtypes because they are not interchangeable:
//
//   1. `IntrinsicConstantInstruction` -- the value is IN the opcode (`iconst_1`, `aconst_null`).
//   2. `ArgumentConstantInstruction` -- the value is in the immediate operand (`bipush`, `sipush`),
//      always a small `int`.
//   3. `LoadConstantInstruction` -- the value is in the pool (`ldc`, `ldc_w`, `ldc2_w`).
//
// `constantValue()` returns the value of all three, but only the third can carry a `String`, a class
// literal or a dynamic constant.
public interface ConstantInstruction extends Instruction {

    /** The value it loads. */
    ConstantDesc constantValue();

    /** The value's type. */
    TypeKind typeKind();

    /** The constant that goes inside the opcode. */
    public static IntrinsicConstantInstruction ofIntrinsic(Opcode op) {
        return Instructions.intrinsicConstant(op);
    }

    /** The constant that goes in the immediate operand. */
    public static ArgumentConstantInstruction ofArgument(Opcode op, int value) {
        return Instructions.argumentConstant(op, value);
    }

    /** The constant that goes in the pool. */
    public static LoadConstantInstruction ofLoad(Opcode op, LoadableConstantEntry constant) {
        return Instructions.loadConstant(op, constant);
    }

    /** A constant the opcode already carries. */
    public interface IntrinsicConstantInstruction extends ConstantInstruction {

        /** The type, which comes from the opcode. */
        default TypeKind typeKind() {
            return Instructions.intrinsicConstantTypeKind(opcode());
        }
    }

    /** A constant travelling in the immediate operand: always an `int`. */
    public interface ArgumentConstantInstruction extends ConstantInstruction {

        /** The value. */
        Integer constantValue();

        /** Always `INT`. */
        default TypeKind typeKind() {
            return TypeKind.INT;
        }
    }

    /** A constant travelling in the pool. */
    public interface LoadConstantInstruction extends ConstantInstruction {

        /** The pool entry. */
        LoadableConstantEntry constantEntry();

        /** The type, which comes from the entry. */
        default TypeKind typeKind() {
            return constantEntry().typeKind();
        }
    }
}
