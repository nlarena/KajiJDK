package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * Un vector de posiciones {@code long}.
 *
 * <p>Es una de las seis clases donde el API se vuelve concreto. {@link Vector} habla de posiciones
 * sin decir de que son y por eso sus metodos toman y devuelven {@code Object} o el tipo envuelto;
 * aca las posiciones son {@code long} de verdad, asi que se puede cargar desde un {@code long[]},
 * leer una posicion como {@code long} y operar sin envolver nada.
 *
 * <h2>Las constantes {@code SPECIES_}</h2>
 *
 * <p>Cada una es esta clase con una forma ya elegida, y son objetos de verdad: contestan cuantas
 * posiciones tienen, cuanto ocupan y donde termina un bucle que avanza de a un vector. Lo que no
 * pueden es fabricar el vector.
 *
 * <p>{@link #SPECIES_MAX} y {@link #SPECIES_PREFERRED} dependen de la maquina. Aca las dos dan 64
 * bits, que es el minimo del API: el maximo real sale de los intrinsecos y no hay a quien
 * preguntarle.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Las firmas estan todas y son las del JDK 25, asi que el codigo que use este API compila.
 * Ninguna operacion puede ejecutar: crear u operar un vector se apoya en intrinsecos de la VM
 * --cada operacion se reemplaza por una instruccion vectorial de la maquina-- y esta VM no los
 * tiene. Cada metodo concreto tira {@link UnsupportedOperationException} en vez de devolver un
 * vector inventado, que es lo unico honesto que se puede hacer -- un vector de ceros compilaria
 * igual y daria resultados equivocados sin avisar.
 *
 * @since 16
 */
public abstract class LongVector extends AbstractVector<Long> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    LongVector(Object payload) {
        super(payload);
    }

    /** La especie de {@code long} de 64 bits. */
    public static final VectorSpecies<Long> SPECIES_64 =
            Especie.de(long.class, VectorShape.S_64_BIT);

    /** La especie de {@code long} de 128 bits. */
    public static final VectorSpecies<Long> SPECIES_128 =
            Especie.de(long.class, VectorShape.S_128_BIT);

    /** La especie de {@code long} de 256 bits. */
    public static final VectorSpecies<Long> SPECIES_256 =
            Especie.de(long.class, VectorShape.S_256_BIT);

    /** La especie de {@code long} de 512 bits. */
    public static final VectorSpecies<Long> SPECIES_512 =
            Especie.de(long.class, VectorShape.S_512_BIT);

    /** La especie de {@code long} de la forma mas grande de esta maquina. */
    public static final VectorSpecies<Long> SPECIES_MAX =
            Especie.de(long.class, VectorShape.S_Max_BIT);

    /** La especie de {@code long} de la forma que conviene en esta maquina. */
    public static final VectorSpecies<Long> SPECIES_PREFERRED =
            Especie.de(long.class, VectorShape.preferredShape());

    /**
     * Un vector con todas las posiciones en cero.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector zero(VectorSpecies<Long> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     */
    public abstract LongVector broadcast(long l);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector broadcast(VectorSpecies<Long> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @return el {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Unary unary);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Unary unary, VectorMask<Long> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Binary binary, Vector<Long> vector);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Binary binary, Vector<Long> vector,
            VectorMask<Long> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Long>}
     * @param vector2 el {@code Vector<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector,
            Vector<Long> vector2);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Long>}
     * @param vector2 el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector,
            Vector<Long> vector2, VectorMask<Long> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param l el {@code long}
     * @param l2 el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, long l2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param l el {@code long}
     * @param l2 el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, long l2,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Long>}
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Long>}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector, long l,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param l el {@code long}
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param l el {@code long}
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, Vector<Long> vector,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector add(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector add(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector add(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector add(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector sub(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector sub(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector sub(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector sub(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector mul(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector mul(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector mul(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector mul(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector div(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector div(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector div(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector div(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector min(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector min(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector max(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector max(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector and(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector and(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector or(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector or(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El opuesto de cada posicion.
     *
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector neg() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El valor absoluto de cada posicion.
     *
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector abs() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Invierte cada posicion.
     *
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector not() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Long> eq(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param l el {@code long}
     * @return el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Long> eq(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Long> lt(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param l el {@code long}
     * @return el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Long> lt(long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @return el {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> test(VectorOperators.Test test);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> test(VectorOperators.Test test, VectorMask<Long> vectorMask);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<Long>}
     * @return el {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> compare(VectorOperators.Comparison comparison,
            Vector<Long> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @return el {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Long> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector blend(Vector<Long> vector, VectorMask<Long> vectorMask);

    /**
     * Le suma a cada posicion su propio indice multiplicado por ese paso.
     *
     * @param i el {@code int}
     * @return el {@code LongVector}
     */
    public abstract LongVector addIndex(int i);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector blend(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector slice(int i, Vector<Long> vector);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector slice(int i, Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code LongVector}
     */
    public abstract LongVector slice(int i);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Long>}
     * @param i2 el {@code int}
     * @return el {@code LongVector}
     */
    public abstract LongVector unslice(int i, Vector<Long> vector, int i2);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Long>}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector unslice(int i, Vector<Long> vector, int i2,
            VectorMask<Long> vectorMask);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @return el {@code LongVector}
     */
    public abstract LongVector unslice(int i);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector rearrange(VectorShuffle<Long> vectorShuffle);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector rearrange(VectorShuffle<Long> vectorShuffle,
            VectorMask<Long> vectorMask);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Long>}
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector rearrange(VectorShuffle<Long> vectorShuffle, Vector<Long> vector);

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector compress(VectorMask<Long> vectorMask);

    /**
     * Reparte las posiciones del principio en los lugares que la mascara marca.
     *
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector expand(VectorMask<Long> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector selectFrom(Vector<Long> vector);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Long>}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector selectFrom(Vector<Long> vector, VectorMask<Long> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Long>}
     * @param vector2 el {@code Vector<Long>}
     * @return el {@code LongVector}
     */
    public abstract LongVector selectFrom(Vector<Long> vector, Vector<Long> vector2);

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Long>}
     * @param vector2 el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector bitwiseBlend(Vector<Long> vector, Vector<Long> vector2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param l el {@code long}
     * @param l2 el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector bitwiseBlend(long l, long l2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param l el {@code long}
     * @param vector el {@code Vector<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector bitwiseBlend(long l, Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Long>}
     * @param l el {@code long}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector bitwiseBlend(Vector<Long> vector, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @return el numero
     */
    public abstract long reduceLanes(VectorOperators.Associative associative);

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el numero
     */
    public abstract long reduceLanes(VectorOperators.Associative associative,
            VectorMask<Long> vectorMask);

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
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Long> vectorMask);

    /**
     * El valor de esa posicion.
     *
     * @param i el {@code int}
     * @return el numero
     */
    public abstract long lane(int i);

    /**
     * El mismo vector con esa posicion cambiada.
     *
     * @param i el {@code int}
     * @param l el {@code long}
     * @return el {@code LongVector}
     */
    public abstract LongVector withLane(int i, long l);

    /**
     * Las posiciones en un arreglo nuevo.
     *
     * @return el {@code long[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final long[] toArray() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Las posiciones en un arreglo de {@code int} nuevo.
     *
     * @return el {@code int[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final int[] toIntArray() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Las posiciones en un arreglo de {@code long} nuevo.
     *
     * @return el {@code long[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final long[] toLongArray() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Las posiciones en un arreglo de {@code double} nuevo.
     *
     * @return el {@code double[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final double[] toDoubleArray() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i,
            int[] is, int i2, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector fromMemorySegment(VectorSpecies<Long> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Long>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<Long>}
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static LongVector fromMemorySegment(VectorSpecies<Long> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(long[] ls, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(long[] ls, int i, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(long[] ls, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ls el {@code long[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(long[] ls, int i, int[] is, int i2, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en esa zona de memoria.
     *
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en esa zona de memoria.
     *
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<Long>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Los mismos bits leidos como {@code byte}.
     *
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector reinterpretAsBytes() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Los mismos bits vistos como el entero del mismo tamano.
     *
     * @return el {@code LongVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final LongVector viewAsIntegralLanes() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Los mismos bits vistos como el flotante del mismo tamano.
     *
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector viewAsFloatingLanes() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una representacion legible.
     *
     * @return el texto
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final String toString() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Si el otro es igual a este.
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
