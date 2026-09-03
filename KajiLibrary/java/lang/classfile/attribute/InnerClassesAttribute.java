package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `InnerClasses` (JVMS §4.7.6): la tabla de todas las clases anidadas que este archivo menciona,
// sean suyas o ajenas. Que también estén las ajenas es lo que hace que una clase que usa
// `Map.Entry` lleve una fila para `Map.Entry` sin tener nada anidado propio.
public interface InnerClassesAttribute extends Attribute<InnerClassesAttribute>, ClassElement {

    /** Las filas, en el orden del archivo. */
    List<InnerClassInfo> classes();

    /** El atributo con estas filas. */
    public static InnerClassesAttribute of(List<InnerClassInfo> innerClasses) {
        return TypedAttributes.innerClasses(innerClasses);
    }

    /** El atributo con estas filas. */
    public static InnerClassesAttribute of(InnerClassInfo... innerClasses) {
        return TypedAttributes.innerClasses(TypedAttributes.listOf(innerClasses));
    }
}
