package javax.swing.plaf;

import java.awt.Rectangle;

import javax.swing.JTabbedPane;

/**
 * El aspecto de un {@link JTabbedPane}.
 *
 * <h2>Tres preguntas sobre las solapas</h2>
 *
 * <p>Que solapa cae en un punto, donde esta una solapa, y en cuantas filas quedaron. El panel no
 * las puede contestar: el tamano de una solapa depende de la tipografia y del adorno que dibuje el
 * aspecto, y cuantas filas quedan depende de eso y del ancho.
 */
public abstract class TabbedPaneUI extends ComponentUI {

    protected TabbedPaneUI() {
    }

    /** Que solapa cae en ese punto, o -1. */
    public abstract int tabForCoordinate(JTabbedPane pane, int x, int y);

    /** El rectangulo de esa solapa. */
    public abstract Rectangle getTabBounds(JTabbedPane pane, int index);

    /** En cuantas filas quedaron las solapas. */
    public abstract int getTabRunCount(JTabbedPane pane);
}
