package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * Un vector de posiciones {@code short}.
 *
 * <p>Es una de las seis clases donde el API se vuelve concreto. {@link Vector} habla de posiciones
 * sin decir de que son y por eso sus metodos toman y devuelven {@code Object} o el tipo envuelto;
 * aca las posiciones son {@code short} de verdad, asi que se puede cargar desde un {@code short[]},
 * leer una posicion como {@code short} y operar sin envolver nada.
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
public abstract class ShortVector extends AbstractVector<Short> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    ShortVector(Object payload) {
        super(payload);
    }

    /** La especie de {@code short} de 64 bits. */
    public static final VectorSpecies<Short> SPECIES_64 =
            Especie.de(short.class, VectorShape.S_64_BIT);

    /** La especie de {@code short} de 128 bits. */
    public static final VectorSpecies<Short> SPECIES_128 =
            Especie.de(short.class, VectorShape.S_128_BIT);

    /** La especie de {@code short} de 256 bits. */
    public static final VectorSpecies<Short> SPECIES_256 =
            Especie.de(short.class, VectorShape.S_256_BIT);

    /** La especie de {@code short} de 512 bits. */
    public static final VectorSpecies<Short> SPECIES_512 =
            Especie.de(short.class, VectorShape.S_512_BIT);

    /** La especie de {@code short} de la forma mas grande de esta maquina. */
    public static final VectorSpecies<Short> SPECIES_MAX =
            Especie.de(short.class, VectorShape.S_Max_BIT);

    /** La especie de {@code short} de la forma que conviene en esta maquina. */
    public static final VectorSpecies<Short> SPECIES_PREFERRED =
            Especie.de(short.class, VectorShape.preferredShape());

    /**
     * Un vector con todas las posiciones en cero.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector zero(VectorSpecies<Short> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector broadcast(short s);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector broadcast(VectorSpecies<Short> vectorSpecies, short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector broadcast(long l);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param l el {@code long}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector broadcast(VectorSpecies<Short> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Unary unary);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Unary unary, VectorMask<Short> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Binary binary, Vector<Short> vector);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Binary binary, Vector<Short> vector,
            VectorMask<Short> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, short s,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Short>}
     * @param vector2 el {@code Vector<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector,
            Vector<Short> vector2);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Short>}
     * @param vector2 el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector,
            Vector<Short> vector2, VectorMask<Short> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param s el {@code short}
     * @param s2 el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s, short s2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param s el {@code short}
     * @param s2 el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s, short s2,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Short>}
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector, short s
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Short>}
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector,
            short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param s el {@code short}
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s, Vector<Short> vector
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param s el {@code short}
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s,
            Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector add(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector add(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector add(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector add(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector sub(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector sub(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector sub(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector sub(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector mul(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector mul(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector mul(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector mul(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector div(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector div(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector div(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector div(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector min(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector min(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector max(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector max(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector and(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector and(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector or(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector or(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El opuesto de cada posicion.
     *
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector neg() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El valor absoluto de cada posicion.
     *
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector abs() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Invierte cada posicion.
     *
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector not() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Short> eq(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param s el {@code short}
     * @return el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Short> eq(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Short> lt(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param s el {@code short}
     * @return el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Short> lt(short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @return el {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> test(VectorOperators.Test test);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> test(VectorOperators.Test test, VectorMask<Short> vectorMask);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<Short>}
     * @return el {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> compare(VectorOperators.Comparison comparison,
            Vector<Short> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param s el {@code short}
     * @return el {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> compare(VectorOperators.Comparison comparison, short s);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Short> compare(VectorOperators.Comparison comparison, short s,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @return el {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Short> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector blend(Vector<Short> vector, VectorMask<Short> vectorMask);

    /**
     * Le suma a cada posicion su propio indice multiplicado por ese paso.
     *
     * @param i el {@code int}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector addIndex(int i);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param s el {@code short}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector blend(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector blend(long l, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector slice(int i, Vector<Short> vector);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector slice(int i, Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector slice(int i);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Short>}
     * @param i2 el {@code int}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector unslice(int i, Vector<Short> vector, int i2);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Short>}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector unslice(int i, Vector<Short> vector, int i2,
            VectorMask<Short> vectorMask);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector unslice(int i);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector rearrange(VectorShuffle<Short> vectorShuffle);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector rearrange(VectorShuffle<Short> vectorShuffle,
            VectorMask<Short> vectorMask);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Short>}
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector rearrange(VectorShuffle<Short> vectorShuffle, Vector<Short> vector);

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector compress(VectorMask<Short> vectorMask);

    /**
     * Reparte las posiciones del principio en los lugares que la mascara marca.
     *
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector expand(VectorMask<Short> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector selectFrom(Vector<Short> vector);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Short>}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector selectFrom(Vector<Short> vector, VectorMask<Short> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Short>}
     * @param vector2 el {@code Vector<Short>}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector selectFrom(Vector<Short> vector, Vector<Short> vector2);

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Short>}
     * @param vector2 el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector bitwiseBlend(Vector<Short> vector, Vector<Short> vector2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param s el {@code short}
     * @param s2 el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector bitwiseBlend(short s, short s2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param s el {@code short}
     * @param vector el {@code Vector<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector bitwiseBlend(short s, Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Short>}
     * @param s el {@code short}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector bitwiseBlend(Vector<Short> vector, short s) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @return el {@code short}
     */
    public abstract short reduceLanes(VectorOperators.Associative associative);

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code short}
     */
    public abstract short reduceLanes(VectorOperators.Associative associative,
            VectorMask<Short> vectorMask);

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
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Short> vectorMask);

    /**
     * El valor de esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code short}
     */
    public abstract short lane(int i);

    /**
     * El mismo vector con esa posicion cambiada.
     *
     * @param i el {@code int}
     * @param s el {@code short}
     * @return el {@code ShortVector}
     */
    public abstract ShortVector withLane(int i, short s);

    /**
     * Las posiciones en un arreglo nuevo.
     *
     * @return el {@code short[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final short[] toArray() {
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
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i,
            int[] is, int i2, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo de caracteres.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo de caracteres.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo de caracteres.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo de caracteres.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i,
            int[] is, int i2, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromMemorySegment(VectorSpecies<Short> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Short>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<Short>}
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ShortVector fromMemorySegment(VectorSpecies<Short> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(short[] ss, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(short[] ss, int i, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(short[] ss, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ss el {@code short[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(short[] ss, int i, int[] is, int i2, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo de caracteres.
     *
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoCharArray(char[] chars, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo de caracteres.
     *
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoCharArray(char[] chars, int i, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo de caracteres.
     *
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoCharArray(char[] chars, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo de caracteres.
     *
     * @param chars el {@code char[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoCharArray(char[] chars, int i, int[] is, int i2,
            VectorMask<Short> vectorMask) {
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
     * @param vectorMask el {@code VectorMask<Short>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Short> vectorMask) {
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
     * @return el {@code ShortVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ShortVector viewAsIntegralLanes() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Los mismos bits vistos como el flotante del mismo tamano.
     *
     * @return el {@code Vector<?>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final Vector<?> viewAsFloatingLanes() {
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
