package javax.swing.plaf;

import java.awt.Graphics;

import javax.swing.JSplitPane;

/**
 * El aspecto de un {@link JSplitPane}.
 *
 * <h2>La division es del aspecto</h2>
 *
 * <p>Los seis metodos son sobre la division: donde esta, hasta donde puede ir, y como se dibuja. El
 * panel no lo sabe porque la division es un componente que arma el aspecto -- con o sin flechitas,
 * de un ancho o de otro --, y sus limites dependen de los tamanos minimos de los dos lados.
 */
public abstract class SplitPaneUI extends ComponentUI {

    protected SplitPaneUI() {
    }

    /** Pone la division donde los dos lados tengan su tamano preferido. */
    public abstract void resetToPreferredSizes(JSplitPane jc);

    public abstract void setDividerLocation(JSplitPane jc, int location);

    public abstract int getDividerLocation(JSplitPane jc);

    /** Lo mas a la izquierda que la division puede ir. */
    public abstract int getMinimumDividerLocation(JSplitPane jc);

    public abstract int getMaximumDividerLocation(JSplitPane jc);

    /**
     * Se llama despues de dibujar los hijos.
     *
     * <p>Es el gancho para dibujar encima de ellos: la sombra de la division al arrastrarla se
     * tiene que ver sobre los dos lados, no debajo.
     */
    public abstract void finishedPaintingChildren(JSplitPane jc, Graphics g);
}
