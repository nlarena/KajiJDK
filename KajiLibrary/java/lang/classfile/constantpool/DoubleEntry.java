package java.lang.classfile.constantpool;

import java.lang.classfile.TypeKind;

// `CONSTANT_Double_info` (JVMS §4.4.5). Ocupa dos ranuras del pool: `width()` da 2.
public interface DoubleEntry extends AnnotationConstantValueEntry, ConstantValueEntry {

    /** El valor. */
    double doubleValue();

    default TypeKind typeKind() {
        return TypeKind.DOUBLE;
    }
}
