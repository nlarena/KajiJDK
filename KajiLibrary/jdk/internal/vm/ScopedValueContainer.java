package jdk.internal.vm;

import java.lang.ScopedValue;

/**
 * KajiLibrary's jdk.internal.vm.ScopedValueContainer — el ámbito donde viven las ligaduras de
 * {@link ScopedValue}.
 *
 * <p>Un `ScopedValue` está ligado sólo mientras dura una llamada, y esa llamada puede anidar otras.
 * Este contenedor es lo que marca dónde empieza y termina cada tramo: se apila antes de correr el
 * cuerpo y se saca después, pase lo que pase.
 *
 * <p>Que sea un {@link StackableScope} es lo que hace que las ligaduras se deshagan **en orden
 * inverso** aunque el cuerpo salga por una excepción — es exactamente el problema que la clase base
 * resuelve.
 */
public class ScopedValueContainer extends StackableScope {

    /**
     * Una foto de las ligaduras vigentes, junto con el contenedor donde estaban.
     *
     * <p>Es un `record` porque es eso y nada más: dos valores que viajan juntos y se comparan por
     * contenido. Sirve para que un hilo que arranca dentro de un ámbito herede lo que había, sin
     * quedarse con una referencia viva al ámbito que quizás ya se cerró.
     *
     * @param scopedValueBindings las ligaduras
     * @param container el contenedor que las tenía
     */
    public record BindingsSnapshot(Object scopedValueBindings, ScopedValueContainer container) {
    }

    protected ScopedValueContainer() {
        super();
    }

    /** El contenedor más cercano de ese tipo en el hilo actual, o `null`. */
    public static <T extends ScopedValueContainer> T latest(Class<T> tipo) {
        StackableScope cabeza = StackableScope.head();
        if (cabeza == null) {
            return null;
        }
        if (tipo.isInstance(cabeza)) {
            return (T) cabeza;
        }
        return cabeza.enclosingScope(tipo);
    }

    /** El contenedor más cercano del hilo actual, o `null`. */
    public static ScopedValueContainer latest() {
        return ScopedValueContainer.latest(ScopedValueContainer.class);
    }

    /**
     * Una foto de las ligaduras vigentes.
     *
     * <p>Devuelve una foto con `null` de ligaduras cuando no hay ninguna --que es lo que corresponde,
     * y no una foto nula: "no hay ligaduras" es un estado, no la ausencia de respuesta.
     */
    public static BindingsSnapshot captureBindings() {
        return new BindingsSnapshot(null, ScopedValueContainer.latest());
    }

    /**
     * Corre `op` dentro de un contenedor nuevo.
     *
     * <p>El `finally` es la clase entera: si el cuerpo tira, el contenedor **igual** se saca. Sin eso,
     * una excepción dejaría ligaduras vivas en un hilo que ya salió del ámbito, que es la clase de
     * error que después aparece a mil líneas de distancia.
     */
    public static void run(Runnable op) {
        if (op == null) {
            throw new NullPointerException("op");
        }
        ScopedValueContainer c = new ScopedValueContainer();
        c.push();
        try {
            op.run();
        } finally {
            c.popForcefully();
        }
    }

    /**
     * Corre `op` dentro de un contenedor nuevo y devuelve su resultado.
     *
     * <p>La variante que devuelve valor y que puede tirar una excepción **chequeada** propia: por eso
     * la segunda variable de tipo. Es lo que permite envolver código que tira sin obligarlo a
     * envolverse en algo no chequeado.
     */
    public static <V, X extends Throwable> V call(ScopedValue.CallableOp<V, X> op) throws X {
        if (op == null) {
            throw new NullPointerException("op");
        }
        ScopedValueContainer c = new ScopedValueContainer();
        c.push();
        try {
            return op.call();
        } finally {
            c.popForcefully();
        }
    }
}
