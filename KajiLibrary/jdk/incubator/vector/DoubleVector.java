package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * Un vector de posiciones {@code double}.
 *
 * <p>Es una de las seis clases donde el API se vuelve concreto. {@link Vector} habla de posiciones
 * sin decir de que son y por eso sus metodos toman y devuelven {@code Object} o el tipo envuelto;
 * aca las posiciones son {@code double} de verdad, asi que se puede cargar desde un
 * {@code double[]}, leer una posicion como {@code double} y operar sin envolver nada.
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
public abstract class DoubleVector extends AbstractVector<Double> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    DoubleVector(Object payload) {
        super(payload);
    }

    /** La especie de {@code double} de 64 bits. */
    public static final VectorSpecies<Double> SPECIES_64 =
            Especie.de(double.class, VectorShape.S_64_BIT);

    /** La especie de {@code double} de 128 bits. */
    public static final VectorSpecies<Double> SPECIES_128 =
            Especie.de(double.class, VectorShape.S_128_BIT);

    /** La especie de {@code double} de 256 bits. */
    public static final VectorSpecies<Double> SPECIES_256 =
            Especie.de(double.class, VectorShape.S_256_BIT);

    /** La especie de {@code double} de 512 bits. */
    public static final VectorSpecies<Double> SPECIES_512 =
            Especie.de(double.class, VectorShape.S_512_BIT);

    /** La especie de {@code double} de la forma mas grande de esta maquina. */
    public static final VectorSpecies<Double> SPECIES_MAX =
            Especie.de(double.class, VectorShape.S_Max_BIT);

    /** La especie de {@code double} de la forma que conviene en esta maquina. */
    public static final VectorSpecies<Double> SPECIES_PREFERRED =
            Especie.de(double.class, VectorShape.preferredShape());

    /**
     * Un vector con todas las posiciones en cero.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector zero(VectorSpecies<Double> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector broadcast(double d);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector broadcast(VectorSpecies<Double> vectorSpecies, double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector broadcast(long l);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param l el {@code long}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector broadcast(VectorSpecies<Double> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Unary unary);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Unary unary, VectorMask<Double> vectorMask
            );

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Binary binary, Vector<Double> vector);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Binary binary, Vector<Double> vector,
            VectorMask<Double> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, double d,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Double>}
     * @param vector2 el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            Vector<Double> vector2);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Double>}
     * @param vector2 el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            Vector<Double> vector2, VectorMask<Double> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param d el {@code double}
     * @param d2 el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d, double d2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param d el {@code double}
     * @param d2 el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d, double d2,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Double>}
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Double>}
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param d el {@code double}
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d,
            Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param d el {@code double}
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d,
            Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector add(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector add(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector add(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector add(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector sub(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector sub(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector sub(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector sub(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector mul(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector mul(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector mul(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector mul(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector div(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector div(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector div(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector div(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector min(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector min(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector max(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector max(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Cada posicion elevada a la que le corresponde del otro.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector pow(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Cada posicion elevada a la que le corresponde del otro.
     *
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector pow(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El opuesto de cada posicion.
     *
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector neg() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El valor absoluto de cada posicion.
     *
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector abs() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La raiz cuadrada de cada posicion.
     *
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector sqrt() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Double> eq(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param d el {@code double}
     * @return el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Double> eq(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Double> lt(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param d el {@code double}
     * @return el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Double> lt(double d) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @return el {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> test(VectorOperators.Test test);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> test(VectorOperators.Test test, VectorMask<Double> vectorMask
            );

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<Double>}
     * @return el {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> compare(VectorOperators.Comparison comparison,
            Vector<Double> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param d el {@code double}
     * @return el {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> compare(VectorOperators.Comparison comparison, double d);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Double> compare(VectorOperators.Comparison comparison, double d,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @return el {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Double> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector blend(Vector<Double> vector, VectorMask<Double> vectorMask);

    /**
     * Le suma a cada posicion su propio indice multiplicado por ese paso.
     *
     * @param i el {@code int}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector addIndex(int i);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param d el {@code double}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector blend(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector blend(long l, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector slice(int i, Vector<Double> vector);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector slice(int i, Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector slice(int i);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Double>}
     * @param i2 el {@code int}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector unslice(int i, Vector<Double> vector, int i2);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Double>}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector unslice(int i, Vector<Double> vector, int i2,
            VectorMask<Double> vectorMask);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector unslice(int i);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector rearrange(VectorShuffle<Double> vectorShuffle);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector rearrange(VectorShuffle<Double> vectorShuffle,
            VectorMask<Double> vectorMask);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Double>}
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector rearrange(VectorShuffle<Double> vectorShuffle,
            Vector<Double> vector);

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector compress(VectorMask<Double> vectorMask);

    /**
     * Reparte las posiciones del principio en los lugares que la mascara marca.
     *
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector expand(VectorMask<Double> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector selectFrom(Vector<Double> vector);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Double>}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector selectFrom(Vector<Double> vector, VectorMask<Double> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Double>}
     * @param vector2 el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector selectFrom(Vector<Double> vector, Vector<Double> vector2);

    /**
     * Multiplica y suma con un solo redondeo al final, no dos; ver {@code Math.fma}.
     *
     * @param vector el {@code Vector<Double>}
     * @param vector2 el {@code Vector<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector fma(Vector<Double> vector, Vector<Double> vector2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica y suma con un solo redondeo al final, no dos; ver {@code Math.fma}.
     *
     * @param d el {@code double}
     * @param d2 el {@code double}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final DoubleVector fma(double d, double d2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @return el {@code double}
     */
    public abstract double reduceLanes(VectorOperators.Associative associative);

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code double}
     */
    public abstract double reduceLanes(VectorOperators.Associative associative,
            VectorMask<Double> vectorMask);

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
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Double> vectorMask);

    /**
     * El valor de esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code double}
     */
    public abstract double lane(int i);

    /**
     * El mismo vector con esa posicion cambiada.
     *
     * @param i el {@code int}
     * @param d el {@code double}
     * @return el {@code DoubleVector}
     */
    public abstract DoubleVector withLane(int i, double d);

    /**
     * Las posiciones en un arreglo nuevo.
     *
     * @return el {@code double[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final double[] toArray() {
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
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i,
            int[] is, int i2, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector fromMemorySegment(VectorSpecies<Double> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Double>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<Double>}
     * @return el {@code DoubleVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static DoubleVector fromMemorySegment(VectorSpecies<Double> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(double[] ds, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(double[] ds, int i, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(double[] ds, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param ds el {@code double[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(double[] ds, int i, int[] is, int i2, VectorMask<Double> vectorMask
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
     * @param vectorMask el {@code VectorMask<Double>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Double> vectorMask) {
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
