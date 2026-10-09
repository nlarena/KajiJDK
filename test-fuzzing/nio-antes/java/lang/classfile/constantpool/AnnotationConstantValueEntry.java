package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;

// An entry that may appear as a constant value inside an annotation's `element_value`
// (JVMS §4.7.16.1): the five value constants plus `CONSTANT_Utf8`, which is the one that represents a
// `String` and a class name there. It differs from {@link LoadableConstantEntry}: `Utf8` is not
// loadable with `ldc`, and `Class`/`MethodType`/`MethodHandle`/`Dynamic` do not count as an
// `element_value`.
public interface AnnotationConstantValueEntry extends PoolEntry {

    /** The value's nominal descriptor. */
    ConstantDesc constantValue();
}
