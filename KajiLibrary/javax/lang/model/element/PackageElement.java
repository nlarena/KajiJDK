package javax.lang.model.element;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.PackageElement — a package.
//
// asType() gives a NoType of kind PACKAGE: a package is a declaration without a type, and
// the model says so out loud instead of returning null. getEnclosedElements() lists the
// top-level classes and interfaces of the package, not its subpackages — package nesting is
// a naming convention, not a containment relation.
//
// The unnamed package reports isUnnamed() true and an empty qualified name.
public interface PackageElement extends Element, QualifiedNameable {

    TypeMirror asType();

    Name getQualifiedName();

    Name getSimpleName();

    List<? extends Element> getEnclosedElements();

    boolean isUnnamed();

    Element getEnclosingElement();
}
