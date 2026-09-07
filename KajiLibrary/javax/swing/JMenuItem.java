package javax.swing;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.plaf.MenuItemUI;

/**
 * Una opcion de menu.
 *
 * <h2>Es un boton, y eso resuelve casi todo</h2>
 *
 * <p>Hereda de {@link AbstractButton}, asi que ya tiene texto, icono, accion, habilitado y el
 * evento al apretarla. Lo que agrega es lo que un boton suelto no necesita: el atajo de teclado y
 * el manejo del recorrido del menu.
 *
 * <h2>Armada no es apretada</h2>
 *
 * <p>{@link #setArmed} marca la opcion sobre la que esta el mouse mientras el menu esta abierto. Es
 * distinto de apretada: apretada dura lo que dura el clic, armada dura mientras el mouse pasa por
 * encima. Un menu se recorre sin soltar el boton, y esa es la diferencia que lo permite.
 *
 * <h2>Los eventos vienen con el camino</h2>
 *
 * <p>{@link #processMouseEvent(MouseEvent, MenuElement[], MenuSelectionManager)} recibe ademas el
 * camino de elementos desde el menu de arriba. Una opcion no puede decidir sola: si el mouse se
 * mueve a un submenu, quien cierra el anterior es quien conoce el camino entero.
 */
public class JMenuItem extends AbstractButton implements Accessible, MenuElement {

    private static final String uiClassID = "MenuItemUI";

    private KeyStroke accelerator;
    private boolean isMouseDragged = false;
    private AccessibleContext accessibleContext;

    /** Una opcion vacia. */
    public JMenuItem() {
        this(null, (Icon) null);
    }

    /** Una opcion con solo un icono. */
    public JMenuItem(Icon icon) {
        this(null, icon);
    }

    /** Una opcion con ese texto. */
    public JMenuItem(String text) {
        this(text, (Icon) null);
    }

    /** Una opcion que dispara esa accion y toma de ella su texto y su icono. */
    public JMenuItem(Action a) {
        this();
        setAction(a);
    }

    /** Una opcion con texto e icono. */
    public JMenuItem(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
        // Por la via del aspecto, no por la del usuario: asi el UI que se instale despues puede
        // volver a prenderlo. Con `setBorderPainted(false)` quedaria marcado como decision del
        // programa y ningun aspecto lo tocaria nunca mas.
        javax.swing.LookAndFeel.installProperty(this, "borderPainted", Boolean.FALSE);
        setFocusPainted(false);
        setHorizontalTextPosition(JButton.TRAILING);
        setHorizontalAlignment(JButton.LEADING);
        updateUI();
    }

    /** Una opcion con texto y esa letra subrayada. */
    public JMenuItem(String text, int mnemonic) {
        setModel(new DefaultButtonModel());
        init(text, null);
        setMnemonic(mnemonic);
        // Por la via del aspecto, no por la del usuario: asi el UI que se instale despues puede
        // volver a prenderlo. Con `setBorderPainted(false)` quedaria marcado como decision del
        // programa y ningun aspecto lo tocaria nunca mas.
        javax.swing.LookAndFeel.installProperty(this, "borderPainted", Boolean.FALSE);
        setFocusPainted(false);
        setHorizontalTextPosition(JButton.TRAILING);
        setHorizontalAlignment(JButton.LEADING);
        updateUI();
    }

    public void setModel(ButtonModel newModel) {
        super.setModel(newModel);
    }

    protected void init(String text, Icon icon) {
        if (text != null) {
            setText(text);
        }
        if (icon != null) {
            setIcon(icon);
        }
    }

    public void setUI(MenuItemUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Marca la opcion sobre la que esta el mouse; ver la nota de la clase. */
    public void setArmed(boolean b) {
        ButtonModel model = getModel();
        if (model.isArmed() != b) {
            model.setArmed(b);
        }
    }

    public boolean isArmed() {
        ButtonModel model = getModel();
        return (model != null) && model.isArmed();
    }

    /**
     * Habilita o deshabilita la opcion.
     *
     * <p>Deshabilitarla la desarma: una opcion apagada no puede quedar resaltada, porque al volver
     * a habilitarla apareceria marcada sin que el mouse este encima.
     */
    public void setEnabled(boolean b) {
        if (!b) {
            setArmed(false);
        }
        super.setEnabled(b);
    }

    /**
     * El atajo que dispara la opcion sin abrir el menu.
     *
     * <p>Es del menu, no del componente: funciona con la ventana en foco aunque el menu este
     * cerrado. Por eso se dibuja al lado del texto, para que se pueda aprender.
     */
    public void setAccelerator(KeyStroke keyStroke) {
        KeyStroke oldAccelerator = accelerator;
        this.accelerator = keyStroke;
        firePropertyChange("accelerator", oldAccelerator, accelerator);
        repaint();
        revalidate();
    }

    public KeyStroke getAccelerator() {
        return this.accelerator;
    }

    protected void configurePropertiesFromAction(Action a) {
        super.configurePropertiesFromAction(a);
        if (a != null) {
            Object o = a.getValue(Action.ACCELERATOR_KEY);
            if (o instanceof KeyStroke) {
                setAccelerator((KeyStroke) o);
            }
        }
    }

    protected void actionPropertyChanged(Action action, String propertyName) {
        if (Action.ACCELERATOR_KEY.equals(propertyName)) {
            Object o = action.getValue(Action.ACCELERATOR_KEY);
            setAccelerator(o instanceof KeyStroke ? (KeyStroke) o : null);
        } else {
            super.actionPropertyChanged(action, propertyName);
        }
    }

    /** Un evento de mouse con el camino del menu; ver la nota de la clase. */
    public void processMouseEvent(MouseEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
        processMenuDragMouseEvent(new MenuDragMouseEvent(e.getComponent(), e.getID(),
                e.getWhen(), e.getModifiersEx(), e.getX(), e.getY(), e.getClickCount(),
                e.isPopupTrigger(), path, manager));
    }

    public void processKeyEvent(KeyEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
        MenuKeyEvent mke = new MenuKeyEvent(e.getComponent(), e.getID(), e.getWhen(),
                e.getModifiersEx(), e.getKeyCode(), e.getKeyChar(), path, manager);
        processMenuKeyEvent(mke);
        if (mke.isConsumed()) {
            e.consume();
        }
    }

    /** Reparte el evento de arrastre a quien corresponda. */
    public void processMenuDragMouseEvent(MenuDragMouseEvent e) {
        int id = e.getID();
        if (id == MouseEvent.MOUSE_ENTERED) {
            isMouseDragged = false;
            fireMenuDragMouseEntered(e);
        } else if (id == MouseEvent.MOUSE_EXITED) {
            isMouseDragged = false;
            fireMenuDragMouseExited(e);
        } else if (id == MouseEvent.MOUSE_DRAGGED) {
            isMouseDragged = true;
            fireMenuDragMouseDragged(e);
        } else if (id == MouseEvent.MOUSE_RELEASED) {
            if (isMouseDragged) {
                fireMenuDragMouseReleased(e);
            }
        }
    }

    public void processMenuKeyEvent(MenuKeyEvent e) {
        int id = e.getID();
        if (id == KeyEvent.KEY_PRESSED) {
            fireMenuKeyPressed(e);
        } else if (id == KeyEvent.KEY_RELEASED) {
            fireMenuKeyReleased(e);
        } else if (id == KeyEvent.KEY_TYPED) {
            fireMenuKeyTyped(e);
        }
    }

    protected void fireMenuDragMouseEntered(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseEntered(event);
            }
        }
    }

    protected void fireMenuDragMouseExited(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseExited(event);
            }
        }
    }

    protected void fireMenuDragMouseDragged(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseDragged(event);
            }
        }
    }

    protected void fireMenuDragMouseReleased(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseReleased(event);
            }
        }
    }

    protected void fireMenuKeyPressed(MenuKeyEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuKeyListener.class) {
                ((MenuKeyListener) listeners[i + 1]).menuKeyPressed(event);
            }
        }
    }

    protected void fireMenuKeyReleased(MenuKeyEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuKeyListener.class) {
                ((MenuKeyListener) listeners[i + 1]).menuKeyReleased(event);
            }
        }
    }

    protected void fireMenuKeyTyped(MenuKeyEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuKeyListener.class) {
                ((MenuKeyListener) listeners[i + 1]).menuKeyTyped(event);
            }
        }
    }

    /** El recorrido del menu paso por aca, o dejo de pasar. */
    public void menuSelectionChanged(boolean isIncluded) {
        setArmed(isIncluded);
    }

    /**
     * Los elementos de adentro; ninguno.
     *
     * <p>Una opcion es una hoja. {@link JMenu}, que si tiene submenu, lo sobrescribe.
     */
    public MenuElement[] getSubElements() {
        return new MenuElement[0];
    }

    public Component getComponent() {
        return this;
    }

    public void addMenuDragMouseListener(MenuDragMouseListener l) {
        listenerList.add(MenuDragMouseListener.class, l);
    }

    public void removeMenuDragMouseListener(MenuDragMouseListener l) {
        listenerList.remove(MenuDragMouseListener.class, l);
    }

    public MenuDragMouseListener[] getMenuDragMouseListeners() {
        return listenerList.getListeners(MenuDragMouseListener.class);
    }

    public void addMenuKeyListener(MenuKeyListener l) {
        listenerList.add(MenuKeyListener.class, l);
    }

    public void removeMenuKeyListener(MenuKeyListener l) {
        listenerList.remove(MenuKeyListener.class, l);
    }

    public MenuKeyListener[] getMenuKeyListeners() {
        return listenerList.getListeners(MenuKeyListener.class);
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
