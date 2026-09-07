package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SeparatorUI;

/**
 * Una raya que separa grupos de cosas.
 *
 * <h2>Un componente para una raya</h2>
 *
 * <p>Podria ser un borde o dos lineas dibujadas a mano. Es un componente porque asi lo acomoda el
 * acomodador junto con lo que separa: en una barra de herramientas que se reordena, la raya se
 * mueve con los botones sin que nadie recalcule nada.
 *
 * <p>Ademas el aspecto la dibuja como corresponda al sistema -- una linea, dos, un hueco -- sin que
 * quien la puso tenga que saber cual.
 */
public class JSeparator extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "SeparatorUI";

    private int orientation = HORIZONTAL;
    private AccessibleContext accessibleContext;

    /** Una raya horizontal. */
    public JSeparator() {
        this(HORIZONTAL);
    }

    /** Una raya con esa orientacion. */
    public JSeparator(int orientation) {
        checkOrientation(orientation);
        this.orientation = orientation;
        setFocusable(false);
        updateUI();
    }

    public SeparatorUI getUI() {
        return (SeparatorUI) ui;
    }

    public void setUI(SeparatorUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public int getOrientation() {
        return this.orientation;
    }

    /**
     * Si la raya va acostada o parada.
     *
     * @throws IllegalArgumentException si no es una de las dos.
     */
    public void setOrientation(int orientation) {
        if (this.orientation == orientation) {
            return;
        }
        int oldValue = this.orientation;
        checkOrientation(orientation);
        this.orientation = orientation;
        firePropertyChange("orientation", oldValue, orientation);
        revalidate();
        repaint();
    }

    private static void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
