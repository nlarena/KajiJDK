package javax.annotation.processing;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

// Por donde un procesador **reporta** (JSR 269 §Messager). Existe para que un procesador no imprima
// por su cuenta: la herramienta necesita ver los mensajes para contarlos, ubicarlos en el fuente y,
// sobre todo, para que un `Kind.ERROR` haga fallar la compilacion. Un `System.out.println` no hace
// nada de eso.
//
// Las cuatro sobrecargas abstractas son la misma operacion con cada vez mas contexto: solo el
// mensaje, el mensaje en un elemento, en una anotacion de ese elemento, y en un valor de esa
// anotacion. Mas contexto = el subrayado cae en un lugar mas preciso.
public interface Messager {

    /** Un mensaje sin ubicacion. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg);

    /** Un mensaje ubicado en `e`. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e);

    /** Un mensaje ubicado en la anotacion `a` de `e`. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a);

    /** Un mensaje ubicado en el valor `v` de la anotacion `a` de `e`. */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a,
            AnnotationValue v);

    // Los seis atajos de abajo son `default` en el contrato, no metodos de una clase de utilidad:
    // se resuelven contra `this`, asi que un implementador que solo escriba las cuatro de arriba ya
    // los tiene, y uno que quiera contarlos aparte puede sobreescribirlos.

    /** Atajo de {@code printMessage(Kind.ERROR, msg)}. */
    default void printError(CharSequence msg) {
        this.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    /** Atajo de {@code printMessage(Kind.ERROR, msg, e)}. */
    default void printError(CharSequence msg, Element e) {
        this.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    /** Atajo de {@code printMessage(Kind.WARNING, msg)}. */
    default void printWarning(CharSequence msg) {
        this.printMessage(Diagnostic.Kind.WARNING, msg);
    }

    /** Atajo de {@code printMessage(Kind.WARNING, msg, e)}. */
    default void printWarning(CharSequence msg, Element e) {
        this.printMessage(Diagnostic.Kind.WARNING, msg, e);
    }

    /** Atajo de {@code printMessage(Kind.NOTE, msg)}. */
    default void printNote(CharSequence msg) {
        this.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    /** Atajo de {@code printMessage(Kind.NOTE, msg, e)}. */
    default void printNote(CharSequence msg, Element e) {
        this.printMessage(Diagnostic.Kind.NOTE, msg, e);
    }
}
