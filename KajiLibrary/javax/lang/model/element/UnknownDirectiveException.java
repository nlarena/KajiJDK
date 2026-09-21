package javax.lang.model.element;

import javax.lang.model.UnknownEntityException;
import javax.lang.model.element.ModuleElement;
// The note said nested types must be single-type-imported because the frozen javac could not
// resolve a member type through its outer class (`ModuleElement.Directive`) when the outer
// class comes from the classpath. That no longer reproduces: `ModuleElement.Directive` written
// through the outer class compiles with the frozen javac (checked 2026-09-18).

// KajiLibrary's javax.lang.model.element.UnknownDirectiveException — the
// ModuleElement.DirectiveVisitor counterpart of UnknownElementException, thrown from its
// visitUnknown when the visitor meets a directive kind it does not know.
public class UnknownDirectiveException extends UnknownEntityException {

    private static final long serialVersionUID = 269L;

    private final transient ModuleElement.Directive directive;
    private final transient Object parameter;

    // String.valueOf() stays, but its reasons are gone. The note said that concatenating an
    // operand whose static type is a cross-file nested type made the frozen javac report an
    // ambiguous `append`, and that StringBuilder had no append(Object), so the concat was emitted
    // as *nothing* (#114). StringBuilder has append(Object) now, #114 is closed, and the frozen
    // javac lowers `"x" + d` with a nested-type `d` through invokedynamic (checked 2026-09-18).
    // The message is the same either way.
    public UnknownDirectiveException(ModuleElement.Directive d, Object p) {
        super("Unknown directive: \"" + String.valueOf(d) + "\"");
        directive = d;
        parameter = p;
    }

    public ModuleElement.Directive getUnknownDirective() {
        return directive;
    }

    public Object getArgument() {
        return parameter;
    }
}
