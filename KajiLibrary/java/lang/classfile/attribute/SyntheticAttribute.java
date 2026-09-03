package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `Synthetic` (JVMS §4.7.8): el miembro no está en el fuente, lo inventó el compilador. Hoy casi
// siempre se marca con el bit `ACC_SYNTHETIC` en vez de con este atributo, pero las clases viejas
// usan el atributo y hay que poder leerlas.
public interface SyntheticAttribute
        extends Attribute<SyntheticAttribute>, ClassElement, MethodElement, FieldElement {

    /** El atributo. */
    public static SyntheticAttribute of() {
        return TypedAttributes.synthetic();
    }
}
