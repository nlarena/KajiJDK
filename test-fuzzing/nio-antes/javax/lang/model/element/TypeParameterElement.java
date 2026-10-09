package javax.lang.model.element;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.TypeParameterElement — the declaration of a formal
// type parameter of a generic class, interface, method or constructor.
//
// getBounds() returns the bounds as written; a parameter declared with no `extends` clause
// reports an empty list, not a one-element list holding Object. getGenericElement() and
// getEnclosingElement() both point back at the declaring element and agree with each other.
public interface TypeParameterElement extends Element {

    TypeMirror asType();

    Element getGenericElement();

    List<? extends TypeMirror> getBounds();

    Element getEnclosingElement();
}
