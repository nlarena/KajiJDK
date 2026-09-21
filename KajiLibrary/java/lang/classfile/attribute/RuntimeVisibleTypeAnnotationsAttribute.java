package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.TypeAnnotation;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeVisibleTypeAnnotations` (JVMS §4.7.20/§4.7.21): the TYPE annotations visible by reflection.
// Unlike ordinary annotations, these can also appear inside the `Code` attribute, because an annotated
// type can be in an `instanceof` or in a cast.
public interface RuntimeVisibleTypeAnnotationsAttribute extends Attribute<RuntimeVisibleTypeAnnotationsAttribute>,
        ClassElement, MethodElement, FieldElement, CodeElement {

    /** The type annotations, in file order. */
    List<TypeAnnotation> annotations();

    /** The attribute with these annotations. */
    public static RuntimeVisibleTypeAnnotationsAttribute of(List<TypeAnnotation> annotations) {
        return TypedAttributes.runtimeVisibleTypeAnnotations(annotations);
    }

    /** The attribute with these annotations. */
    public static RuntimeVisibleTypeAnnotationsAttribute of(TypeAnnotation... annotations) {
        return TypedAttributes.runtimeVisibleTypeAnnotations(TypedAttributes.listOfTypeAnnotations(annotations));
    }
}
