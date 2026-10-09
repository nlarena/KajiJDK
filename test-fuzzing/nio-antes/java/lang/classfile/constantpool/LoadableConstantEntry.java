package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;
import java.lang.constant.ConstantDesc;

// An entry that can be the operand of `ldc`/`ldc_w`/`ldc2_w` (JVMS §4.4, the "loadable constants"
// table): the five value constants, plus `CONSTANT_Class`, `CONSTANT_MethodType`,
// `CONSTANT_MethodHandle` and `CONSTANT_Dynamic`.
//
// `typeKind()` is the type of what is left on the stack when it is loaded. `REFERENCE` by default,
// which is right for a class, a method type and a method handle; the numeric ones override it.
public interface LoadableConstantEntry extends PoolEntry {

    /** The nominal descriptor of the constant this entry loads. */
    ConstantDesc constantValue();

    /** The type of the value `ldc` leaves on the stack. */
    default TypeKind typeKind() {
        return TypeKind.REFERENCE;
    }
}
