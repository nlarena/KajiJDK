package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.Annotations;

// Una anotación tal como el formato la guarda (JVMS §4.7.16, `annotation`): el descriptor del tipo
// anotado y la lista de pares nombre-valor. No es una `java.lang.annotation.Annotation` — acá no hay
// clase cargada ni valores por omisión resueltos, sólo lo que está escrito en el archivo. Un
// elemento que la anotación declara con valor por omisión y el sitio no menciona simplemente NO
// aparece en `elements()`; el valor por omisión vive en el `AnnotationDefault` del método del tipo
// de anotación, que es otro archivo.
public interface Annotation {

    /** El `Utf8` con el descriptor del tipo de la anotación (`Ljava/lang/Deprecated;`). */
    Utf8Entry className();

    /** El tipo de la anotación. */
    default ClassDesc classSymbol() {
        return ClassDesc.ofDescriptor(className().stringValue());
    }

    /** Los pares nombre-valor, en el orden del archivo. */
    List<AnnotationElement> elements();

    /** Una anotación de este tipo con estos elementos. */
    public static Annotation of(Utf8Entry annotationClass, List<AnnotationElement> elements) {
        return Annotations.annotation(annotationClass, elements);
    }

    /** Una anotación de este tipo con estos elementos. */
    public static Annotation of(Utf8Entry annotationClass, AnnotationElement... elements) {
        return Annotations.annotation(annotationClass, Annotations.listOf(elements));
    }

    /** Una anotación de este tipo con estos elementos. */
    public static Annotation of(ClassDesc annotationClass, List<AnnotationElement> elements) {
        return Annotations.annotation(Annotations.utf8(descriptorOf(annotationClass)), elements);
    }

    /** Una anotación de este tipo con estos elementos. */
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
