package javax.lang.model.element;

import javax.lang.model.element.AnnotationValueVisitor;

// KajiLibrary's javax.lang.model.element.AnnotationValue — the value of one element of an
// annotation instance. getValue() hands back a boxed form whose runtime class tells you
// what the value is:
//
//   a wrapper (Boolean/Byte/…/Double)  a primitive
//   String                             a string
//   TypeMirror                         a class literal
//   VariableElement                    an enum constant
//   AnnotationMirror                   a nested annotation
//   List<? extends AnnotationValue>    an array, element by element
//
// accept() is the type-safe way to ask the same question without a chain of instanceof.
public interface AnnotationValue {

    Object getValue();

    String toString();

    <R, P> R accept(AnnotationValueVisitor<R, P> v, P p);
}
