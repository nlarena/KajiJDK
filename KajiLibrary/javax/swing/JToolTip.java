package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ToolTipUI;

/**
 * El cartelito de ayuda que aparece al dejar el puntero quieto.
 *
 * <h2>Casi nadie lo construye</h2>
 *
 * <p>Lo normal es {@code componente.setToolTipText("...")} y listo: el
 * {@code ToolTipManager} arma el cartel, lo muestra y lo esconde. Esta clase existe para el caso en
 * que haga falta otra cosa -- un cartel con varias lineas, con un icono, con un borde propio --, y
 * entonces se la extiende y se devuelve la subclase desde {@code createToolTip()}.
 *
 * <h2>Guarda a quien describe</h2>
 *
 * <p>{@link #setComponent} le dice de que componente es este cartel. No es adorno: el aspecto lo usa
 * para tomarle prestada la tipografia y los colores, de manera que el cartel se parezca a lo que
 * describe.
 */
public class JToolTip extends JComponent implements Accessible {

    private static final String uiClassID = "ToolTipUI";

    String tipText;
    JComponent component;

    /** Un cartel vacio. */
    public JToolTip() {
        setOpaque(true);
        updateUI();
    }

    public ToolTipUI getUI() {
        return (ToolTipUI) ui;
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** El texto; cambiarlo lo vuelve a medir, porque el cartel se ajusta a lo que dice. */
    public void setTipText(String tipText) {
        String oldValue = this.tipText;
        this.tipText = tipText;
        firePropertyChange("tiptext", oldValue, tipText);
        if (tipText == null ? oldValue != null : !tipText.equals(oldValue)) {
            revalidate();
            repaint();
        }
    }

    public String getTipText() {
        return tipText;
    }

    /** De que componente es este cartel; ver la nota de la clase. */
    public void setComponent(JComponent c) {
        JComponent oldValue = this.component;
        component = c;
        firePropertyChange("component", oldValue, c);
    }

    public JComponent getComponent() {
        return component;
    }

    /** Siempre cierto: un cartel de ayuda tapado por lo que describe no serviria de nada. */
    boolean alwaysOnTop() {
        return true;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
