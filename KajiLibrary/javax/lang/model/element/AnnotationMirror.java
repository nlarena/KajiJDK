package javax.lang.model.element;

import java.util.Map;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;

// KajiLibrary's javax.lang.model.element.AnnotationMirror — one annotation *as written on a
// declaration*, modelled rather than reflected. Unlike java.lang.annotation.Annotation it
// never loads the annotation class, so it can describe an annotation whose type is not on
// the runtime classpath, or is not even compiled yet.
//
// getElementValues() returns only the pairs actually present in the source or classfile;
// elements left to their declared default are absent. (Elements.getElementValuesWithDefaults
// is the view that fills them in.)
//
// Not an Element and not comparable with equals(): the JDK explicitly leaves equality
// unspecified here.
public interface AnnotationMirror {

    DeclaredType getAnnotationType();

    Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues();
}
