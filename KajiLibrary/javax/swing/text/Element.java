package javax.swing.text;

/**
 * Un nodo de la estructura de un documento: un parrafo, una linea, un tramo con el mismo formato.
 *
 * <h2>Dos vistas del mismo texto</h2>
 *
 * <p>Un documento es a la vez una secuencia plana de caracteres y un arbol de elementos. Los
 * elementos no guardan texto: guardan <strong>un tramo</strong> —{@link #getStartOffset} a
 * {@link #getEndOffset}— y los atributos que valen ahi. Por eso editar el texto no reconstruye el
 * arbol: los tramos se corren.
 *
 * <p>Un mismo documento puede tener <em>varios</em> arboles a la vez sobre el mismo texto — uno de
 * parrafos y otro de lineas visuales, que no coinciden cuando hay ajuste de linea. De ahi que
 * {@link Document#getRootElements} devuelva un arreglo y no uno solo.
 */
public interface Element {

    /** El documento al que pertenece. */
    Document getDocument();

    /** El elemento que lo contiene, o {@code null} si es raiz. */
    Element getParentElement();

    /** El nombre del tipo de elemento. */
    String getName();

    /** Los atributos que valen en este tramo. */
    AttributeSet getAttributes();

    /** Donde empieza el tramo. */
    int getStartOffset();

    /** Donde termina el tramo. */
    int getEndOffset();

    /** Cual de los hijos cubre la posicion {@code offset}. */
    int getElementIndex(int offset);

    /** Cuantos hijos tiene. */
    int getElementCount();

    /** El hijo numero {@code index}. */
    Element getElement(int index);

    /**
     * Si no tiene hijos.
     *
     * <p>No es lo mismo que {@code getElementCount() == 0}: un elemento con hijos puede quedar
     * momentaneamente vacio durante una edicion sin dejar de ser una rama.
     */
    boolean isLeaf();
}
