package repro504;

import java.lang.annotation.Annotation;

// Repro de #504: con `--emit`, un tipo @interface que se esta compilando DESDE FUENTE no lleva su
// supertipo implicito java.lang.annotation.Annotation.
//
// La JLS 9.6 dice que declarar un tipo de anotacion crea una interfaz que implicitamente extiende
// Annotation. El chequeo lo aplica; el generador de bytecode no.
//
//   bin/javac.exe        Anot504.java Uso504.java   -> compila entero
//   bin/javac.exe --emit Anot504.java Uso504.java   -> FALLA en a() y en b()
//
// El JDK 25 compila los dos archivos juntos sin problema.
//
// Lo que separa lo que anda de lo que no es de donde sale el @interface:
//
//   | @interface anidado en este mismo archivo, con --emit        | FALLA   (metodo b)
//   | @interface en otro archivo del MISMO lote, con --emit       | FALLA   (metodo a)
//   | @interface leido de un .class ya compilado, con --emit      | compila (metodo c)
//   | cualquiera de los tres, SIN --emit                          | compila
//
// O sea: el supertipo implicito esta solo en el simbolo que arma el chequeo, y el generador trabaja
// con uno que no lo tiene. Un .class lo trae explicito en su tabla de interfaces y por eso anda.
//
// Es la misma forma que #502 y #503 -- la ruta de emision resuelve menos que la de chequeo -- y las
// tres conviene mirarlas juntas.
//
// Consecuencia practica: compilar un paquete entero de una sola invocacion, que es la forma
// recomendada de arrancarlo desde cero, es justamente la que falla. De a un archivo por vez anda,
// porque para cuando se compila el que usa la anotacion ya existe su .class.
public class Uso504 {

    /** Anidada en ESTE archivo. Tambien falla con --emit; ver la tabla de arriba. */
    public @interface Anidada {
        String value();
    }

    static void recibe(Class<? extends Annotation> c) {
    }

    static void recibeInstancia(Annotation a) {
    }

    // A: @interface de otro archivo del mismo lote. FALLA con --emit.
    static void a() {
        recibe(Anot504.class);
    }

    // B: @interface anidada en este archivo. FALLA con --emit.
    static void b() {
        recibe(Anidada.class);
    }

    // C: control -- anotacion leida de un .class. Compila con --emit.
    static void c() {
        recibe(Deprecated.class);
    }

    // D: control -- una instancia, no la clase. Compila con --emit.
    static void d(Anot504 x) {
        recibeInstancia(x);
    }
}
