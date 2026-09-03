package java.lang.classfile.attribute;

import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `AnnotationDefault` (JVMS §4.7.22): el valor por omisión de un elemento de un tipo de anotación.
// Vive en el MÉTODO del tipo de anotación, no en el sitio anotado: por eso un sitio que no menciona
// el elemento no guarda nada, y quien quiera el valor efectivo tiene que ir a buscar este atributo
// al `.class` de la anotación.
public interface AnnotationDefaultAttribute
        extends Attribute<AnnotationDefaultAttribute>, MethodElement {

    /** El valor por omisión. */
    AnnotationValue defaultValue();

    /** El atributo con este valor. */
    public static AnnotationDefaultAttribute of(AnnotationValue annotationDefault) {
        return TypedAttributes.annotationDefault(annotationDefault);
    }
}
