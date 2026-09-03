package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.TypeAnnotation;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeInvisibleTypeAnnotations` (JVMS §4.7.20/§4.7.21): las anotaciones de TIPO
// no visibles por reflexión. A
// diferencia de las anotaciones comunes, éstas también pueden aparecer dentro del atributo `Code`,
// porque un tipo anotado puede estar en un `instanceof` o en un cast.
public interface RuntimeInvisibleTypeAnnotationsAttribute extends Attribute<RuntimeInvisibleTypeAnnotationsAttribute>,
        ClassElement, MethodElement, FieldElement, CodeElement {

    /** Las anotaciones de tipo, en el orden del archivo. */
    List<TypeAnnotation> annotations();

    /** El atributo con estas anotaciones. */
    public static RuntimeInvisibleTypeAnnotationsAttribute of(List<TypeAnnotation> annotations) {
        return TypedAttributes.runtimeInvisibleTypeAnnotations(annotations);
    }

    /** El atributo con estas anotaciones. */
    public static RuntimeInvisibleTypeAnnotationsAttribute of(TypeAnnotation... annotations) {
        return TypedAttributes.runtimeInvisibleTypeAnnotations(TypedAttributes.listOfTypeAnnotations(annotations));
    }
}
