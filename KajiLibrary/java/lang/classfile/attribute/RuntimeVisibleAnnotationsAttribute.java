package java.lang.classfile.attribute;

import java.lang.classfile.Annotation;
import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeVisibleAnnotations` (JVMS §4.7.16/§4.7.17): the annotations visible at run time by
// reflection. That is the only difference between the two tables: the annotation's format is the
// same, and who sees it is decided by which attribute it is in, not by the annotation.
public interface RuntimeVisibleAnnotationsAttribute
        extends Attribute<RuntimeVisibleAnnotationsAttribute>, ClassElement, MethodElement, FieldElement {

    /** The annotations, in file order. */
    List<Annotation> annotations();

    /** The attribute with these annotations. */
    public static RuntimeVisibleAnnotationsAttribute of(List<Annotation> annotations) {
        return TypedAttributes.runtimeVisibleAnnotations(annotations);
    }

    /** The attribute with these annotations. */
    public static RuntimeVisibleAnnotationsAttribute of(Annotation... annotations) {
        return TypedAttributes.runtimeVisibleAnnotations(TypedAttributes.listOfAnnotations(annotations));
    }
}
