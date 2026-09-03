package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;

// `CONSTANT_Float_info` (JVMS §4.4.4).
public interface FloatEntry extends AnnotationConstantValueEntry, ConstantValueEntry {

    /** El valor. */
    float floatValue();

    default TypeKind typeKind() {
        return TypeKind.FLOAT;
    }
}
