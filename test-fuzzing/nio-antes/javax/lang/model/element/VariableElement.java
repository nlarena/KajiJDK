package javax.lang.model.element;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.VariableElement — a field, an enum constant, a
// method or constructor parameter, a local variable, a resource variable, a binding
// variable from a pattern, or an exception parameter. One interface for all of them; the
// ElementKind tells them apart.
//
// getConstantValue() is non-null only for a variable that actually *is* a compile-time
// constant: a final field of primitive or String type with a constant initializer. Anything
// else — including a final field initialised to a constant expression of the wrong type —
// gives null.
public interface VariableElement extends Element {

    TypeMirror asType();

    Object getConstantValue();

    Name getSimpleName();

    Element getEnclosingElement();

    // The JDK writes this as `getSimpleName().isEmpty()`. The note said KajiLibrary's
    // java.lang.CharSequence had no isEmpty(); it has one now (a default method). The test goes
    // through length(), which gives the same answer.
    default boolean isUnnamed() {
        return getSimpleName().length() == 0;
    }
}
