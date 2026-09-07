package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.util.function;
import java.util.function.IntUnaryOperator;
import jdk.internal.vm.vector.VectorSupport;

/**
 * Una permutacion de carriles: de donde sale cada posicion del resultado.
 *
 * <p>Es la operacion que no tiene equivalente escalar barato. Reordenar, intercalar, dar vuelta o
 * repetir carriles cuesta una instruccion, y hacerlo con indices en un bucle cuesta un acceso a
 * memoria por elemento.
 *
 * @since 16
 */
public abstract class VectorShuffle<E extends Object> extends VectorSupport.VectorShuffle<E> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    VectorShuffle(Object payload) {
        super(payload);
    }

    /**
     * La especie de los vectores que este barajado sabe reordenar.
     *
     * @return el {@code VectorSpecies<E>}
     */
    public abstract VectorSpecies<E> vectorSpecies();

    /**
     * Cuantas posiciones tiene el barajado.
     *
     * @return el numero
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final int length() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mismo barajado para otra especie del mismo largo.
     *
     * @param <F> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @return el {@code VectorShuffle<F>}
     */
    public abstract <F extends Object> VectorShuffle<F> cast(VectorSpecies<F> vectorSpecies);

    /**
     * Comprueba que el barajado sea de esa especie y devuelve lo mismo, ya tipado.
     *
     * @param <F> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @return el {@code VectorShuffle<F>}
     */
    public abstract <F extends Object> VectorShuffle<F> check(VectorSpecies<F> vectorSpecies);

    /**
     * Comprueba que ese indice caiga dentro.
     *
     * @param i el {@code int}
     * @return el numero
     */
    public abstract int checkIndex(int i);

    /**
     * Ese indice llevado al rango dando la vuelta.
     *
     * @param i el {@code int}
     * @return el numero
     */
    public abstract int wrapIndex(int i);

    /**
     * Comprueba que todos los indices caigan dentro.
     *
     * @return el {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> checkIndexes();

    /**
     * El barajado con todos sus indices llevados al rango.
     *
     * @return el {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> wrapIndexes();

    /**
     * La mascara de los indices que caen dentro del vector.
     *
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> laneIsValid();

    /**
     * Un barajado con esos indices.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param i el {@code int...}
     * @return el {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorShuffle<E> fromValues(VectorSpecies<E> vectorSpecies,
            int... i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un barajado leido de ese arreglo de indices.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @return el {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorShuffle<E> fromArray(VectorSpecies<E> vectorSpecies,
            int[] is, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un barajado leido de esa zona de memoria.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorShuffle<E> fromMemorySegment(
            VectorSpecies<E> vectorSpecies, java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un barajado cuyos indices los calcula esa funcion a partir de la posicion.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param intUnaryOperator el {@code java.util.function.IntUnaryOperator}
     * @return el {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorShuffle<E> fromOp(VectorSpecies<E> vectorSpecies,
            java.util.function.IntUnaryOperator intUnaryOperator) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un barajado que cuenta: arranca en un valor y avanza de a un paso.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @param flag el {@code boolean}
     * @return el {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorShuffle<E> iota(VectorSpecies<E> vectorSpecies, int i,
            int i2, boolean flag) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El barajado que entrelaza dos vectores.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param i el {@code int}
     * @return el {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorShuffle<E> makeZip(VectorSpecies<E> vectorSpecies, int i
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El barajado que deshace el entrelazado.
     *
     * @param <E> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<E>}
     * @param i el {@code int}
     * @return el {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static <E extends Object> VectorShuffle<E> makeUnzip(VectorSpecies<E> vectorSpecies,
            int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Los indices en un arreglo nuevo.
     *
     * @return el {@code int[]}
     */
    public abstract int[] toArray();

    /**
     * Escribe los indices en ese arreglo.
     *
     * @param is el {@code int[]}
     * @param i el {@code int}
     */
    public abstract void intoArray(int[] is, int i);

    /**
     * Escribe los indices en esa zona de memoria.
     *
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     */
    public abstract void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder);

    /**
     * Los indices vistos como un vector.
     *
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> toVector();

    /**
     * De que posicion del origen sale esa posicion.
     *
     * @param i el {@code int}
     * @return el numero
     */
    public abstract int laneSource(int i);

    /**
     * Compone este barajado con el otro.
     *
     * @param vectorShuffle el {@code VectorShuffle<E>}
     * @return el {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> rearrange(VectorShuffle<E> vectorShuffle);

    /**
     * Los indices escritos como una lista.
     *
     * @return el texto
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final String toString() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Si el otro barajado tiene los mismos indices.
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
}
