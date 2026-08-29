package javax.lang.model.element;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeParameterElement;

// KajiLibrary's javax.lang.model.element.Parameterizable — the mixin for elements that may
// declare type parameters of their own: classes and interfaces (TypeElement) and methods
// and constructors (ExecutableElement).
public interface Parameterizable extends Element {

    List<? extends TypeParameterElement> getTypeParameters();
}
