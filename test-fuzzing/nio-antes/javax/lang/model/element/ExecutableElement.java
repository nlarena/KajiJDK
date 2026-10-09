package javax.lang.model.element;

import java.util.List;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.ExecutableElement — a method, a constructor, or a
// class/instance initializer. Parameterizable because methods and constructors may declare
// type parameters of their own.
//
// getReceiverType() is the type of the explicit receiver parameter (the `Foo this` form) if
// the executable has one; for a static method, or a constructor of a top-level class, or a
// declaration that simply omits it, the answer is a NoType of kind NONE rather than null.
//
// getDefaultValue() is non-null only for an annotation type element with a `default` clause.
public interface ExecutableElement extends Element, Parameterizable {

    TypeMirror asType();

    List<? extends TypeParameterElement> getTypeParameters();

    TypeMirror getReturnType();

    List<? extends VariableElement> getParameters();

    TypeMirror getReceiverType();

    boolean isVarArgs();

    boolean isDefault();

    List<? extends TypeMirror> getThrownTypes();

    AnnotationValue getDefaultValue();

    Element getEnclosingElement();

    Name getSimpleName();
}
