package javax.tools;

// KajiLibrary's javax.tools.DiagnosticListener<S> — the callback a tool uses to hand back
// its diagnostics one at a time instead of printing them. S is the source type the
// diagnostics point at; the parameter is `? extends S` because a listener for a general
// source type must accept diagnostics about any more specific one.
public interface DiagnosticListener<S> {

    void report(Diagnostic<? extends S> diagnostic);
}
