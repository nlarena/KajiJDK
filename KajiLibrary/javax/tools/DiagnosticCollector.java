package javax.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// KajiLibrary's javax.tools.DiagnosticCollector<S> — the listener you use when you do not
// want to react to diagnostics as they arrive, only to read them all once the tool is done.
// It is the whole reason DiagnosticListener is worth having as a separate interface.
public final class DiagnosticCollector<S> implements DiagnosticListener<S> {

    private List<Diagnostic<? extends S>> diagnostics;

    public DiagnosticCollector() {
        this.diagnostics = new ArrayList<Diagnostic<? extends S>>();
    }

    public void report(Diagnostic<? extends S> diagnostic) {
        Objects.requireNonNull(diagnostic);
        this.diagnostics.add(diagnostic);
    }

    // La lista real del JDK es inmodificable; aca devolvemos la interna porque
    // Collections.unmodifiableList no esta disponible de forma confiable.
    public List<Diagnostic<? extends S>> getDiagnostics() {
        return this.diagnostics;
    }
}
