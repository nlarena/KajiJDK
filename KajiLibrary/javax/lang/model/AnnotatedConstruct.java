package javax.lang.model;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;

public interface AnnotatedConstruct {
    List<? extends AnnotationMirror> getAnnotationMirrors();

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType);
}
