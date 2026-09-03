package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import jdk.internal.classfile.impl.Instructions;

// Una conversión numérica (`i2l`, `d2f`, `i2b`, …). Las tres que estrechan a `byte`, `char` y
// `short` salen de `INT` y vuelven a `INT` en la pila, pero `toType()` dice el tipo angosto: es lo
// que distingue a `i2b` de un `nop`.
public interface ConvertInstruction extends Instruction {

    /** El tipo de partida. */
    TypeKind fromType();

    /** El tipo de llegada. */
    TypeKind toType();

    /** La conversión de `fromType` a `toType`. Tira `IllegalArgumentException` si no existe. */
    public static ConvertInstruction of(TypeKind fromType, TypeKind toType) {
        return Instructions.convert(fromType, toType);
    }

    /** La conversión de este opcode. */
    public static ConvertInstruction of(Opcode op) {
        return Instructions.convert(op);
    }
}
