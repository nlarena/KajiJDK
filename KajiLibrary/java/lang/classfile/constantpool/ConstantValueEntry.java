package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;

// The pool's five value constants: `Integer`, `Float`, `Long`, `Double` and `String`. They are the
// only ones that can be the `ConstantValue` attribute's `constantvalue_index` (JVMS §4.7.2), and the
// only ones that are both loadable with `ldc` and admissible as an annotation value.
public interface ConstantValueEntry extends LoadableConstantEntry {

    /** The value: a boxed `Integer`, `Float`, `Long`, `Double` or `String`. */
    ConstantDesc constantValue();
}
