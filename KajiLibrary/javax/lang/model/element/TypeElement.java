package javax.lang.model.element;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.TypeElement — a class, interface, enum, record or
// annotation type declaration. The *declaration*, not the type: List and List<String> are
// two DeclaredTypes over the one TypeElement for List.
//
// getSuperclass() on an interface, and on java.lang.Object itself, is a NoType of kind NONE
// rather than null. getEnclosingElement() is the enclosing class for a nested type and the
// enclosing package for a top-level one — but a *local* or *anonymous* class is enclosed by
// the executable it appears in, which is why getNestingKind() exists as a separate question.
//
// getRecordComponents() and getPermittedSubclasses() are defaults returning an empty list,
// the same evolution trick ElementVisitor uses: records (16) and sealed types (17) could be
// added to this interface without breaking every implementation already out there.
public interface TypeElement extends Element, Parameterizable, QualifiedNameable {

    TypeMirror asType();

    List<? extends Element> getEnclosedElements();

    NestingKind getNestingKind();

    Name getQualifiedName();

    Name getSimpleName();

    TypeMirror getSuperclass();

    List<? extends TypeMirror> getInterfaces();

    List<? extends TypeParameterElement> getTypeParameters();

    // The JDK writes both bodies as `List.of()`. KajiLibrary's java.util.List is the subset
    // interface and carries no static factories, and #11 blocks calling java.util statics
    // from outside java.util anyway, so the empty list is a fresh ArrayList. Same contract
    // (an empty list), one allocation more.
    default List<? extends RecordComponentElement> getRecordComponents() {
        return new ArrayList<RecordComponentElement>();
    }

    default List<? extends TypeMirror> getPermittedSubclasses() {
        return new ArrayList<TypeMirror>();
    }

    Element getEnclosingElement();
}
