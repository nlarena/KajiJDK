package java.lang.classfile;

/**
 * Una transformación: recibe los elementos de algo que ya existe y va escribiendo lo que ese algo
 * debe pasar a ser.
 *
 * <p>La forma es la que da todo el sentido a esta API. Un transformador **no** modifica un modelo —
 * los modelos son inmutables— sino que se lo recorre elemento por elemento y, por cada uno, decide
 * qué poner en el constructor de la copia. Dejarlo pasar tal cual, cambiarlo, tirarlo, o emitir
 * varios en su lugar: las cuatro cosas son el mismo gesto.
 *
 * <p><strong>{@link #atStart} y {@link #atEnd} son la mitad que se olvida.</strong> Sin ellos, un
 * transformador sólo puede reaccionar a lo que ve, y hay dos cosas que no se pueden hacer así:
 * agregar algo que en el original no estaba (va en `atStart` o en `atEnd`, según dónde tenga que
 * quedar) y cerrar un estado que se fue acumulando (un contador, una tabla de lo visto). Un
 * transformador con estado se fabrica con `ofStateful`, que da uno nuevo por cada uso — ver
 * {@link ClassTransform#ofStateful}.
 *
 * <p>Los tres parámetros de tipo se leen así: `C` es el propio transformador (para que
 * {@link #andThen} devuelva su tipo y no éste), `E` el elemento que consume y `B` el constructor en
 * el que escribe. Las cuatro especializaciones —{@link ClassTransform}, {@link MethodTransform},
 * {@link FieldTransform}, {@link CodeTransform}— fijan los tres.
 *
 * <p>En el JDK esta interfaz es `sealed`; acá no, por el mismo motivo que
 * {@link ClassFileElement}: sellarla obligaría al paquete público a nombrar sus implementaciones
 * internas.
 */
public interface ClassFileTransform<C extends ClassFileTransform<C, E, B>,
        E extends ClassFileElement, B extends ClassFileBuilder<E, B>> {

    /** Qué hacer con este elemento. Lo normal es escribir algo en `builder`. */
    void accept(B builder, E element);

    /**
     * Se llama **antes** del primer elemento.
     *
     * <p>Vacío por omisión. Es donde va lo que tiene que quedar al principio de la copia, y donde un
     * transformador con estado lo inicializa.
     */
    default void atStart(B builder) {
    }

    /**
     * Se llama **después** del último.
     *
     * <p>Vacío por omisión. Es donde va lo que tiene que quedar al final, y donde un transformador
     * con estado vuelca lo que junto.
     */
    default void atEnd(B builder) {
    }

    /**
     * Este transformador y después ese otro, como uno solo.
     *
     * <p>Lo que sale de éste entra al siguiente, no al constructor final: encadenar dos es aplicar
     * el segundo **sobre el resultado** del primero, no correr los dos sobre el original. Por eso el
     * orden importa, y por eso `a.andThen(b)` no es lo mismo que `b.andThen(a)`.
     */
    C andThen(C next);
}
