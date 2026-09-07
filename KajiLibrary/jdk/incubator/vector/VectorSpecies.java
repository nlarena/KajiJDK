package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.util.function;
import java.util.function.IntUnaryOperator;

/**
 * El tipo de carril mas la forma del vector: cuantos carriles hay y de que tamano.
 *
 * <p>Es lo primero que se pide y lo que decide todo lo demas. Un {@code VectorSpecies<Integer>} de
 * 256 bits tiene ocho carriles; el mismo tipo con 128 bits tiene cuatro.
 *
 * <p>{@link #loopBound} es el metodo que hay que usar y el que se olvida: devuelve hasta donde
 * llega el bucle vectorial, y lo que sobra --el remanente-- se hace de a un elemento. Sin eso, un
 * arreglo cuyo largo no es multiplo del numero de carriles se procesa mal o se sale de rango.
 *
 * <p>En esta biblioteca los metadatos son reales: {@link #length}, {@link #vectorBitSize},
 * {@link #elementSize} y {@link #loopBound} calculan de verdad. Lo que no puede funcionar es
 * fabricar vectores.
 *
 * @since 16
 */
public interface VectorSpecies<E extends Object> {

    /**
     * El tipo de las posiciones.
     *
     * @return el {@code Class<E>}
     */
    Class<E> elementType();

    /**
     * La clase de los vectores de esta especie.
     *
     * @return el {@code Class<? extends Vector<E>>}
     */
    Class<? extends Vector<E>> vectorType();

    /**
     * La clase de las mascaras de esta especie.
     *
     * @return el {@code Class<? extends VectorMask<E>>}
     */
    Class<? extends VectorMask<E>> maskType();

    /**
     * El tamano de una posicion, en bits.
     *
     * @return el numero
     */
    int elementSize();

    /**
     * La forma de esta especie.
     *
     * @return el {@code VectorShape}
     */
    VectorShape vectorShape();

    /**
     * Cuantas posiciones tiene.
     *
     * @return el numero
     */
    int length();

    /**
     * El tamano del vector, en bits.
     *
     * @return el numero
     */
    int vectorBitSize();

    /**
     * El tamano del vector, en bytes.
     *
     * @return el numero
     */
    int vectorByteSize();

    /**
     * El multiplo de la cantidad de posiciones mas grande que no pasa de ese numero.
     *
     * @param i el {@code int}
     * @return el numero
     */
    int loopBound(int i);

    /**
     * El multiplo de la cantidad de posiciones mas grande que no pasa de ese numero.
     *
     * @param l el {@code long}
     * @return el numero
     */
    long loopBound(long l);

    /**
     * La mascara de las posiciones cuyo indice todavia entra en el rango.
     *
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @return el {@code VectorMask<E>}
     */
    VectorMask<E> indexInRange(int i, int i2);

    /**
     * La mascara de las posiciones cuyo indice todavia entra en el rango.
     *
     * @param l el {@code long}
     * @param l2 el {@code long}
     * @return el {@code VectorMask<E>}
     */
    VectorMask<E> indexInRange(long l, long l2);

    /**
     * Comprueba que el tipo de posicion sea ese y devuelve lo mismo, ya tipado.
     *
     * @param <F> el tipo, en su version envuelta
     * @param classArg el {@code Class<F>}
     * @return el {@code VectorSpecies<F>}
     */
    <F extends Object> VectorSpecies<F> check(Class<F> classArg);

    /**
     * Cuantas partes hacen falta para pasar a la otra especie.
     *
     * @param vectorSpecies el {@code VectorSpecies<?>}
     * @param flag el {@code boolean}
     * @return el numero
     */
    int partLimit(VectorSpecies<?> vectorSpecies, boolean flag);

    /**
     * La misma forma con otro tipo de posicion.
     *
     * @param <F> el tipo, en su version envuelta
     * @param classArg el {@code Class<F>}
     * @return el {@code VectorSpecies<F>}
     */
    <F extends Object> VectorSpecies<F> withLanes(Class<F> classArg);

    /**
     * El mismo tipo de posicion con otra forma.
     *
     * @param vectorShape el {@code VectorShape}
     * @return el {@code VectorSpecies<E>}
     */
    VectorSpecies<E> withShape(VectorShape vectorShape);

    /**
     * La especie de ese tipo y esa forma.
     *
     * @param <E> el tipo, en su version envuelta
     * @param classArg el {@code Class<E>}
     * @param vectorShape el {@code VectorShape}
     * @return el {@code VectorSpecies<E>}
     */
    static <E extends Object> VectorSpecies<E> of(Class<E> classArg, VectorShape vectorShape) {
        return Especie.de(classArg, vectorShape);
    }

    /**
     * La especie de ese tipo con la forma mas grande de esta maquina.
     *
     * @param <E> el tipo, en su version envuelta
     * @param classArg el {@code Class<E>}
     * @return el {@code VectorSpecies<E>}
     */
    static <E extends Object> VectorSpecies<E> ofLargestShape(Class<E> classArg) {
        return Especie.de(classArg, VectorShape.largestShapeFor(classArg));
    }

    /**
     * La especie de ese tipo con la forma que conviene en esta maquina.
     *
     * @param <E> el tipo, en su version envuelta
     * @param classArg el {@code Class<E>}
     * @return el {@code VectorSpecies<E>}
     */
    static <E extends Object> VectorSpecies<E> ofPreferred(Class<E> classArg) {
        return Especie.de(classArg, VectorShape.preferredShape());
    }

    /**
     * El tamano de una posicion, en bits.
     *
     * @param classArg el {@code Class<?>}
     * @return el numero
     */
    static int elementSize(Class<?> classArg) {
        return Especie.bitsDe(classArg);
    }

    /**
     * Un vector con todas las posiciones en cero.
     *
     * @return el {@code Vector<E>}
     */
    Vector<E> zero();

    /**
     * Un vector leido de ese arreglo.
     *
     * @param obj el {@code Object}
     * @param i el {@code int}
     * @return el {@code Vector<E>}
     */
    Vector<E> fromArray(Object obj, int i);

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code Vector<E>}
     */
    Vector<E> fromMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder);

    /**
     * Una mascara leida de ese arreglo de banderas.
     *
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @return el {@code VectorMask<E>}
     */
    VectorMask<E> loadMask(boolean[] flags, int i);

    /**
     * Una mascara con todas las posiciones en ese valor.
     *
     * @param flag el {@code boolean}
     * @return el {@code VectorMask<E>}
     */
    VectorMask<E> maskAll(boolean flag);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code Vector<E>}
     */
    Vector<E> broadcast(long l);

    /**
     * Comprueba que ese valor entre en una posicion de esta especie.
     *
     * @param l el {@code long}
     * @return el numero
     */
    long checkValue(long l);

    /**
     * Un barajado con esos indices.
     *
     * @param i el {@code int...}
     * @return el {@code VectorShuffle<E>}
     */
    VectorShuffle<E> shuffleFromValues(int... i);

    /**
     * Un barajado leido de ese arreglo.
     *
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @return el {@code VectorShuffle<E>}
     */
    VectorShuffle<E> shuffleFromArray(int[] is, int i);

    /**
     * Un barajado cuyos indices los calcula esa funcion.
     *
     * @param intUnaryOperator el {@code java.util.function.IntUnaryOperator}
     * @return el {@code VectorShuffle<E>}
     */
    VectorShuffle<E> shuffleFromOp(java.util.function.IntUnaryOperator intUnaryOperator);

    /**
     * Un barajado que cuenta, de esta especie.
     *
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @param flag el {@code boolean}
     * @return el {@code VectorShuffle<E>}
     */
    VectorShuffle<E> iotaShuffle(int i, int i2, boolean flag);

    /**
     * Una representacion legible.
     *
     * @return el texto
     */
    String toString();

    /**
     * Si el otro es igual a este.
     *
     * @param obj el {@code Object}
     * @return cierto o falso, segun corresponda
     */
    boolean equals(Object obj);

    /**
     * El codigo de dispersion.
     *
     * @return el numero
     */
    int hashCode();
}
