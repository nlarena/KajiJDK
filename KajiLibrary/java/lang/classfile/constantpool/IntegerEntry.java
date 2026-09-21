package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;

// `CONSTANT_Integer_info` (JVMS §4.4.4). It is also the representation of `boolean`, `byte`, `char`
// and `short` in an annotation value and in a `ConstantValue` attribute: the format does not tell
// them apart.
public interface IntegerEntry extends AnnotationConstantValueEntry, ConstantValueEntry {

    /** The value. */
    int intValue();

    default TypeKind typeKind() {
        return TypeKind.INT;
    }
}
