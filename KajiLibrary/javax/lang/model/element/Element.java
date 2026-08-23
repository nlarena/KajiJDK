package javax.lang.model.element;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.Element — a declaration in a program: a module, a
// package, a class or interface, a method or constructor, a field, a parameter, a type
// variable. It is the *declaration* side of the language model; asType() crosses over to
// the type side (javax.lang.model.type).
//
// Elements are compared with equals(), never with ==: an implementation is free to hand
// back a different object for the same declaration on each call, so identity means nothing.
// accept() is the visitor hook — dispatch on the kind of element without a chain of
// instanceof tests.
//
// Two deliberate departures from the JDK's declaration, both because of the frozen javac:
//
// One deliberate departure from the JDK's declaration: the JDK also re-declares
// getAnnotationsByType here, and we cannot. That would be overriding a method whose return
// type is an array of a type outside java.lang, which the frozen javac rejects (#211). It is
// inherited from AnnotatedConstruct instead, so the effective member set is unchanged — only
// the javadoc anchor is lost. The other four re-declarations (equals, hashCode,
// getAnnotationMirrors, getAnnotation) are spelled out as in the JDK.
public interface Element extends AnnotatedConstruct {

    boolean equals(Object obj);

    int hashCode();

    List<? extends AnnotationMirror> getAnnotationMirrors();

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    TypeMirror asType();

    ElementKind getKind();

    Set<Modifier> getModifiers();

    Name getSimpleName();

    Element getEnclosingElement();

    List<? extends Element> getEnclosedElements();

    <R, P> R accept(ElementVisitor<R, P> v, P p);
}
