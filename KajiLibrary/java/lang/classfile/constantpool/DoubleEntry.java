package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;

// `CONSTANT_Double_info` (JVMS §4.4.5). It takes two slots of the pool: `width()` gives 2.
public interface DoubleEntry extends AnnotationConstantValueEntry, ConstantValueEntry {

    /** The value. */
    double doubleValue();

    default TypeKind typeKind() {
        return TypeKind.DOUBLE;
    }
}
