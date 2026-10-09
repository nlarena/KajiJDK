package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.TypeAnnotation;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeInvisibleTypeAnnotations` (JVMS §4.7.20/§4.7.21): the TYPE annotations not visible by
// reflection. Unlike ordinary annotations, these can also appear inside the `Code` attribute, because
// an annotated type can be in an `instanceof` or in a cast.
public interface RuntimeInvisibleTypeAnnotationsAttribute extends Attribute<RuntimeInvisibleTypeAnnotationsAttribute>,
        ClassElement, MethodElement, FieldElement, CodeElement {

    /** The type annotations, in file order. */
    List<TypeAnnotation> annotations();

    /** The attribute with these annotations. */
    public static RuntimeInvisibleTypeAnnotationsAttribute of(List<TypeAnnotation> annotations) {
        return TypedAttributes.runtimeInvisibleTypeAnnotations(annotations);
    }

    /** The attribute with these annotations. */
    public static RuntimeInvisibleTypeAnnotationsAttribute of(TypeAnnotation... annotations) {
        return TypedAttributes.runtimeInvisibleTypeAnnotations(TypedAttributes.listOfTypeAnnotations(annotations));
    }
}
