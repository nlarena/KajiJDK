package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.UnionLayout -- los miembros **superpuestos**, todos desde el
 * offset cero.
 *
 * <p>El tamanio es el del mas grande y el alineamiento el mas estricto. A diferencia del
 * {@link StructLayout} no hay regla de offsets que respetar: si todos empiezan en cero y el
 * alineamiento de la union es el maximo de los suyos, todos caen bien por construccion.
 */
public interface UnionLayout extends GroupLayout {

    UnionLayout withName(String name);

    UnionLayout withoutName();

    UnionLayout withByteAlignment(long byteAlignment);
}
