package javax.swing;

/**
 * <strong>Un lugar reservado, no una implementación.</strong> Esta biblioteca no trae Swing.
 *
 * <p>En Swing de verdad, `JMenuBar` es la barra de menús de una ventana: guarda los `JMenu`, lleva
 * cuál está abierto, y participa de la navegación por teclado a través de `MenuElement`.
 *
 * <p><strong>Existe por un solo motivo</strong>: es el tipo del parámetro de
 * {@code java.awt.Desktop.setDefaultMenuBar}, el único método de todo `java.awt` cuya firma nombra
 * un tipo de Swing. Ese método pone la barra de menús del programa en la del sistema —la franja de
 * arriba de macOS— y en esta biblioteca tira {@code UnsupportedOperationException} como todo lo
 * demás de {@code Desktop}, porque no hay escritorio. O sea que este tipo se nombra y nunca se usa.
 *
 * <p><strong>Qué falta, dicho de frente:</strong> todos sus miembros, y las dos interfaces que
 * implementa de verdad —{@code javax.accessibility.Accessible} y {@link MenuElement}—. La segunda se
 * dejó afuera a propósito: implementarla obliga a traer `MenuSelectionManager`, que a su vez trae
 * `EventListenerList` y los eventos de cambio, o sea a empezar Swing por la puerta de atrás. La
 * primera se podría haber puesto casi gratis, pero mezclar media interfaz de verdad con una clase
 * vacía es peor que ser parejo: esto es un nombre con la jerarquía correcta, y se anuncia como tal.
 *
 * @see JComponent
 */
public class JMenuBar extends JComponent {

    private static final long serialVersionUID = -8191026883931977036L;

    /** Una barra de menús. */
    public JMenuBar() {
    }
}
