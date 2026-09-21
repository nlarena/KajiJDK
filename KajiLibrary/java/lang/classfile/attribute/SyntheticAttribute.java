package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `Synthetic` (JVMS §4.7.8): the member is not in the source, the compiler invented it. Today it is
// almost always marked with the `ACC_SYNTHETIC` bit instead of with this attribute, but old classes
// use the attribute and one has to be able to read them.
public interface SyntheticAttribute
        extends Attribute<SyntheticAttribute>, ClassElement, MethodElement, FieldElement {

    /** The attribute. */
    public static SyntheticAttribute of() {
        return TypedAttributes.synthetic();
    }
}
