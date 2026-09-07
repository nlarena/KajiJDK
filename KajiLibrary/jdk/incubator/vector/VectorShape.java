package jdk.incubator.vector;

/**
 * El tamano en bits de un vector, sin decir de que son las posiciones.
 *
 * <h2>Por que el tamano va aparte del tipo</h2>
 *
 * <p>Un registro vectorial de 256 bits es 256 bits sea lo que sea que se guarde adentro: pueden ser
 * 32 {@code byte}, 8 {@code int} o 4 {@code double}. El hardware tiene registros de un ancho fijo, y
 * lo que cambia es en cuantos pedazos se lo mira.
 *
 * <p>Esa es la razon de que la forma y el tipo de posicion sean dos cosas separadas que se combinan:
 * {@link #withLanes(Class)} toma una forma y un tipo y da la especie concreta. Una especie es una
 * forma con las posiciones ya decididas.
 *
 * <h2>{@link #S_Max_BIT}</h2>
 *
 * <p>Las otras cuatro constantes nombran un tamano fijo. Esta nombra <strong>el mas grande que esta
 * maquina puede</strong>, que se sabe recien al ejecutar. Sirve para escribir codigo que aproveche
 * el hardware que le toque sin elegir un ancho a mano.
 *
 * <h2>Cuanto vale el maximo en esta VM</h2>
 *
 * <p>Vale 64, el minimo que el API admite. No es una eleccion: el maximo real sale de preguntarle a
 * los intrinsecos de la VM cuantas posiciones entran en un registro, y esta VM no los tiene. Decir
 * 512 seria mentir, y {@link #forBitSize(int)} devolveria una forma que despues no se puede llenar.
 *
 * <p>Por eso {@link #preferredShape()} y {@link #largestShapeFor(Class)} devuelven aca
 * {@link #S_64_BIT}. Los tamanos y las cuentas de esta clase son reales y se pueden usar; lo que no
 * hay es con que crear el vector.
 *
 * @since 16
 */
public enum VectorShape {

    /** Vectores de 64 bits. */
    S_64_BIT(64),
    /** Vectores de 128 bits. */
    S_128_BIT(128),
    /** Vectores de 256 bits. */
    S_256_BIT(256),
    /** Vectores de 512 bits. */
    S_512_BIT(512),
    /** El vector mas grande que soporta esta maquina; aca, 64 bits. */
    S_Max_BIT(Limite.MAXIMO);

    /**
     * El maximo que esta VM puede sostener.
     *
     * <p>En el JDK sale de {@code VectorSupport.getMaxLaneCount}, que es un intrinseco. Aca es la
     * constante que el API pone como piso, porque no hay a quien preguntarle.
     *
     * <p>Va en una clase aparte y no como campo de este enum porque el argumento de una constante de
     * enum no puede leer un campo estatico del mismo enum: cuando se construyen las constantes la
     * clase todavia no termino de inicializarse, asi que el lenguaje lo prohibe.
     */
    static final class Limite {
        static final int MAXIMO = 64;

        private Limite() {
        }
    }

    private final int vectorBitSize;

    VectorShape(final int vectorBitSize) {
        this.vectorBitSize = vectorBitSize;
    }

    /**
     * El tamano del vector en bits.
     *
     * @return los bits
     */
    public int vectorBitSize() {
        return vectorBitSize;
    }

    /**
     * La especie que resulta de llenar esta forma con posiciones de ese tipo.
     *
     * @param <E> el tipo de la posicion, en su version envuelta
     * @param elementType el tipo de la posicion
     * @return la especie
     * @throws UnsupportedOperationException en esta biblioteca; ver la nota de la clase
     */
    public <E> VectorSpecies<E> withLanes(final Class<E> elementType) {
        return VectorSpecies.of(elementType, this);
    }

    /**
     * La forma de ese tamano en bits.
     *
     * <p>Los cuatro tamanos fijos se reconocen siempre. Cualquier otro solo vale si es un multiplo
     * de 128 que no pasa de 2048, y entonces la respuesta es {@link #S_Max_BIT}: es el unico caso en
     * que un tamano que no esta nombrado igual existe, porque el maximo de la maquina puede ser
     * cualquiera de esos.
     *
     * @param bitSize el tamano en bits
     * @return la forma
     * @throws IllegalArgumentException si no hay forma de ese tamano
     */
    public static VectorShape forBitSize(final int bitSize) {
        switch (bitSize) {
            case 64:
                return S_64_BIT;
            case 128:
                return S_128_BIT;
            case 256:
                return S_256_BIT;
            case 512:
                return S_512_BIT;
            default:
                if (bitSize > 0 && bitSize <= 2048 && bitSize % 128 == 0) {
                    return S_Max_BIT;
                }
                throw new IllegalArgumentException("Bad vector bit-size: " + bitSize);
        }
    }

    /**
     * La forma cuyo vector de indices de ese tamano de posicion mide esos bits.
     *
     * <p>Un vector de indices acompana a otro vector diciendo de donde sale cada posicion, y sus
     * indices son siempre {@code int}. Si el vector original tiene posiciones de 8 bits, el de
     * indices necesita cuatro veces mas espacio para las mismas posiciones. Este metodo hace esa
     * cuenta al reves: dado el tamano que ocupa el vector de indices, devuelve la forma del otro.
     *
     * <p>Por eso 32 y 64 dan los dos {@link #S_64_BIT}: un solo indice de 32 bits ya corresponde al
     * vector mas chico.
     *
     * @param bitSize el tamano del vector de indices, en bits
     * @param elementSize el tamano de la posicion del vector original, en bits
     * @return la forma
     * @throws IllegalArgumentException si no hay forma que corresponda
     */
    public static VectorShape forIndexBitSize(final int bitSize, final int elementSize) {
        switch (bitSize) {
            case 32:
            case 64:
                return S_64_BIT;
            case 128:
                return S_128_BIT;
            case 256:
                return S_256_BIT;
            case 512:
                return S_512_BIT;
            default:
                final int max = 2048 / elementSize * 32;
                final int min = 128 / elementSize * 32;
                if (bitSize > 0 && bitSize <= max && bitSize % min == 0) {
                    return S_Max_BIT;
                }
                throw new IllegalArgumentException("Bad vector index bit-size: " + bitSize);
        }
    }

    /**
     * La forma mas grande que esta maquina puede con posiciones de ese tipo.
     *
     * @param etype el tipo de la posicion
     * @return la forma mas grande
     */
    public static VectorShape largestShapeFor(final Class<?> etype) {
        return forBitSize(maximoPara(etype));
    }

    /**
     * La forma que conviene usar en esta maquina.
     *
     * <p>Es la mas grande que sirve para <strong>todos</strong> los tipos de posicion, no la mas
     * grande a secas: un codigo que mezcla {@code byte} y {@code double} necesita una forma que los
     * dos puedan.
     *
     * @return la forma preferida
     */
    public static VectorShape preferredShape() {
        return forBitSize(Limite.MAXIMO);
    }

    /**
     * El maximo en bits para ese tipo de posicion.
     *
     * <p>El {@code etype} se valida aunque el resultado no dependa de el: el metodo publico que
     * llama aca tiene que rechazar un tipo que no es de posicion, y no hacerlo seria aceptar
     * {@code largestShapeFor(String.class)}.
     */
    private static int maximoPara(final Class<?> etype) {
        if (etype != byte.class && etype != short.class && etype != int.class
                && etype != long.class && etype != float.class && etype != double.class) {
            throw new IllegalArgumentException("Bad vector element type: " + etype);
        }
        return Limite.MAXIMO;
    }
}
