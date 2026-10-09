package javax.annotation.processing;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

// Where a processor **reports** (JSR 269 §Messager). It exists so that a processor does not print on
// its own: the tool needs to see the messages in order to count them, place them in the source and,
// above all, so that a `Kind.ERROR` makes the compilation fail. A `System.out.println` does none of
// that.
//
// The four abstract overloads are the same operation with more and more context: the message alone,
// the message on an element, on an annotation of that element, and on a value of that annotation.
// More context = the underline lands in a more precise place.
public interface Messager {

    /** A message with no location. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg);

    /** A message located at `e`. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e);

    /** A message located at `e`'s annotation `a`. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a);

    /** A message located at value `v` of `e`'s annotation `a`. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a,
            AnnotationValue v);

    // The six shortcuts below are `default` in the contract, not methods of a utility class: they
    // resolve against `this`, so an implementor that writes only the four above already has them, and
    // one that wants to count them separately can override them.

    /** Shortcut for {@code printMessage(Kind.ERROR, msg)}. */
    default void printError(CharSequence msg) {
        this.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    /** Shortcut for {@code printMessage(Kind.ERROR, msg, e)}. */
    default void printError(CharSequence msg, Element e) {
        this.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    /** Shortcut for {@code printMessage(Kind.WARNING, msg)}. */
    default void printWarning(CharSequence msg) {
        this.printMessage(Diagnostic.Kind.WARNING, msg);
    }

    /** Shortcut for {@code printMessage(Kind.WARNING, msg, e)}. */
    default void printWarning(CharSequence msg, Element e) {
        this.printMessage(Diagnostic.Kind.WARNING, msg, e);
    }

    /** Shortcut for {@code printMessage(Kind.NOTE, msg)}. */
    default void printNote(CharSequence msg) {
        this.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    /** Shortcut for {@code printMessage(Kind.NOTE, msg, e)}. */
    default void printNote(CharSequence msg, Element e) {
        this.printMessage(Diagnostic.Kind.NOTE, msg, e);
    }
}
