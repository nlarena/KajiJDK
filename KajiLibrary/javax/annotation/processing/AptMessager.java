package javax.annotation.processing;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

// El Messager del round loop de este proyecto: escribe por el mismo puente nativo que usa AptTrace,
// que es la consola del intérprete y lo que `AptOutcome.console` termina capturando. Así un
// procesador puede reportar sin `System.out.println` (que todavía no compila en este javac).
//
// El formato es `KIND: mensaje`, y cuando hay contexto se agrega ` en <elemento>` — no más que eso:
// esta implementación **no** puede subrayar una posición en el fuente, porque el round loop no le
// pasa la unidad de compilación ni las posiciones. Poner un número de línea inventado sería peor
// que no ponerlo.
//
// Lo que este Messager NO hace, y conviene saberlo: un `Kind.ERROR` **no** hace fallar la
// compilación. En el JDK real ese es el efecto principal de reportar un error; acá el round loop no
// mira los mensajes, así que un error es una línea en la consola y nada más. Es la razón por la que
// `RoundEnvironmentImpl.errorRaised()` puede devolver `false` con la conciencia tranquila.
class AptMessager implements Messager {

    // El puente: `AptTrace.trace` es el native que ya existe para que un procesador imprima.
    private static void emit(Diagnostic.Kind kind, CharSequence msg, String where) {
        String texto = kind.toString() + ": " + String.valueOf(msg);
        if (where != null) {
            texto = texto + " en " + where;
        }
        AptTrace.trace(texto);
    }

    // El nombre de un elemento para el mensaje, o null si no hay elemento. Se usa `toString()` y no
    // `getSimpleName()` porque la reificación de elementos es parcial y `toString` es lo único que
    // todo Element de acá contesta.
    private static String nombre(Element e) {
        if (e == null) {
            return null;
        }
        return e.toString();
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
        emit(kind, msg, null);
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
        emit(kind, msg, nombre(e));
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
        emit(kind, msg, nombre(e));
    }

    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a,
            AnnotationValue v) {
        emit(kind, msg, nombre(e));
    }
}
