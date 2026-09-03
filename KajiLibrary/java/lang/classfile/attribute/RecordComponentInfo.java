package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// Un componente de un `record` (JVMS §4.7.30). Lleva atributos propios —`Signature`, las de
// anotaciones— y por eso es un {@link AttributedElement} y no una tupla: las anotaciones que se
// escriben sobre el componente en el fuente terminan acá, además de en el campo y en el accesor.
public interface RecordComponentInfo extends AttributedElement {

    /** El nombre del componente. */
    Utf8Entry name();

    /** El descriptor de su tipo. */
    Utf8Entry descriptor();

    /** Su tipo. */
    default ClassDesc descriptorSymbol() {
        return ClassDesc.ofDescriptor(descriptor().stringValue());
    }

    /** El componente con estos valores. */
    public static RecordComponentInfo of(Utf8Entry name, Utf8Entry descriptor,
            List<Attribute<?>> attributes) {
        return TypedAttributes.recordComponentInfo(name, descriptor, attributes);
    }

    /** El componente con estos valores. */
    public static RecordComponentInfo of(Utf8Entry name, Utf8Entry descriptor,
            Attribute<?>... attributes) {
        return TypedAttributes.recordComponentInfo(name, descriptor,
                TypedAttributes.listOfAttributes(attributes));
    }

    /** El componente con estos valores. */
    public static RecordComponentInfo of(String name, ClassDesc descriptor,
            List<Attribute<?>> attributes) {
        return TypedAttributes.recordComponentInfo(TypedAttributes.utf8(name),
                TypedAttributes.utf8(descriptor.descriptorString()), attributes);
    }

    /** El componente con estos valores. */
    public static RecordComponentInfo of(String name, ClassDesc descriptor,
            Attribute<?>... attributes) {
        return TypedAttributes.recordComponentInfo(TypedAttributes.utf8(name),
                TypedAttributes.utf8(descriptor.descriptorString()),
                TypedAttributes.listOfAttributes(attributes));
    }
}
