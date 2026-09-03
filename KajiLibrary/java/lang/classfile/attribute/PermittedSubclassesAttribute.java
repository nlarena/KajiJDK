package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `PermittedSubclasses` (JVMS §4.7.31): las clases que pueden extender a una clase sellada. Que la
// lista esté en el `.class` y no sólo en el fuente es lo que hace que el sellado lo verifique la
// JVM al cargar y no el compilador de buena fe.
public interface PermittedSubclassesAttribute
        extends Attribute<PermittedSubclassesAttribute>, ClassElement {

    /** Las subclases permitidas. */
    List<ClassEntry> permittedSubclasses();

    /** El atributo con estas subclases. */
    public static PermittedSubclassesAttribute of(List<ClassEntry> permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(permittedSubclasses);
    }

    /** El atributo con estas subclases. */
    public static PermittedSubclassesAttribute of(ClassEntry... permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(TypedAttributes.listOfClasses(permittedSubclasses));
    }

    /** El atributo con estas subclases. */
    public static PermittedSubclassesAttribute ofSymbols(List<ClassDesc> permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(TypedAttributes.classEntries(permittedSubclasses));
    }

    /** El atributo con estas subclases. */
    public static PermittedSubclassesAttribute ofSymbols(ClassDesc... permittedSubclasses) {
        return TypedAttributes.permittedSubclasses(TypedAttributes.classEntries(permittedSubclasses));
    }
}
