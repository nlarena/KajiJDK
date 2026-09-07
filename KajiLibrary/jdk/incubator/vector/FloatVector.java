package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * Un vector de posiciones {@code float}.
 *
 * <p>Es una de las seis clases donde el API se vuelve concreto. {@link Vector} habla de posiciones
 * sin decir de que son y por eso sus metodos toman y devuelven {@code Object} o el tipo envuelto;
 * aca las posiciones son {@code float} de verdad, asi que se puede cargar desde un {@code float[]},
 * leer una posicion como {@code float} y operar sin envolver nada.
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
public abstract class FloatVector extends AbstractVector<Float> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    FloatVector(Object payload) {
        super(payload);
    }

    /** La especie de {@code float} de 64 bits. */
    public static final VectorSpecies<Float> SPECIES_64 =
            Especie.de(float.class, VectorShape.S_64_BIT);

    /** La especie de {@code float} de 128 bits. */
    public static final VectorSpecies<Float> SPECIES_128 =
            Especie.de(float.class, VectorShape.S_128_BIT);

    /** La especie de {@code float} de 256 bits. */
    public static final VectorSpecies<Float> SPECIES_256 =
            Especie.de(float.class, VectorShape.S_256_BIT);

    /** La especie de {@code float} de 512 bits. */
    public static final VectorSpecies<Float> SPECIES_512 =
            Especie.de(float.class, VectorShape.S_512_BIT);

    /** La especie de {@code float} de la forma mas grande de esta maquina. */
    public static final VectorSpecies<Float> SPECIES_MAX =
            Especie.de(float.class, VectorShape.S_Max_BIT);

    /** La especie de {@code float} de la forma que conviene en esta maquina. */
    public static final VectorSpecies<Float> SPECIES_PREFERRED =
            Especie.de(float.class, VectorShape.preferredShape());

    /**
     * Un vector con todas las posiciones en cero.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector zero(VectorSpecies<Float> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector broadcast(float f);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector broadcast(VectorSpecies<Float> vectorSpecies, float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector broadcast(long l);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param l el {@code long}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector broadcast(VectorSpecies<Float> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Unary unary);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Unary unary, VectorMask<Float> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Binary binary, Vector<Float> vector);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Binary binary, Vector<Float> vector,
            VectorMask<Float> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, float f,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Float>}
     * @param vector2 el {@code Vector<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector,
            Vector<Float> vector2);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Float>}
     * @param vector2 el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector,
            Vector<Float> vector2, VectorMask<Float> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param f el {@code float}
     * @param f2 el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f, float f2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param f el {@code float}
     * @param f2 el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f, float f2,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Float>}
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector, float f
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Float>}
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector,
            float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param f el {@code float}
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f, Vector<Float> vector
            ) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param f el {@code float}
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f,
            Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector add(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector add(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector add(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector add(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector sub(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector sub(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector sub(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector sub(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector mul(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector mul(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector mul(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector mul(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector div(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector div(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector div(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector div(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector min(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector min(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector max(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector max(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Cada posicion elevada a la que le corresponde del otro.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector pow(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Cada posicion elevada a la que le corresponde del otro.
     *
     * @param f el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector pow(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El opuesto de cada posicion.
     *
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector neg() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El valor absoluto de cada posicion.
     *
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector abs() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La raiz cuadrada de cada posicion.
     *
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector sqrt() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Float> eq(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param f el {@code float}
     * @return el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Float> eq(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Float> lt(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param f el {@code float}
     * @return el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Float> lt(float f) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @return el {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> test(VectorOperators.Test test);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> test(VectorOperators.Test test, VectorMask<Float> vectorMask);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<Float>}
     * @return el {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> compare(VectorOperators.Comparison comparison,
            Vector<Float> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param f el {@code float}
     * @return el {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> compare(VectorOperators.Comparison comparison, float f);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Float> compare(VectorOperators.Comparison comparison, float f,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @return el {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Float> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector blend(Vector<Float> vector, VectorMask<Float> vectorMask);

    /**
     * Le suma a cada posicion su propio indice multiplicado por ese paso.
     *
     * @param i el {@code int}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector addIndex(int i);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param f el {@code float}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector blend(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector blend(long l, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector slice(int i, Vector<Float> vector);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector slice(int i, Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector slice(int i);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Float>}
     * @param i2 el {@code int}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector unslice(int i, Vector<Float> vector, int i2);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Float>}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector unslice(int i, Vector<Float> vector, int i2,
            VectorMask<Float> vectorMask);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector unslice(int i);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector rearrange(VectorShuffle<Float> vectorShuffle);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector rearrange(VectorShuffle<Float> vectorShuffle,
            VectorMask<Float> vectorMask);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Float>}
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector rearrange(VectorShuffle<Float> vectorShuffle, Vector<Float> vector);

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector compress(VectorMask<Float> vectorMask);

    /**
     * Reparte las posiciones del principio en los lugares que la mascara marca.
     *
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector expand(VectorMask<Float> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector selectFrom(Vector<Float> vector);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Float>}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector selectFrom(Vector<Float> vector, VectorMask<Float> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Float>}
     * @param vector2 el {@code Vector<Float>}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector selectFrom(Vector<Float> vector, Vector<Float> vector2);

    /**
     * Multiplica y suma con un solo redondeo al final, no dos; ver {@code Math.fma}.
     *
     * @param vector el {@code Vector<Float>}
     * @param vector2 el {@code Vector<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector fma(Vector<Float> vector, Vector<Float> vector2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica y suma con un solo redondeo al final, no dos; ver {@code Math.fma}.
     *
     * @param f el {@code float}
     * @param f2 el {@code float}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final FloatVector fma(float f, float f2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @return el {@code float}
     */
    public abstract float reduceLanes(VectorOperators.Associative associative);

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code float}
     */
    public abstract float reduceLanes(VectorOperators.Associative associative,
            VectorMask<Float> vectorMask);

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
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Float> vectorMask);

    /**
     * El valor de esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code float}
     */
    public abstract float lane(int i);

    /**
     * El mismo vector con esa posicion cambiada.
     *
     * @param i el {@code int}
     * @param f el {@code float}
     * @return el {@code FloatVector}
     */
    public abstract FloatVector withLane(int i, float f);

    /**
     * Las posiciones en un arreglo nuevo.
     *
     * @return el {@code float[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final float[] toArray() {
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
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i,
            int[] is, int i2, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector fromMemorySegment(VectorSpecies<Float> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Float>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<Float>}
     * @return el {@code FloatVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static FloatVector fromMemorySegment(VectorSpecies<Float> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(float[] fs, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(float[] fs, int i, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(float[] fs, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param fs el {@code float[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(float[] fs, int i, int[] is, int i2, VectorMask<Float> vectorMask) {
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
     * @param vectorMask el {@code VectorMask<Float>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Float> vectorMask) {
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
