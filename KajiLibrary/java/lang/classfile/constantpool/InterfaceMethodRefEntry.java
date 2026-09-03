package java.lang.classfile.constantpool;

import java.lang.constant.MethodTypeDesc;

// `CONSTANT_InterfaceMethodref_info` (JVMS §4.4.2): referencia a un método de una *interfaz*.
public interface InterfaceMethodRefEntry extends MemberRefEntry {

    /** El tipo del método. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }
}
