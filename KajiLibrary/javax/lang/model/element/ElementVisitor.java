package javax.lang.model.element;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

// KajiLibrary's javax.lang.model.element.ElementVisitor<R,P> — the visitor over program
// elements. R is what a visit produces, P the extra argument threaded through it; use Void
// for either when there is nothing to carry.
//
// visitModule and visitRecordComponent are default methods, not abstract ones, and that is
// the whole story of how this interface evolves without breaking visitors: modules arrived
// in 9 and record components in 16, and each was added with a default that routes to
// visitUnknown, so a visitor written before them still compiles and still lands somewhere
// sensible. New element kinds in future releases go the same way, which is why
// visitUnknown exists at all — the convention is to throw UnknownElementException from it.
public interface ElementVisitor<R, P> {

    R visit(Element e, P p);

    default R visit(Element e) {
        return visit(e, null);
    }

    R visitPackage(PackageElement e, P p);

    R visitType(TypeElement e, P p);

    R visitVariable(VariableElement e, P p);

    R visitExecutable(ExecutableElement e, P p);

    R visitTypeParameter(TypeParameterElement e, P p);

    R visitUnknown(Element e, P p);

    default R visitModule(ModuleElement e, P p) {
        return visitUnknown(e, p);
    }

    default R visitRecordComponent(RecordComponentElement e, P p) {
        return visitUnknown(e, p);
    }
}
