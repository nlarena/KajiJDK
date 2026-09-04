package javax.swing;

import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Un boton que se aprieta: texto, icono, o los dos, y un {@code ActionEvent} al soltar.
 *
 * <p>Casi todo esta en {@link AbstractButton}: el modelo, los iconos por estado, las alineaciones,
 * la {@link Action}. Lo que agrega esta clase es el concepto de <em>boton por omision</em> —el que
 * dispara Enter en un dialogo—, y ese concepto vive en {@code JRootPane}, que no esta. De ahi que
 * {@link #isDefaultButton} sea siempre {@code false}: no hay panel raiz que lo haya nombrado.
 * {@link #isDefaultCapable} si funciona, porque es una propiedad del boton.
 *
 * <p>Sin {@code UIManager}, {@link #updateUI} instala el aspecto basico directamente; ver
 * {@link BasicButtonUI} para los valores por omision y de donde salen.
 */
public class JButton extends AbstractButton implements Accessible {

    private static final String uiClassID = "ButtonUI";

    public JButton() {
        this(null, null);
    }

    public JButton(Icon icon) {
        this(null, icon);
    }

    public JButton(String text) {
        this(text, null);
    }

    /** Un boton que toma texto, icono, mnemonico y estado de esa accion, y la dispara. */
    public JButton(Action a) {
        this();
        setAction(a);
    }

    public JButton(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
    }

    /** Instala el aspecto basico; ver la nota de la clase. */
    public void updateUI() {
        setUI((ButtonUI) BasicButtonUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Si es el boton por omision de su panel raiz: nunca, porque no hay panel raiz. */
    public boolean isDefaultButton() {
        return false;
    }

    /** Si puede ser el boton por omision de un dialogo; {@code true} salvo que se le quite. */
    public boolean isDefaultCapable() {
        return defaultCapable;
    }

    public void setDefaultCapable(boolean defaultCapable) {
        boolean viejo = this.defaultCapable;
        this.defaultCapable = defaultCapable;
        firePropertyChange("defaultCapable", viejo, defaultCapable);
    }

    /**
     * Se va de la jerarquia.
     *
     * <p>El JDK aprovecha para dejar de ser el boton por omision de su panel raiz; sin panel raiz,
     * queda lo de {@link AbstractButton#removeNotify}.
     */
    public void removeNotify() {
        super.removeNotify();
    }

    protected String paramString() {
        String defaultCapableString = defaultCapable ? "true" : "false";
        return super.paramString() + ",defaultCapable=" + defaultCapableString;
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
