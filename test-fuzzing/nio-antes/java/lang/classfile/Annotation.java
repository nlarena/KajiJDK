package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.Annotations;

// An annotation just as the format stores it (JVMS §4.7.16, `annotation`): the annotated type's
// descriptor and the list of name-value pairs. It is not a `java.lang.annotation.Annotation` -- there
// is no loaded class here and no resolved default values, only what is written in the file. An
// element the annotation declares with a default value and the site does not mention simply does NOT
// show up in `elements()`; the default value lives in the `AnnotationDefault` of the annotation type's
// method, which is another file.
public interface Annotation {

    /** The `Utf8` with the annotation type's descriptor (`Ljava/lang/Deprecated;`). */
    Utf8Entry className();

    /** The annotation's type. */
    default ClassDesc classSymbol() {
        return ClassDesc.ofDescriptor(className().stringValue());
    }

    /** The name-value pairs, in file order. */
    List<AnnotationElement> elements();

    /** An annotation of this type with these elements. */
    public static Annotation of(Utf8Entry annotationClass, List<AnnotationElement> elements) {
        return Annotations.annotation(annotationClass, elements);
    }

    /** An annotation of this type with these elements. */
    public static Annotation of(Utf8Entry annotationClass, AnnotationElement... elements) {
        return Annotations.annotation(annotationClass, Annotations.listOf(elements));
    }

    /** An annotation of this type with these elements. */
    public static Annotation of(ClassDesc annotationClass, List<AnnotationElement> elements) {
        return Annotations.annotation(Annotations.utf8(descriptorOf(annotationClass)), elements);
    }

    /** An annotation of this type with these elements. */
    public static Annotation of(ClassDesc annotationClass, AnnotationElement... elements) {
        return Annotations.annotation(Annotations.utf8(descriptorOf(annotationClass)),
                Annotations.listOf(elements));
    }

    private static String descriptorOf(ClassDesc desc) {
        if (desc == null) {
            throw new NullPointerException("annotationClass");
        }
        return desc.descriptorString();
    }
}
