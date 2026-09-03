package java.lang.classfile.attribute;

import java.lang.classfile.Annotation;
import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeVisibleAnnotations` (JVMS §4.7.16/§4.7.17): las anotaciones visibles en
// ejecución por reflexión. La diferencia entre las dos
// tablas es sólo esa: el formato de la anotación es el mismo, y quién la ve lo decide en qué
// atributo está, no la anotación.
public interface RuntimeVisibleAnnotationsAttribute
        extends Attribute<RuntimeVisibleAnnotationsAttribute>, ClassElement, MethodElement, FieldElement {

    /** Las anotaciones, en el orden del archivo. */
    List<Annotation> annotations();

    /** El atributo con estas anotaciones. */
    public static RuntimeVisibleAnnotationsAttribute of(List<Annotation> annotations) {
        return TypedAttributes.runtimeVisibleAnnotations(annotations);
    }

    /** El atributo con estas anotaciones. */
    public static RuntimeVisibleAnnotationsAttribute of(Annotation... annotations) {
        return TypedAttributes.runtimeVisibleAnnotations(TypedAttributes.listOfAnnotations(annotations));
    }
}
