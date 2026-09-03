package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;

// `CONSTANT_Integer_info` (JVMS §4.4.4). Es también la representación de `boolean`, `byte`, `char` y
// `short` en un valor de anotación y en un atributo `ConstantValue`: el formato no los distingue.
public interface IntegerEntry extends AnnotationConstantValueEntry, ConstantValueEntry {

    /** El valor. */
    int intValue();

    default TypeKind typeKind() {
        return TypeKind.INT;
    }
}
