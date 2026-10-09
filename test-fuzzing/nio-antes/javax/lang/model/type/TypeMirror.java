package javax.lang.model.type;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;

public interface TypeMirror extends AnnotatedConstruct {
    TypeKind getKind();

    boolean equals(Object obj);

    int hashCode();

    String toString();

    List<? extends AnnotationMirror> getAnnotationMirrors();

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType);

    <R, P> R accept(TypeVisitor<R, P> v, P p);
}
