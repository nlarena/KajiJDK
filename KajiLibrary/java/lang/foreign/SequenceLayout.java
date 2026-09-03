package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.SequenceLayout -- N copias de un layout, una detras de la otra.
 *
 * <p>Es el arreglo de C. El tamanio es `N * elemento`, y el alineamiento **es el del elemento**: la
 * secuencia no impone uno propio porque, si cada elemento cae alineado, la secuencia entera tambien.
 */
public interface SequenceLayout extends MemoryLayout {

    /** El layout que se repite. */
    MemoryLayout elementLayout();

    /** Cuantas veces. */
    long elementCount();

    /**
     * La misma secuencia con otra cantidad de elementos.
     *
     * @throws IllegalArgumentException si es negativa, o si el tamanio total se pasa de `long`
     */
    SequenceLayout withElementCount(long elementCount);

    /**
     * Una secuencia de una sola dimension con los mismos elementos.
     *
     * <p>Aplana los niveles anidados: una secuencia de 3 secuencias de 4 `int` se vuelve una de 12.
     * Sirve para recorrer una matriz como si fuera plana, que es como esta en memoria.
     */
    SequenceLayout flatten();

    /**
     * La misma cantidad de elementos, repartida en las dimensiones que se pidan.
     *
     * <p>Una de las dimensiones puede ser `-1`: se deduce de las otras. Es la inversa de
     * {@link #flatten()}.
     *
     * @throws IllegalArgumentException si hay mas de un `-1`, si alguna no es positiva, o si el
     *     producto no da la cantidad de elementos que hay
     */
    SequenceLayout reshape(long... elementCounts);

    SequenceLayout withName(String name);

    SequenceLayout withoutName();

    SequenceLayout withByteAlignment(long byteAlignment);
}
