package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;

// `CONSTANT_Long_info` (JVMS §4.4.5). It takes two slots of the pool: `width()` gives 2.
public interface LongEntry extends AnnotationConstantValueEntry, ConstantValueEntry {

    /** The value. */
    long longValue();

    default TypeKind typeKind() {
        return TypeKind.LONG;
    }
}
