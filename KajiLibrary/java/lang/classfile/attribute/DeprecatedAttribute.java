package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `Deprecated` (JVMS §4.7.15): un atributo sin cuerpo, cuya sola presencia dice que el miembro está
// obsoleto. Es anterior a `@Deprecated` y no lo reemplaza: `javac` emite los dos.
public interface DeprecatedAttribute
        extends Attribute<DeprecatedAttribute>, ClassElement, MethodElement, FieldElement {

    /** El atributo. */
    public static DeprecatedAttribute of() {
        return TypedAttributes.deprecated();
    }
}
