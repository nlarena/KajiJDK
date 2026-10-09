package java.lang.classfile.constantpool;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

// `CONSTANT_Utf8_info` (JVMS §4.4.7): the string in modified UTF-8 that sits underneath nearly
// everything else -- class names, member names, descriptors, attribute names and a
// `CONSTANT_String`'s contents.
//
// It is a `CharSequence` on purpose: it allows comparing against a name without materializing the
// `String`. `isFieldType`/`isMethodType` exist for the same reason -- comparing the raw string
// against a `ClassDesc`'s descriptor avoids building the descriptor on the other side.
public interface Utf8Entry extends CharSequence, AnnotationConstantValueEntry {

    /** The contents as a `String`. */
    String stringValue();

    /** Whether the contents are exactly `s`, without building the intermediate `String`. */
    boolean equalsString(String s);

    /** Whether the contents are `desc`'s field descriptor. */
    boolean isFieldType(ClassDesc desc);

    /** Whether the contents are `desc`'s method descriptor. */
    boolean isMethodType(MethodTypeDesc desc);
}
