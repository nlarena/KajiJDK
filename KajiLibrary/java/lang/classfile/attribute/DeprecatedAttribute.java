package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `Deprecated` (JVMS §4.7.15): a body-less attribute whose mere presence says the member is
// deprecated. It predates `@Deprecated` and does not replace it: `javac` emits both.
public interface DeprecatedAttribute
        extends Attribute<DeprecatedAttribute>, ClassElement, MethodElement, FieldElement {

    /** The attribute. */
    public static DeprecatedAttribute of() {
        return TypedAttributes.deprecated();
    }
}
