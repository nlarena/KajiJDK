package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;

// Las cinco constantes de valor del pool: `Integer`, `Float`, `Long`, `Double` y `String`. Son las
// únicas que pueden ser el `constantvalue_index` del atributo `ConstantValue` (JVMS §4.7.2), y las
// únicas que son a la vez cargables con `ldc` y admisibles como valor de anotación.
public interface ConstantValueEntry extends LoadableConstantEntry {

    /** El valor: un `Integer`, `Float`, `Long`, `Double` o `String` boxeado. */
    ConstantDesc constantValue();
}
