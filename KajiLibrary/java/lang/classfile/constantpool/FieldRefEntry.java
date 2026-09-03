package java.lang.classfile.constantpool;

import java.lang.constant.ClassDesc;

// `CONSTANT_Fieldref_info` (JVMS §4.4.2). Su descriptor es de campo, así que se lee como `ClassDesc`.
public interface FieldRefEntry extends MemberRefEntry {

    /** El tipo del campo. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }
}
