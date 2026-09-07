package javax.annotation.processing;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

// This project's round-loop Messager: it writes through the same native bridge AptTrace uses, which
// is the interpreter's console and what `AptOutcome.console` ends up capturing. That way a processor
// can report without `System.out.println` (which does not compile yet in this javac).
//
// The format is `KIND: message`, and when there is context ` at <element>` is appended — no more than
// that: this implementation **cannot** underline a position in the source, because the round loop
// passes it neither the compilation unit nor the positions. Putting an invented line number would be
// worse than putting none.
//
// What this Messager does NOT do, and it is worth knowing: a `Kind.ERROR` does **not** make the
// compilation fail. In the real JDK that is the main effect of reporting an error; here the round
// loop does not look at the messages, so an error is a line on the console and nothing more. It is
// the reason `RoundEnvironmentImpl.errorRaised()` can return `false` with a clear conscience.
class AptMessager implements Messager {

    // The bridge: `AptTrace.trace` is the native that already exists so that a processor can print.
    private static void emit(Diagnostic.Kind kind, CharSequence msg, String where) {
        String text = kind.toString() + ": " + String.valueOf(msg);
        if (where != null) {
            text = text + " at " + where;
        }
        AptTrace.trace(text);
    }

    // An element's name for the message, or null if there is no element. `toString()` is used and not
    // `getSimpleName()` because element reification is partial and `toString` is the only thing every
    // Element here answers.
    private static String name(Element e) {
        if (e == null) {
            return null;
        }
        return e.toString();
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
        emit(kind, msg, null);
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
        emit(kind, msg, name(e));
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
        emit(kind, msg, name(e));
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a,
            AnnotationValue v) {
        emit(kind, msg, name(e));
    }
}
