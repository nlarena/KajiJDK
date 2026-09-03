package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;

// `CONSTANT_Long_info` (JVMS §4.4.5). Ocupa dos ranuras del pool: `width()` da 2.
public interface LongEntry extends AnnotationConstantValueEntry, ConstantValueEntry {

    /** El valor. */
    long longValue();

    default TypeKind typeKind() {
        return TypeKind.LONG;
    }
}
