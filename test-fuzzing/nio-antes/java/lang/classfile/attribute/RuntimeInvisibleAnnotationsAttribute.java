package java.lang.classfile.attribute;

import java.lang.classfile.Annotation;
import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeInvisibleAnnotations` (JVMS §4.7.16/§4.7.17): the annotations NOT visible by reflection.
// That is the only difference between the two tables: the annotation's format is the same, and who
// sees it is decided by which attribute it is in, not by the annotation.
public interface RuntimeInvisibleAnnotationsAttribute
        extends Attribute<RuntimeInvisibleAnnotationsAttribute>, ClassElement, MethodElement, FieldElement {

    /** The annotations, in file order. */
    List<Annotation> annotations();

    /** The attribute with these annotations. */
    public static RuntimeInvisibleAnnotationsAttribute of(List<Annotation> annotations) {
        return TypedAttributes.runtimeInvisibleAnnotations(annotations);
    }

    /** The attribute with these annotations. */
    public static RuntimeInvisibleAnnotationsAttribute of(Annotation... annotations) {
        return TypedAttributes.runtimeInvisibleAnnotations(TypedAttributes.listOfAnnotations(annotations));
    }
}
