package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `PermittedSubclasses` (JVMS §4.7.31): the classes that may extend a sealed class. That the list is
// in the `.class` and not only in the source is what makes the sealing checked by the JVM at load time
// and not by the compiler in good faith.
public interface PermittedSubclassesAttribute
        extends Attribute<PermittedSubclassesAttribute>, ClassElement {

    /** The permitted subclasses. */
    List<ClassEntry> permittedSubclasses();

    /** The attribute with these subclasses. */
    public static PermittedSubclassesAttribute of(List<ClassEntry> permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(permittedSubclasses);
    }

    /** The attribute with these subclasses. */
    public static PermittedSubclassesAttribute of(ClassEntry... permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(TypedAttributes.listOfClasses(permittedSubclasses));
    }

    /** The attribute with these subclasses. */
    public static PermittedSubclassesAttribute ofSymbols(List<ClassDesc> permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(TypedAttributes.classEntries(permittedSubclasses));
    }

    /** The attribute with these subclasses. */
    public static PermittedSubclassesAttribute ofSymbols(ClassDesc... permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(TypedAttributes.classEntries(permittedSubclasses));
    }
}
