package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.FieldElement;
import java.lang.classfile.constantpool.ConstantValueEntry;
import java.lang.constant.ConstantDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// `ConstantValue` (JVMS §4.7.2): the value of a `static final` field of primitive or `String` type.
// The JVM assigns it when initialising the class, BEFORE running `<clinit>`, and that is why a field
// carrying this attribute can be read even though the static initialiser has not run yet.
public interface ConstantValueAttribute extends Attribute<ConstantValueAttribute>, FieldElement {

    /** The pool entry holding the value. */
    ConstantValueEntry constant();

    /** The attribute with this value. */
    public static ConstantValueAttribute of(ConstantValueEntry value) {
        return TypedAttributes.constantValue(value);
    }

    /** The attribute with this value. */
    public static ConstantValueAttribute of(ConstantDesc value) {
        return TypedAttributes.constantValue(TypedAttributes.constantValueEntry(value));
    }
}
