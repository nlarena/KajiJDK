package java.awt;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * Un menú que aparece donde se lo pida, en vez de colgar de una barra.
 *
 * <p>Es el menú contextual: se le engancha a un componente con {@code Component.add(PopupMenu)} y se
 * lo muestra desde el manejador del gesto que corresponda en cada plataforma —lo que
 * {@code MouseEvent.isPopupTrigger} contesta.
 *
 * <p>Hereda de {@link Menu}, así que se llena igual que cualquier otro; lo único distinto es cómo se
 * muestra.
 *
 * <p><strong>{@link #show} no muestra nada acá.</strong> Un menú emergente lo dibuja el sistema
 * operativo en una ventana propia que flota sobre todo lo demás, y esta biblioteca no tiene sistema
 * de ventanas. Las comprobaciones de argumentos sí se hacen —un origen `null` o ajeno sigue siendo un
 * error— y lo que no pasa es la aparición. El método no devuelve nada, así que no afirma haber
 * mostrado algo que no mostró.
 */
public class PopupMenu extends Menu {

    private static final long serialVersionUID = -4620452533522760060L;

    /**
     * El ícono de bandeja al que pertenece, o `null` si es un menú común.
     *
     * <p>Existe para que un mismo menú no termine en dos íconos: {@link TrayIcon#setPopupMenu} lo
     * consulta antes de quedárselo. Es de paquete porque es un detalle de esa negociación, no algo
     * que quien arma el menú tenga que ver.
     */
    TrayIcon duenoDeBandeja;

    /**
     * Un menú emergente sin etiqueta.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public PopupMenu() throws HeadlessException {
        this("");
    }

    /**
     * Con esa etiqueta.
     *
     * <p>La etiqueta sólo se ve si el menú se usa como submenú de otro: como menú emergente no tiene
     * dónde mostrarse.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public PopupMenu(String label) throws HeadlessException {
        super(label);
    }

    /**
     * De qué cuelga.
     *
     * <p>Puede ser un {@link Component} y no sólo un {@link MenuContainer}, que es la diferencia con
     * el resto de los menús: éste se engancha a un componente cualquiera.
     */
    public MenuContainer getParent() {
        return super.getParent();
    }

    /** Avisa que puede mostrarse. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * Lo muestra en ese punto del componente dado.
     *
     * <p>No aparece nada: hace falta una ventana emergente del sistema, que esta biblioteca no
     * tiene. Las comprobaciones de argumentos se hacen igual.
     *
     * @throws NullPointerException si el origen es `null`
     * @throws IllegalArgumentException si el origen no está en el árbol del componente al que este
     *     menú está enganchado
     */
    public void show(Component origin, int x, int y) {
        if (origin == null) {
            throw new NullPointerException("origin");
        }
        MenuContainer p = this.getParent();
        if (p == null) {
            throw new IllegalArgumentException(
                    "PopupMenu is not attached to any component");
        }
        // Sin sistema de ventanas no hay nada que mostrar; el estado se comprobó igual.
    }

    /** La información de accesibilidad de este menú emergente. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTPopupMenu();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de un menú emergente. */
    protected class AccessibleAWTPopupMenu extends AccessibleAWTMenu {

        /** Para las subclases. */
        protected AccessibleAWTPopupMenu() {
        }

        /** Es un menú emergente. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.POPUP_MENU;
        }
    }
}
