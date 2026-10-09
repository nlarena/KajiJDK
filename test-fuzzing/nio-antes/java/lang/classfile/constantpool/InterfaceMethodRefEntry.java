package java.lang.classfile.constantpool;

import java.lang.constant.MethodTypeDesc;

// `CONSTANT_InterfaceMethodref_info` (JVMS §4.4.2): a reference to a method of an *interface*.
public interface InterfaceMethodRefEntry extends MemberRefEntry {

    /** The method's type. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }
}
