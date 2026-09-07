package jdk.incubator.vector;

import jdk.internal.vm.vector.VectorSupport;

/**
 * Un vector de banderas: que carriles participan de una operacion.
 *
 * <p>Es como se hace un {@code if} sin ramificar. En vez de saltear los carriles que no cumplen la
 * condicion --imposible, porque la instruccion opera sobre todos-- se calcula sobre todos y la
 * mascara decide cuales se escriben.
 *
 * <p>Tambien es lo que resuelve el remanente de un arreglo: una mascara que apaga los carriles que
 * caerian fuera permite procesar la ultima porcion con la misma instruccion que el resto.
 *
 * @since 16
 */
public abstract class VectorMask<E extends Object> extends VectorSupport.VectorMask<E> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    VectorMask(Object payload) {
        super(payload);
    }

    /**
     * La especie de los vectores a los que acompana.
     *
     * @return el {@code VectorSpecies<E>}
     */
    public abstract VectorSpecies<E> vectorSpecies();

    /**
     * Cuantas posiciones tiene la mascara.
     *
     * @return el numero
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final int length() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una mascara con esas banderas.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param flag el {@code boolean...}
     * @return el {@code VectorMask<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorMask<E> fromValues(VectorSpecies<E> vectorSpecies,
            boolean... flag) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una mascara leida de ese arreglo de banderas.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @return el {@code VectorMask<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorMask<E> fromArray(VectorSpecies<E> vectorSpecies,
            boolean[] flags, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una mascara armada con los bits bajos de ese {@code long}, uno por posicion.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param l el {@code long}
     * @return el {@code VectorMask<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorMask<E> fromLong(VectorSpecies<E> vectorSpecies, long l
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La misma mascara para otra especie del mismo largo.
     *
     * @param <F> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @return el {@code VectorMask<F>}
     */
    public abstract <F extends Object> VectorMask<F> cast(VectorSpecies<F> vectorSpecies);

    /**
     * Las banderas de la mascara juntas en un {@code long}, una por bit.
     *
     * @return el numero
     */
    public abstract long toLong();

    /**
     * Las banderas en un arreglo nuevo.
     *
     * @return el {@code boolean[]}
     */
    public abstract boolean[] toArray();

    /**
     * Escribe las banderas en ese arreglo.
     *
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     */
    public abstract void intoArray(boolean[] flags, int i);

    /**
     * Si hay al menos una posicion prendida.
     *
     * @return cierto o falso, segun corresponda
     */
    public abstract boolean anyTrue();

    /**
     * Si estan prendidas todas.
     *
     * @return cierto o falso, segun corresponda
     */
    public abstract boolean allTrue();

    /**
     * Cuantas posiciones estan prendidas.
     *
     * @return el numero
     */
    public abstract int trueCount();

    /**
     * El indice de la primera prendida, o la cantidad de posiciones si no hay ninguna.
     *
     * @return el numero
     */
    public abstract int firstTrue();

    /**
     * El indice de la ultima prendida, o -1 si no hay ninguna.
     *
     * @return el numero
     */
    public abstract int lastTrue();

    /**
     * La conjuncion bit a bit.
     *
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> and(VectorMask<E> vectorMask);

    /**
     * La disyuncion bit a bit.
     *
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> or(VectorMask<E> vectorMask);

    /**
     * La disyuncion exclusiva bit a bit.
     *
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> xor(VectorMask<E> vectorMask);

    /**
     * La conjuncion con el otro invertido.
     *
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> andNot(VectorMask<E> vectorMask);

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> eq(VectorMask<E> vectorMask);

    /**
     * Invierte cada posicion.
     *
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> not();

    /**
     * La mascara de las posiciones cuyo indice todavia entra en el rango.
     *
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> indexInRange(int i, int i2);

    /**
     * La mascara de las posiciones cuyo indice todavia entra en el rango.
     *
     * @param l el {@code long}
     * @param l2 el {@code long}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> indexInRange(long l, long l2);

    /**
     * La mascara vista como vector: cero donde esta apagada.
     *
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> toVector();

    /**
     * Si esa posicion esta prendida.
     *
     * @param i el {@code int}
     * @return cierto o falso, segun corresponda
     */
    public abstract boolean laneIsSet(int i);

    /**
     * Comprueba que la mascara sea de esa especie y devuelve lo mismo, ya tipado.
     *
     * @param <F> el tipo, en su version envuelta
     * @param classArg el {@code Class<F>}
     * @return el {@code VectorMask<F>}
     */
    public abstract <F extends Object> VectorMask<F> check(Class<F> classArg);

    /**
     * Comprueba que la mascara sea de esa especie y devuelve lo mismo, ya tipado.
     *
     * @param <F> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @return el {@code VectorMask<F>}
     */
    public abstract <F extends Object> VectorMask<F> check(VectorSpecies<F> vectorSpecies);

    /**
     * Las banderas escritas como una lista.
     *
     * @return el texto
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final String toString() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Si la otra mascara tiene las mismas banderas.
     *
     * @param obj el {@code Object}
     * @return cierto o falso, segun corresponda
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final boolean equals(Object obj) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El codigo de dispersion.
     *
     * @return el numero
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final int hashCode() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compress();
}
