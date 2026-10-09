package java.lang.classfile.attribute;

import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `AnnotationDefault` (JVMS §4.7.22): the default value of an annotation type's element. It lives on
// the annotation type's METHOD, not on the annotated site: that is why a site not mentioning the
// element stores nothing, and whoever wants the effective value has to go look this attribute up in
// the annotation's `.class`.
public interface AnnotationDefaultAttribute
        extends Attribute<AnnotationDefaultAttribute>, MethodElement {

    /** The default value. */
    AnnotationValue defaultValue();

    /** The attribute with this value. */
    public static AnnotationDefaultAttribute of(AnnotationValue annotationDefault) {
        return TypedAttributes.annotationDefault(annotationDefault);
    }
}
