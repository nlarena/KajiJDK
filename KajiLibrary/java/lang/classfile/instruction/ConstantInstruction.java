package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.constant.ConstantDesc;
import jdk.internal.classfile.impl.Instructions;

// Poner una constante en la pila. El formato tiene tres maneras de hacerlo y esta interfaz las
// separa en tres subtipos porque no son intercambiables:
//
//   1. `IntrinsicConstantInstruction` — el valor está EN el opcode (`iconst_1`, `aconst_null`).
//   2. `ArgumentConstantInstruction` — el valor está en el operando inmediato (`bipush`, `sipush`),
//      siempre un `int` chico.
//   3. `LoadConstantInstruction` — el valor está en el pool (`ldc`, `ldc_w`, `ldc2_w`).
//
// `constantValue()` devuelve el valor de las tres, pero sólo la tercera puede llevar un `String`, un
// literal de clase o una constante dinámica.
public interface ConstantInstruction extends Instruction {

    /** El valor que carga. */
    ConstantDesc constantValue();

    /** El tipo del valor. */
    TypeKind typeKind();

    /** La constante que va dentro del opcode. */
    public static IntrinsicConstantInstruction ofIntrinsic(Opcode op) {
        return Instructions.intrinsicConstant(op);
    }

    /** La constante que va en el operando inmediato. */
    public static ArgumentConstantInstruction ofArgument(Opcode op, int value) {
        return Instructions.argumentConstant(op, value);
    }

    /** La constante que va en el pool. */
    public static LoadConstantInstruction ofLoad(Opcode op, LoadableConstantEntry constant) {
        return Instructions.loadConstant(op, constant);
    }

    /** Una constante que el opcode ya trae puesta. */
    public interface IntrinsicConstantInstruction extends ConstantInstruction {

        /** El tipo, que sale del opcode. */
        default TypeKind typeKind() {
            return Instructions.intrinsicConstantTypeKind(opcode());
        }
    }

    /** Una constante que viaja en el operando inmediato: siempre un `int`. */
    public interface ArgumentConstantInstruction extends ConstantInstruction {

        /** El valor. */
        Integer constantValue();

        /** Siempre `INT`. */
        default TypeKind typeKind() {
            return TypeKind.INT;
        }
    }

    /** Una constante que viaja en el pool. */
    public interface LoadConstantInstruction extends ConstantInstruction {

        /** La entrada del pool. */
        LoadableConstantEntry constantEntry();

        /** El tipo, que sale de la entrada. */
        default TypeKind typeKind() {
            return constantEntry().typeKind();
        }
    }
}
