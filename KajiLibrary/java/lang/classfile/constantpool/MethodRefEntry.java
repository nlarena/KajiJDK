package java.lang.classfile.constantpool;

import java.lang.constant.MethodTypeDesc;

// `CONSTANT_Methodref_info` (JVMS §4.4.2): referencia a un método de una *clase*. Que el dueño no
// sea una interfaz es una condición del formato, no una consecuencia de la estructura.
public interface MethodRefEntry extends MemberRefEntry {

    /** El tipo del método. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }
}
