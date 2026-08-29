package javax.lang.model.element;

import javax.lang.model.UnknownEntityException;
import javax.lang.model.element.ModuleElement;
// Nested types must be single-type-imported: the frozen javac cannot resolve a member type
// through its outer class (`ModuleElement.Directive`) when the outer class comes from the
// classpath — see the ZOuter/ZUseA repro in this session's report.

// KajiLibrary's javax.lang.model.element.UnknownDirectiveException — the
// ModuleElement.DirectiveVisitor counterpart of UnknownElementException, thrown from its
// visitUnknown when the visitor meets a directive kind it does not know.
public class UnknownDirectiveException extends UnknownEntityException {

    private static final long serialVersionUID = 269L;

    private final transient ModuleElement.Directive directive;
    private final transient Object parameter;

    // String.valueOf() is load-bearing, twice over. The JDK writes plain `"…" + d`, but
    // (a) concatenating an operand whose static type is a cross-file nested type makes the
    // frozen javac report "la referencia a `append` es ambigua", and (b) StringBuilder has no
    // append(Object), so a reference operand hits #114 and the whole concat is emitted as
    // *nothing* — leaving a super() call with an empty operand stack. Forcing the operand to
    // String selects append(String) and produces the same message the JDK produces.
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
