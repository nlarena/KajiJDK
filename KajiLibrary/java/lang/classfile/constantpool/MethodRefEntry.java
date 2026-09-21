package java.lang.classfile.constantpool;

import java.lang.constant.MethodTypeDesc;

// `CONSTANT_Methodref_info` (JVMS §4.4.2): a reference to a method of a *class*. That the owner is
// not an interface is a condition of the format, not a consequence of the structure.
public interface MethodRefEntry extends MemberRefEntry {

    /** The method's type. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }
}
