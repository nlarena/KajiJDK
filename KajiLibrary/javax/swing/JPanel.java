package javax.swing;

import java.awt.FlowLayout;
import java.awt.LayoutManager;

/**
 * Un contenedor generico: el ladrillo con el que se arma cualquier disposicion en Swing.
 *
 * <p>No tiene comportamiento propio --no dibuja nada por si mismo, no responde a nada-- y esa es su
 * utilidad: un lugar donde poner otros componentes con una disposicion. Casi cualquier pantalla de
 * Swing es un arbol de paneles.
 *
 * <p>Su disposicion por omision es {@link FlowLayout}, no `null`: un panel recien creado ya sabe
 * acomodar lo que le pongan.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Estan los cuatro constructores y el identificador de UI. Lo que falta cuelga del
 * `LookAndFeel` --`getUI`, `setUI`, `updateUI`-- y de la accesibilidad, que esta biblioteca no
 * tiene.
 *
 * <p>El parametro `isDoubleBuffered` se guarda pero no se aplica: el doble bufer es una decision
 * del pintado, y aca no hay pintado.
 */
public class JPanel extends JComponent {


    /** Si se pediria doble bufer al pintar. Ver la nota de la clase. */
    private boolean doubleBuffered;

    /**
     * Un panel con esa disposicion y ese modo de bufer.
     *
     * @param layout la disposicion, o `null` para ninguna
     * @param isDoubleBuffered si se pediria doble bufer
     */
    public JPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super();
        this.doubleBuffered = isDoubleBuffered;
        setLayout(layout);
    }

    /**
     * Un panel con {@link FlowLayout} y ese modo de bufer.
     *
     * @param isDoubleBuffered si se pediria doble bufer
     */
    public JPanel(boolean isDoubleBuffered) {
        this(new FlowLayout(), isDoubleBuffered);
    }

    /**
     * Un panel con esa disposicion, con doble bufer.
     *
     * @param layout la disposicion, o `null` para ninguna
     */
    public JPanel(LayoutManager layout) {
        this(layout, true);
    }

    /** Un panel con {@link FlowLayout} y doble bufer. */
    public JPanel() {
        this(new FlowLayout(), true);
    }

    /** La clave con la que el `LookAndFeel` busca el aspecto de un panel: {@code "PanelUI"}. */
    public String getUIClassID() {
        return "PanelUI";
    }

    /** Si se pediria doble bufer al pintar. Ver la nota de la clase. */
    public boolean isDoubleBuffered() {
        return this.doubleBuffered;
    }
}
