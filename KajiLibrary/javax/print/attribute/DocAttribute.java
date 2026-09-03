package javax.print.attribute;

/**
 * KajiLibrary's javax.print.attribute.DocAttribute -- un atributo que aplica a **un documento**.
 *
 * <h2>La familia de las cuatro marcadoras, explicada una sola vez</h2>
 *
 * <p>{@code DocAttribute}, {@link PrintRequestAttribute}, {@link PrintJobAttribute} y
 * {@link PrintServiceAttribute} no declaran ni un miembro. Lo unico que hacen es decir **a que
 * alcance pertenece** un atributo, y ese alcance se vuelve una restriccion de tipo real en los
 * conjuntos: {@link DocAttributeSet} solo acepta {@code DocAttribute}, {@link PrintJobAttributeSet}
 * solo {@code PrintJobAttribute}, y asi. Un atributo mal puesto no compila --o revienta con
 * {@code ClassCastException} si se lo mete por la interfaz cruda.
 *
 * <p>Un mismo atributo suele pertenecer a varias. {@code Sides} es las tres primeras: se puede pedir
 * por documento, por pedido y se puede reportar en el trabajo ya armado. Lo que **no** existe es un
 * atributo que sea a la vez de pedido y de servicio: uno lo elige quien imprime, el otro lo informa
 * la impresora.
 *
 * <p>Este es el alcance mas chico: vale para **una** pieza a imprimir. En un trabajo de varios
 * documentos cada uno puede traer los suyos --uno en A4 y otro en oficio dentro del mismo trabajo--,
 * que es exactamente lo que un atributo de pedido no puede expresar.
 */
public interface DocAttribute extends Attribute {
}
