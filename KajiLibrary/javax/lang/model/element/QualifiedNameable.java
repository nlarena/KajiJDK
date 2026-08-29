package javax.lang.model.element;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;

// KajiLibrary's javax.lang.model.element.QualifiedNameable — the mixin for elements that
// have a fully qualified name on top of their simple one: modules, packages, and classes
// and interfaces.
public interface QualifiedNameable extends Element {

    Name getQualifiedName();
}
