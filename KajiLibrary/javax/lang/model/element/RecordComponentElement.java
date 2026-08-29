package javax.lang.model.element;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.RecordComponentElement — one component of a record
// declaration. getAccessor() is the method the record generates (or the one the programmer
// wrote explicitly to override it) that reads this component.
public interface RecordComponentElement extends Element {

    TypeMirror asType();

    Element getEnclosingElement();

    Name getSimpleName();

    ExecutableElement getAccessor();
}
