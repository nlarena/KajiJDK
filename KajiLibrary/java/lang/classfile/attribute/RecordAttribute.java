package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `Record` (JVMS §4.7.30): a `record`'s list of components. Its presence is what makes the JVM treat
// the class as a record; the access bit is not enough.
public interface RecordAttribute extends Attribute<RecordAttribute>, ClassElement {

    /** The components, in declaration order. */
    List<RecordComponentInfo> components();

    /** The attribute with these components. */
    public static RecordAttribute of(List<RecordComponentInfo> components) {
        return TypedAttributes.record(components);
    }

    /** The attribute with these components. */
    public static RecordAttribute of(RecordComponentInfo... components) {
        return TypedAttributes.record(TypedAttributes.listOf(components));
    }
}
