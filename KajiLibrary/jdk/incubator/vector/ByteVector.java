package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * Un vector de posiciones {@code byte}.
 *
 * <p>Es una de las seis clases donde el API se vuelve concreto. {@link Vector} habla de posiciones
 * sin decir de que son y por eso sus metodos toman y devuelven {@code Object} o el tipo envuelto;
 * aca las posiciones son {@code byte} de verdad, asi que se puede cargar desde un {@code byte[]},
 * leer una posicion como {@code byte} y operar sin envolver nada.
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
public abstract class ByteVector extends AbstractVector<Byte> {

    /**
     * Con esa carga util.
     *
     * <p>En el JDK este constructor es de paquete, asi que no aparece en los volcados. Va escrito
     * igual porque la superclase no tiene uno sin argumentos: sin el, javac genera uno que llama a
     * un {@code super()} que no existe, y el archivo compilado queda invalido (hallazgo #515).
     *
     * @param payload el arreglo de posiciones
     */
    ByteVector(Object payload) {
        super(payload);
    }

    /** La especie de {@code byte} de 64 bits. */
    public static final VectorSpecies<Byte> SPECIES_64 =
            Especie.de(byte.class, VectorShape.S_64_BIT);

    /** La especie de {@code byte} de 128 bits. */
    public static final VectorSpecies<Byte> SPECIES_128 =
            Especie.de(byte.class, VectorShape.S_128_BIT);

    /** La especie de {@code byte} de 256 bits. */
    public static final VectorSpecies<Byte> SPECIES_256 =
            Especie.de(byte.class, VectorShape.S_256_BIT);

    /** La especie de {@code byte} de 512 bits. */
    public static final VectorSpecies<Byte> SPECIES_512 =
            Especie.de(byte.class, VectorShape.S_512_BIT);

    /** La especie de {@code byte} de la forma mas grande de esta maquina. */
    public static final VectorSpecies<Byte> SPECIES_MAX =
            Especie.de(byte.class, VectorShape.S_Max_BIT);

    /** La especie de {@code byte} de la forma que conviene en esta maquina. */
    public static final VectorSpecies<Byte> SPECIES_PREFERRED =
            Especie.de(byte.class, VectorShape.preferredShape());

    /**
     * Un vector con todas las posiciones en cero.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector zero(VectorSpecies<Byte> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector broadcast(byte b);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector broadcast(VectorSpecies<Byte> vectorSpecies, byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param l el {@code long}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector broadcast(long l);

    /**
     * Un vector con el mismo valor en todas las posiciones.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param l el {@code long}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector broadcast(VectorSpecies<Byte> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Unary unary);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param unary el {@code VectorOperators.Unary}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Unary unary, VectorMask<Byte> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Binary binary, Vector<Byte> vector);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Binary binary, Vector<Byte> vector,
            VectorMask<Byte> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, byte b,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param binary el {@code VectorOperators.Binary}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Byte>}
     * @param vector2 el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector,
            Vector<Byte> vector2);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Byte>}
     * @param vector2 el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector,
            Vector<Byte> vector2, VectorMask<Byte> vectorMask);

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param b el {@code byte}
     * @param b2 el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, byte b2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param b el {@code byte}
     * @param b2 el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, byte b2,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Byte>}
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector, byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param vector el {@code Vector<Byte>}
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector, byte b,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param b el {@code byte}
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Aplica ese operador a cada posicion.
     *
     * @param ternary el {@code VectorOperators.Ternary}
     * @param b el {@code byte}
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, Vector<Byte> vector,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector add(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector add(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector add(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Suma posicion a posicion.
     *
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector add(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector sub(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector sub(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector sub(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Resta posicion a posicion.
     *
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector sub(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector mul(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector mul(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector mul(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Multiplica posicion a posicion.
     *
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector mul(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector div(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector div(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector div(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Divide posicion a posicion.
     *
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector div(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector min(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El menor de cada par de posiciones.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector min(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector max(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El mayor de cada par de posiciones.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector max(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector and(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La conjuncion bit a bit.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector and(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector or(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La disyuncion bit a bit.
     *
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector or(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El opuesto de cada posicion.
     *
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector neg() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * El valor absoluto de cada posicion.
     *
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector abs() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Invierte cada posicion.
     *
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector not() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Byte> eq(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones iguales.
     *
     * @param b el {@code byte}
     * @return el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Byte> eq(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Byte> lt(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones menores.
     *
     * @param b el {@code byte}
     * @return el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Byte> lt(byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @return el {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> test(VectorOperators.Test test);

    /**
     * La mascara de las posiciones que cumplen esa prueba.
     *
     * @param test el {@code VectorOperators.Test}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> test(VectorOperators.Test test, VectorMask<Byte> vectorMask);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param vector el {@code Vector<Byte>}
     * @return el {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> compare(VectorOperators.Comparison comparison,
            Vector<Byte> vector);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param b el {@code byte}
     * @return el {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> compare(VectorOperators.Comparison comparison, byte b);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Byte> compare(VectorOperators.Comparison comparison, byte b,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @return el {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compara posicion a posicion y devuelve la mascara del resultado.
     *
     * @param comparison el {@code VectorOperators.Comparison}
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final VectorMask<Byte> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector blend(Vector<Byte> vector, VectorMask<Byte> vectorMask);

    /**
     * Le suma a cada posicion su propio indice multiplicado por ese paso.
     *
     * @param i el {@code int}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector addIndex(int i);

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param b el {@code byte}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector blend(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Mezcla dos vectores tomando de uno o del otro segun la mascara.
     *
     * @param l el {@code long}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector blend(long l, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector slice(int i, Vector<Byte> vector);

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector slice(int i, Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector que arranca en esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector slice(int i);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Byte>}
     * @param i2 el {@code int}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector unslice(int i, Vector<Byte> vector, int i2);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @param vector el {@code Vector<Byte>}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector unslice(int i, Vector<Byte> vector, int i2,
            VectorMask<Byte> vectorMask);

    /**
     * La operacion inversa de {@code slice}: devuelve las posiciones a su lugar.
     *
     * @param i el {@code int}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector unslice(int i);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector rearrange(VectorShuffle<Byte> vectorShuffle);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector rearrange(VectorShuffle<Byte> vectorShuffle,
            VectorMask<Byte> vectorMask);

    /**
     * Reordena las posiciones segun ese barajado.
     *
     * @param vectorShuffle el {@code VectorShuffle<Byte>}
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector rearrange(VectorShuffle<Byte> vectorShuffle, Vector<Byte> vector);

    /**
     * Junta las posiciones prendidas al principio.
     *
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector compress(VectorMask<Byte> vectorMask);

    /**
     * Reparte las posiciones del principio en los lugares que la mascara marca.
     *
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector expand(VectorMask<Byte> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector selectFrom(Vector<Byte> vector);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector selectFrom(Vector<Byte> vector, VectorMask<Byte> vectorMask);

    /**
     * Toma de otro vector las posiciones que este indica.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vector2 el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector selectFrom(Vector<Byte> vector, Vector<Byte> vector2);

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Byte>}
     * @param vector2 el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector bitwiseBlend(Vector<Byte> vector, Vector<Byte> vector2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param b el {@code byte}
     * @param b2 el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector bitwiseBlend(byte b, byte b2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param b el {@code byte}
     * @param vector el {@code Vector<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector bitwiseBlend(byte b, Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Elige bit a bit entre dos vectores segun un tercero.
     *
     * @param vector el {@code Vector<Byte>}
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector bitwiseBlend(Vector<Byte> vector, byte b) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @return el {@code byte}
     */
    public abstract byte reduceLanes(VectorOperators.Associative associative);

    /**
     * Combina todas las posiciones en un solo valor con ese operador.
     *
     * @param associative el {@code VectorOperators.Associative}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code byte}
     */
    public abstract byte reduceLanes(VectorOperators.Associative associative,
            VectorMask<Byte> vectorMask);

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
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el numero
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Byte> vectorMask);

    /**
     * El valor de esa posicion.
     *
     * @param i el {@code int}
     * @return el {@code byte}
     */
    public abstract byte lane(int i);

    /**
     * El mismo vector con esa posicion cambiada.
     *
     * @param i el {@code int}
     * @param b el {@code byte}
     * @return el {@code ByteVector}
     */
    public abstract ByteVector withLane(int i, byte b);

    /**
     * Las posiciones en un arreglo nuevo.
     *
     * @return el {@code byte[]}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final byte[] toArray() {
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
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de ese arreglo.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i,
            int[] is, int i2, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una mascara leida de ese arreglo de banderas.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una mascara leida de ese arreglo de banderas.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una mascara leida de ese arreglo de banderas.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Una mascara leida de ese arreglo de banderas.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i, int[] is, int i2, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromMemorySegment(VectorSpecies<Byte> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Un vector leido de esa zona de memoria.
     *
     * @param vectorSpecies el {@code VectorSpecies<Byte>}
     * @param memorySegment el {@code java.lang.foreign.MemorySegment}
     * @param l el {@code long}
     * @param byteOrder el {@code java.nio.ByteOrder}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public static ByteVector fromMemorySegment(VectorSpecies<Byte> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(byte[] bs, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(byte[] bs, int i, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(byte[] bs, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe el vector en ese arreglo.
     *
     * @param bs el {@code byte[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoArray(byte[] bs, int i, int[] is, int i2, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe la mascara en ese arreglo de banderas.
     *
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoBooleanArray(boolean[] flags, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe la mascara en ese arreglo de banderas.
     *
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoBooleanArray(boolean[] flags, int i, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe la mascara en ese arreglo de banderas.
     *
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoBooleanArray(boolean[] flags, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Escribe la mascara en ese arreglo de banderas.
     *
     * @param flags el {@code boolean[]}
     * @param i el {@code int}
     * @param is el {@code int[]}
     * @param i2 el {@code int}
     * @param vectorMask el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoBooleanArray(boolean[] flags, int i, int[] is, int i2,
            VectorMask<Byte> vectorMask) {
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
     * @param vectorMask el {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Byte> vectorMask) {
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
     * @return el {@code ByteVector}
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public final ByteVector viewAsIntegralLanes() {
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
