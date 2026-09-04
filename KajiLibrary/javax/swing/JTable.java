package javax.swing;

/**
 * <strong>Un lugar reservado, no una implementacion.</strong> Ya no por falta de rasterizador —lo
 * hay, y {@link JComponent} es real y pinta— sino por tamano: una tabla son doscientos miembros que
 * se sostienen entre si, y todavia no estan.
 *
 * <p><strong>Existe por un motivo concreto</strong>: {@code javax.swing.table.TableCellRenderer} y
 * {@code TableCellEditor} nombran un {@code JTable} en sus firmas, y de esas dos interfaces cuelga
 * {@code TableColumn}, y de ahi {@code TableColumnModel}, que es lo que
 * {@code javax.swing.event.TableColumnModelEvent} necesita. Sin esta clase, doce clases de
 * {@code javax.swing.event} no se pueden declarar — y ese paquete si es escribible entero, porque
 * son eventos y oyentes, no pixeles.
 *
 * <p><strong>Que falta, dicho de frente:</strong> sus doscientos y pico miembros. El modelo de datos,
 * el de columnas, el de seleccion, la edicion de celdas, el ordenamiento, el encabezado y todo el
 * dibujado. Lo unico cierto aca es su lugar en la jerarquia.
 *
 * <p>Se eligio la clase vacia y anunciada antes que una seleccion arbitraria de treinta metodos que
 * pareciera una tabla sin serlo. El criterio es el de la casa: un miembro que falta es un subconjunto
 * legal, uno que miente no lo es, y una clase a medias invita a confundir las dos cosas. Una tabla
 * con {@code getValueAt} pero sin edicion ni seleccion es justamente esa confusion. Cuando se
 * escriba, sera sobre el {@link JComponent} real: hereda su tuberia de pintado, su borde y su
 * aspecto, asi que lo que falta es la tabla, no el suelo.
 */
public class JTable extends JComponent {

    private static final long serialVersionUID = -3876246055581701574L;

    /** Para las subclases. */
    public JTable() {
    }
}
