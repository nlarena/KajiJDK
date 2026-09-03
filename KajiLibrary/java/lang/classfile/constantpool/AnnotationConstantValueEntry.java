package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;

// Una entrada que puede aparecer como valor constante dentro de un `element_value` de anotación
// (JVMS §4.7.16.1): las cinco constantes de valor más `CONSTANT_Utf8`, que es la que ahí representa
// un `String` y un nombre de clase. Es distinto de {@link LoadableConstantEntry}: `Utf8` no es
// cargable con `ldc`, y `Class`/`MethodType`/`MethodHandle`/`Dynamic` no valen como `element_value`.
public interface AnnotationConstantValueEntry extends PoolEntry {

    /** El descriptor nominal del valor. */
    ConstantDesc constantValue();
}
