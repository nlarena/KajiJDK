package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `InnerClasses` (JVMS §4.7.6): the table of every nested class this file mentions, its own or
// someone else's. That the other ones are there too is what makes a class using `Map.Entry` carry a
// row for `Map.Entry` without having anything nested of its own.
public interface InnerClassesAttribute extends Attribute<InnerClassesAttribute>, ClassElement {

    /** The rows, in file order. */
    List<InnerClassInfo> classes();

    /** The attribute with these rows. */
    public static InnerClassesAttribute of(List<InnerClassInfo> innerClasses) {
        return TypedAttributes.innerClasses(innerClasses);
    }

    /** The attribute with these rows. */
    public static InnerClassesAttribute of(InnerClassInfo... innerClasses) {
        return TypedAttributes.innerClasses(TypedAttributes.listOf(innerClasses));
    }
}
