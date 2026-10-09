package java.lang.classfile.constantpool;

import java.lang.constant.ClassDesc;

// `CONSTANT_Fieldref_info` (JVMS §4.4.2). Its descriptor is a field one, so it is read as a
// `ClassDesc`.
public interface FieldRefEntry extends MemberRefEntry {

    /** The field's type. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }
}
