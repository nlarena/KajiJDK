package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;
import java.lang.constant.ConstantDesc;

// Una entrada que puede ser el operando de `ldc`/`ldc_w`/`ldc2_w` (JVMS §4.4, tabla de "loadable
// constants"): las cinco constantes de valor, más `CONSTANT_Class`, `CONSTANT_MethodType`,
// `CONSTANT_MethodHandle` y `CONSTANT_Dynamic`.
//
// `typeKind()` es el tipo de lo que queda en la pila al cargarla. Por defecto `REFERENCE`, que es lo
// correcto para clase, tipo de método y method handle; las numéricas lo pisan.
public interface LoadableConstantEntry extends PoolEntry {

    /** El descriptor nominal de la constante que esta entrada carga. */
    ConstantDesc constantValue();

    /** El tipo del valor que `ldc` deja en la pila. */
    default TypeKind typeKind() {
        return TypeKind.REFERENCE;
    }
}
