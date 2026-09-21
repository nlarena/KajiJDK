package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// A component of a `record` (JVMS §4.7.30). It carries attributes of its own --`Signature`, the
// annotation ones-- and that is why it is an {@link AttributedElement} and not a tuple: the
// annotations written on the component in the source end up here, besides on the field and on the
// accessor.
public interface RecordComponentInfo extends AttributedElement {

    /** The component's name. */
    Utf8Entry name();

    /** Its type's descriptor. */
    Utf8Entry descriptor();

    /** Its type. */
    default ClassDesc descriptorSymbol() {
        return ClassDesc.ofDescriptor(descriptor().stringValue());
    }

    /** The component with these values. */
    public static RecordComponentInfo of(Utf8Entry name, Utf8Entry descriptor,
            List<Attribute<?>> attributes) {
        return TypedAttributes.recordComponentInfo(name, descriptor, attributes);
    }

    /** The component with these values. */
    public static RecordComponentInfo of(Utf8Entry name, Utf8Entry descriptor,
            Attribute<?>... attributes) {
        return TypedAttributes.recordComponentInfo(name, descriptor,
                TypedAttributes.listOfAttributes(attributes));
    }

    /** The component with these values. */
    public static RecordComponentInfo of(String name, ClassDesc descriptor,
            List<Attribute<?>> attributes) {
        return TypedAttributes.recordComponentInfo(TypedAttributes.utf8(name),
                TypedAttributes.utf8(descriptor.descriptorString()), attributes);
    }

    /** The component with these values. */
    public static RecordComponentInfo of(String name, ClassDesc descriptor,
            Attribute<?>... attributes) {
        return TypedAttributes.recordComponentInfo(TypedAttributes.utf8(name),
                TypedAttributes.utf8(descriptor.descriptorString()),
                TypedAttributes.listOfAttributes(attributes));
    }
}
