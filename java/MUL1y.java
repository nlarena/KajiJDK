import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * La interfaz grafica del aspecto auxiliar.
 *
 * <p>Es otra clase --y no otra instancia de {@code MUL1x}-- porque {@code UIDefaults} las busca por
 * nombre: dos entradas con el mismo nombre de clase darian dos instancias de la misma, y lo que se
 * quiere comprobar es que el reparto llega a las dos.
 */
public class MUL1y {
    /** La instancia del aspecto auxiliar. */
    public static ComponentUI createUI(JComponent c) {
        return new MUL1x(false, 20);
    }
}
