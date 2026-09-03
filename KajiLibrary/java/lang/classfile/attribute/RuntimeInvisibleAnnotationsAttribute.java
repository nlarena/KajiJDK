package java.lang.classfile.attribute;

import java.lang.classfile.Annotation;
import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeInvisibleAnnotations` (JVMS §4.7.16/§4.7.17): las anotaciones NO visibles
// por reflexión. La diferencia entre las dos
// tablas es sólo esa: el formato de la anotación es el mismo, y quién la ve lo decide en qué
// atributo está, no la anotación.
public interface RuntimeInvisibleAnnotationsAttribute
        extends Attribute<RuntimeInvisibleAnnotationsAttribute>, ClassElement, MethodElement, FieldElement {

    /** Las anotaciones, en el orden del archivo. */
    List<Annotation> annotations();

    /** El atributo con estas anotaciones. */
    public static RuntimeInvisibleAnnotationsAttribute of(List<Annotation> annotations) {
        return TypedAttributes.runtimeInvisibleAnnotations(annotations);
    }

    /** El atributo con estas anotaciones. */
    public static RuntimeInvisibleAnnotationsAttribute of(Annotation... annotations) {
        return TypedAttributes.runtimeInvisibleAnnotations(TypedAttributes.listOfAnnotations(annotations));
    }
}
