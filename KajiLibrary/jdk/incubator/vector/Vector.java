package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import jdk.internal.vm.vector.VectorSupport;

/**
 * Un vector de valores del mismo tipo, para operar sobre todos a la vez.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>Un procesador moderno puede sumar ocho enteros en una sola instruccion. Un bucle Java que suma
 * de a uno desperdicia siete octavos de esa capacidad, y el compilador solo a veces adivina que
 * puede vectorizarlo. Este API es la forma de pedirlo explicitamente.
 *
 * <h2>La forma del vector no la elige el programa</h2>
 *
 * <p>La elige la maquina: un procesador con AVX-512 tiene registros de 512 bits y otro de 128. Por
 * eso el largo no es un numero fijo sino {@link VectorSpecies#length}, y por eso los bucles se
 * escriben con {@link VectorSpecies#loopBound} en vez de con un paso constante.
 *
 * <p>Escribir el paso a mano es el error que hace que el codigo ande en una maquina y se rompa en
 * otra.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>El API esta declarado entero, con las firmas exactas del JDK 25, y contra el compila cualquier
 * codigo que lo use. Lo que no hay son los <strong>intrinsecos</strong>: cada operacion de vector
 * existe para que la VM la reemplace por una instruccion de la maquina, y sin ese reemplazo no
 * queda nada que ejecutar.
 *
 * <p>Por eso las operaciones lanzan {@link UnsupportedOperationException} en vez de calcular en un
 * bucle escalar. Un fallback escalar seria una mentira util: daria el resultado correcto y mucho
 * mas lento que el bucle que el usuario acaba de reemplazar, o sea lo contrario de lo que este API
 * promete.
 *
 * <p>Las partes que <strong>si</strong> funcionan son las que no dependen de la maquina:
 * {@link VectorMath}, {@link Float16} y los metadatos de {@link VectorSpecies}.
 *
 * @since 16
 */
public abstract class Vector<E extends Object> extends VectorSupport.Vector<E> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    Vector(Object payload) {
        super(payload);
    }

    /**
     * La especie de este vector: su tipo de posicion y su forma.
     *
     * @return el {@code VectorSpecies<E>}
     */
    public abstract VectorSpecies<E> species();

    /**
     * El tipo de las posiciones.
     *
     * @return el {@code Class<E>}
     */
    public abstract Class<E> elementType();

    /**
     * El tamano de una posicion, en bits.
     *
     * @return el numero
     */
    public abstract int elementSize();

    /**
     * La forma de este vector.
     *
     * @return el {@code VectorShape}
     */
    public abstract VectorShape shape();

    /**
     * Cuantas posiciones tiene.
     *
     * @return el numero
     */
    public abstract int length();

    /**
     * El tamano del vector entero, en bits.
     *
     * @return el numero
     */
    public abstract int bitSize();

    /**
     * El tamano del vector entero, en bytes.
     *
     * @return el numero
     */
    public abstract int byteSize();

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Unary unary);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Unary unary, VectorMask<E> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, Vector<E> vector);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, Vector<E> vector,
            VectorMask<E> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, long l);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, long l,
            VectorMask<E> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<E>}
     * @param vector2 el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Ternary ternary, Vector<E> vector,
            Vector<E> vector2);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<E>}
     * @param vector2 el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Ternary ternary, Vector<E> vector,
            Vector<E> vector2, VectorMask<E> vectorMask);

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> add(Vector<E> vector);

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> add(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> sub(Vector<E> vector);

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> sub(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> mul(Vector<E> vector);

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> mul(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> div(Vector<E> vector);

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> div(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * El opuesto de cada posicion.
     *
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> neg();

    /**
     * El valor absoluto de cada posicion.
     *
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> abs();

    /**
     * El menor de cada par de posiciones.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> min(Vector<E> vector);

    /**
     * El mayor de cada par de posiciones.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> max(Vector<E> vector);

    /**
     * Como {@code reduceLanes}, pero el resultado se devuelve como {@code long}.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative);

    /**
     * Como {@code reduceLanes}, pero el resultado se devuelve como {@code long}.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<E> vectorMask);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> test(VectorOperators.Test test);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> test(VectorOperators.Test test, VectorMask<E> vectorMask);

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> eq(Vector<E> vector);

    /**
     * La mascara de las posiciones menores.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> lt(Vector<E> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, Vector<E> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, Vector<E> vector,
            VectorMask<E> vectorMask);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<E> vectorMask);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> blend(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> blend(long l, VectorMask<E> vectorMask);

    /**
     * Le suma a cada posicion su propio indice multiplicado por ese paso.
     *
     * @param i el {@code int}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> addIndex(int i);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> slice(int i, Vector<E> vector);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> slice(int i, Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> slice(int i);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<E>}
     * @param i2 el {@code int}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> unslice(int i, Vector<E> vector, int i2);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<E>}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> unslice(int i, Vector<E> vector, int i2, VectorMask<E> vectorMask);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> unslice(int i);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> vectorShuffle);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> vectorShuffle, VectorMask<E> vectorMask);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<E>}
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> vectorShuffle, Vector<E> vector);

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> compress(VectorMask<E> vectorMask);

    /**
     * Reparte las posiciones del principio en los lugares que la mascara marca.
     *
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> expand(VectorMask<E> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> selectFrom(Vector<E> vector);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<E>}
     * @param vector2 el {@code Vector<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> selectFrom(Vector<E> vector, Vector<E> vector2);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<E>}
     * @param vectorMask el {@code VectorMask<E>}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> selectFrom(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code Vector<E>}
     */
    public abstract Vector<E> broadcast(long l);

    /**
     * Una mascara con todas las posiciones en ese valor.
     *
     * @param flag el {@code boolean}
     * @return el {@code VectorMask<E>}
     */
    public abstract VectorMask<E> maskAll(boolean flag);

    /**
     * Este vector visto como un barajado.
     *
     * @return el {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> toShuffle();

    /**
     * Los mismos bits leidos como otra especie.
     *
     * @param <F> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @param i el {@code int}
     * @return el {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> reinterpretShape(VectorSpecies<F> vectorSpecies,
            int i);

    /**
     * Los mismos bits leidos como {@code byte}.
     *
     * @return el {@code ByteVector}
     */
    public abstract ByteVector reinterpretAsBytes();

    /**
     * Los mismos bits leidos como {@code short}.
     *
     * @return el {@code ShortVector}
     */
    public abstract ShortVector reinterpretAsShorts();

    /**
     * Los mismos bits leidos como {@code int}.
     *
     * @return el {@code IntVector}
     */
    public abstract IntVector reinterpretAsInts();

    /**
     * Los mismos bits leidos como {@code long}.
     *
     * @return el {@code LongVector}
     */
    public abstract LongVector reinterpretAsLongs();

    /**
     * Los mismos bits leidos como {@code float}.
     *
     * @return el {@code FloatVector}
     */
    public abstract FloatVector reinterpretAsFloats();

    /**
     * Los mismos bits leidos como {@code double}.
     *
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector reinterpretAsDoubles();

    /**
     * Los mismos bits vistos como el entero del mismo tamano.
     *
     * @return el {@code Vector<?>}
     */
    public abstract Vector<?> viewAsIntegralLanes();

    /**
     * Los mismos bits vistos como el flotante del mismo tamano.
     *
     * @return el {@code Vector<?>}
     */
    public abstract Vector<?> viewAsFloatingLanes();

    /**
     * Convierte las posiciones con ese operador de conversion.
     *
     * @param <F> el tipo, en su version envuelta
     * @param conversion el {@code VectorOperators.Conversion<E, F>}
     * @param i el {@code int}
     * @return el {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> convert(
            VectorOperators.Conversion<E, F> conversion, int i);

    /**
     * Convierte a otra especie, tomando la parte que se indica.
     *
     * @param <F> el tipo, en su version envuelta
     * @param conversion el {@code VectorOperators.Conversion<E, F>}
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @param i el {@code int}
     * @return el {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> convertShape(
            VectorOperators.Conversion<E, F> conversion, VectorSpecies<F> vectorSpecies, int i);

    /**
     * Como {@code convertShape}, pero siempre conservando el valor.
     *
     * @param <F> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @param i el {@code int}
     * @return el {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> castShape(VectorSpecies<F> vectorSpecies, int i);

    /**
     * Comprueba que el tipo de posicion sea ese y devuelve lo mismo, ya tipado.
     *
     * @param <F> el tipo, en su version envuelta
     * @param classArg el {@code Class<F>}
     * @return el {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> check(Class<F> classArg);

    /**
     * Comprueba que el tipo de posicion sea ese y devuelve lo mismo, ya tipado.
     *
     * @param <F> el tipo, en su version envuelta
     * @param vectorSpecies el {@code VectorSpecies<F>}
     * @return el {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> check(VectorSpecies<F> vectorSpecies);

    /**
     * Escribe el vector en esa zona de memoria.
     *
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     */
    public abstract void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder);

    /**
     * Escribe el vector en esa zona de memoria.
     *
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<E>}
     */
    public abstract void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<E> vectorMask);

    /**
     * Las posiciones en un arreglo nuevo.
     *
     * @return el {@code Object}
     */
    public abstract Object toArray();

    /**
     * Las posiciones en un arreglo de {@code int} nuevo.
     *
     * @return el {@code int[]}
     */
    public abstract int[] toIntArray();

    /**
     * Las posiciones en un arreglo de {@code long} nuevo.
     *
     * @return el {@code long[]}
     */
    public abstract long[] toLongArray();

    /**
     * Las posiciones en un arreglo de {@code double} nuevo.
     *
     * @return el {@code double[]}
     */
    public abstract double[] toDoubleArray();

    /**
     * Una representacion legible.
     *
     * @return el texto
     */
    public abstract String toString();

    /**
     * Si el otro es igual a este.
     *
     * @param obj el {@code Object}
     * @return cierto o falso, segun corresponda
     */
    public abstract boolean equals(Object obj);

    /**
     * El codigo de dispersion.
     *
     * @return el numero
     */
    public abstract int hashCode();
}
