package java.awt;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * El contenedor más simple que hay: un rectángulo donde agrupar otros componentes.
 *
 * <p>No dibuja nada, no tiene borde ni título, y su única diferencia con un {@link Container} pelado
 * es que trae una {@link FlowLayout} puesta y que es concreto. Eso alcanza: la mayoría de los
 * armados de interfaz en AWT son paneles anidados, cada uno con su distribución.
 */
public class Panel extends Container implements Accessible {

    private static final long serialVersionUID = -2728009084054400034L;

    /** Un panel con {@link FlowLayout}. */
    public Panel() {
        this(new FlowLayout());
    }

    /** Un panel con esa distribución. */
    public Panel(LayoutManager layout) {
        this.setLayout(layout);
    }

    String constructComponentName() {
        synchronized (Panel.class) {
            String n = "panel" + panelCounter;
            panelCounter = panelCounter + 1;
            return n;
        }
    }

    private static int panelCounter = 0;

    /** Lo declara mostrable; sin pantalla no hay nada más que hacer. */
    public void addNotify() {
        super.addNotify();
    }

    /** La accesibilidad del panel. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTPanel();
        }
        return this.accessibleContext;
    }

    /** Un panel, para la accesibilidad, es un panel: agrupa y nada más. */
    protected class AccessibleAWTPanel extends AccessibleAWTContainer {

        /** Para las subclases. */
        protected AccessibleAWTPanel() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PANEL;
        }
    }
}
