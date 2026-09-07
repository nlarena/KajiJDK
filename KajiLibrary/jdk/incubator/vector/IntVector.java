package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * Un vector de posiciones {@code int}.
 *
 * <p>Es una de las seis clases donde el API se vuelve concreto. {@link Vector} habla de posiciones
 * sin decir de que son y por eso sus metodos toman y devuelven {@code Object} o el tipo envuelto;
 * aca las posiciones son {@code int} de verdad, asi que se puede cargar desde un {@code int[]},
 * leer una posicion como {@code int} y operar sin envolver nada.
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
public abstract class IntVector extends AbstractVector<Integer> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    IntVector(Object payload) {
        super(payload);
    }

    /** La especie de {@code int} de 64 bits. */
    public static final VectorSpecies<Integer> SPECIES_64 =
            Especie.de(int.class, VectorShape.S_64_BIT);

    /** La especie de {@code int} de 128 bits. */
    public static final VectorSpecies<Integer> SPECIES_128 =
            Especie.de(int.class, VectorShape.S_128_BIT);

    /** La especie de {@code int} de 256 bits. */
    public static final VectorSpecies<Integer> SPECIES_256 =
            Especie.de(int.class, VectorShape.S_256_BIT);

    /** La especie de {@code int} de 512 bits. */
    public static final VectorSpecies<Integer> SPECIES_512 =
            Especie.de(int.class, VectorShape.S_512_BIT);

    /** La especie de {@code int} de la forma mas grande de esta maquina. */
    public static final VectorSpecies<Integer> SPECIES_MAX =
            Especie.de(int.class, VectorShape.S_Max_BIT);

    /** La especie de {@code int} de la forma que conviene en esta maquina. */
    public static final VectorSpecies<Integer> SPECIES_PREFERRED =
            Especie.de(int.class, VectorShape.preferredShape());

    /**
     * Un vector con todas las posiciones en cero.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector zero(VectorSpecies<Integer> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     */
    public abstract IntVector broadcast(int i);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector broadcast(VectorSpecies<Integer> vectorSpecies, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code IntVector}
     */
    public abstract IntVector broadcast(long l);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param l el {@code long}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector broadcast(VectorSpecies<Integer> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @return el {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Unary unary);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Unary unary, VectorMask<Integer> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Binary binary, Vector<Integer> vector);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Binary binary, Vector<Integer> vector,
            VectorMask<Integer> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Binary binary, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Binary binary, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Integer>}
     * @param vector2 el {@code Vector<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector,
            Vector<Integer> vector2);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Integer>}
     * @param vector2 el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector,
            Vector<Integer> vector2, VectorMask<Integer> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, int i2,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Integer>}
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector, int i
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Integer>}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param i el {@code int}
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, Vector<Integer> vector
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param i el {@code int}
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, Vector<Integer> vector,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector add(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector add(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector add(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector add(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector sub(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector sub(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector sub(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector sub(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector mul(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector mul(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector mul(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector mul(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector div(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector div(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector div(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector div(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector min(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector min(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector max(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector max(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector and(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector and(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector or(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector or(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El opuesto de cada posicion.
     *
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector neg() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El valor absoluto de cada posicion.
     *
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector abs() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Invierte cada posicion.
     *
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector not() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Integer> eq(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param i el {@code int}
     * @return el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Integer> eq(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Integer> lt(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param i el {@code int}
     * @return el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Integer> lt(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @return el {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> test(VectorOperators.Test test);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> test(VectorOperators.Test test,
            VectorMask<Integer> vectorMask);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<Integer>}
     * @return el {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> compare(VectorOperators.Comparison comparison,
            Vector<Integer> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param i el {@code int}
     * @return el {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> compare(VectorOperators.Comparison comparison, int i);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Integer> compare(VectorOperators.Comparison comparison, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @return el {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Integer> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector blend(Vector<Integer> vector, VectorMask<Integer> vectorMask);

    /**
     * Le suma a cada posicion su propio indice multiplicado por ese paso.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     */
    public abstract IntVector addIndex(int i);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector blend(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector blend(long l, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector slice(int i, Vector<Integer> vector);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector slice(int i, Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     */
    public abstract IntVector slice(int i);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Integer>}
     * @param i2 el {@code int}
     * @return el {@code IntVector}
     */
    public abstract IntVector unslice(int i, Vector<Integer> vector, int i2);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Integer>}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector unslice(int i, Vector<Integer> vector, int i2,
            VectorMask<Integer> vectorMask);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @return el {@code IntVector}
     */
    public abstract IntVector unslice(int i);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector rearrange(VectorShuffle<Integer> vectorShuffle);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector rearrange(VectorShuffle<Integer> vectorShuffle,
            VectorMask<Integer> vectorMask);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Integer>}
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector rearrange(VectorShuffle<Integer> vectorShuffle, Vector<Integer> vector
            );

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector compress(VectorMask<Integer> vectorMask);

    /**
     * Reparte las posiciones del principio en los lugares que la mascara marca.
     *
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector expand(VectorMask<Integer> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector selectFrom(Vector<Integer> vector);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector selectFrom(Vector<Integer> vector, VectorMask<Integer> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vector2 el {@code Vector<Integer>}
     * @return el {@code IntVector}
     */
    public abstract IntVector selectFrom(Vector<Integer> vector, Vector<Integer> vector2);

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Integer>}
     * @param vector2 el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector bitwiseBlend(Vector<Integer> vector, Vector<Integer> vector2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector bitwiseBlend(int i, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector bitwiseBlend(int i, Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Integer>}
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector bitwiseBlend(Vector<Integer> vector, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @return el numero
     */
    public abstract int reduceLanes(VectorOperators.Associative associative);

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el numero
     */
    public abstract int reduceLanes(VectorOperators.Associative associative,
            VectorMask<Integer> vectorMask);

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
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Integer> vectorMask);

    /**
     * El valor de esa posicion.
     *
     * @param i el {@code int}
     * @return el numero
     */
    public abstract int lane(int i);

    /**
     * El mismo vector con esa posicion cambiada.
     *
     * @param i el {@code int}
     * @param i2 el {@code int}
     * @return el {@code IntVector}
     */
    public abstract IntVector withLane(int i, int i2);

    /**
     * Las posiciones en un arreglo nuevo.
     *
     * @return el {@code int[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final int[] toArray() {
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
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @param is2 el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i,
            int[] is2, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @param is2 el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i,
            int[] is2, int i2, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector fromMemorySegment(VectorSpecies<Integer> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Integer>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static IntVector fromMemorySegment(VectorSpecies<Integer> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(int[] is, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(int[] is, int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @param is2 el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(int[] is, int i, int[] is2, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param is el {@code int[]}
     * @param i el {@code int}
     * @param is2 el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(int[] is, int i, int[] is2, int i2, VectorMask<Integer> vectorMask
            ) {
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
     * @param vectorMask el {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Integer> vectorMask) {
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
     * @return el {@code IntVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final IntVector viewAsIntegralLanes() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Los mismos bits vistos como el flotante del mismo tamano.
     *
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector viewAsFloatingLanes() {
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
