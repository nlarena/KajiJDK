package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PopupMenuUI;

/**
 * Un menu que aparece flotando: el contextual o el que despliega un {@link JMenu}.
 *
 * <h2>No es un contenedor mas</h2>
 *
 * <p>Un menu desplegable no vive donde esta puesto: aparece encima de todo y puede salirse de la
 * ventana. Por eso {@link #setVisible} no hace lo que en otro componente -- pide una
 * {@link Popup} al aspecto y la muestra --, y por eso tiene un <em>invocador</em>: el componente
 * desde el que se abrio, que es contra quien se miden las coordenadas.
 *
 * <h2>Es un elemento de menu</h2>
 *
 * <p>Implementa {@link MenuElement}, asi que participa del recorrido con el teclado y el mouse
 * junto con sus opciones. Es lo que hace que moverse con las flechas entre un menu y su submenu
 * funcione sin que ninguno de los dos sepa del otro.
 *
 * <h2>Liviano o pesado</h2>
 *
 * <p>{@link #setLightWeightPopupEnabled} pide dibujarlo adentro de la ventana. Es un pedido, no una
 * orden: si no entra, se usa una ventana propia igual. Ver la nota de {@link PopupFactory}.
 */
public class JPopupMenu extends JComponent implements Accessible, MenuElement {

    private static final String uiClassID = "PopupMenuUI";

    private static boolean defaultLWPopupEnabled = true;

    private SingleSelectionModel selectionModel;
    private Component invoker;
    private Popup popup;
    private String label = null;
    private boolean paintBorder = true;
    private Insets margin = null;
    private boolean lightWeightPopup = true;
    private int desiredLocationX;
    private int desiredLocationY;
    private boolean visible = false;
    private Dimension popupSize;
    private AccessibleContext accessibleContext;

    /** Si los desplegables nuevos arrancan pidiendo la forma liviana. */
    public static void setDefaultLightWeightPopupEnabled(boolean aFlag) {
        defaultLWPopupEnabled = aFlag;
    }

    public static boolean getDefaultLightWeightPopupEnabled() {
        return defaultLWPopupEnabled;
    }

    /** Un menu desplegable vacio. */
    public JPopupMenu() {
        this(null);
    }

    /** Un menu desplegable con ese titulo. */
    public JPopupMenu(String label) {
        this.label = label;
        lightWeightPopup = getDefaultLightWeightPopupEnabled();
        setSelectionModel(new DefaultSingleSelectionModel());
        setFocusTraversalKeysEnabled(false);
        updateUI();
    }

    public PopupMenuUI getUI() {
        return (PopupMenuUI) ui;
    }

    public void setUI(PopupMenuUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    protected void processFocusEvent(FocusEvent evt) {
        super.processFocusEvent(evt);
    }

    /**
     * Atiende las teclas del menu.
     *
     * <p>Con el menu abierto, las flechas y el Escape no van al componente que tenga el foco sino
     * al recorrido del menu. Si no, abrir un menu sobre un campo de texto haria que las flechas
     * movieran el cursor en lugar de recorrer las opciones.
     */
    protected void processKeyEvent(KeyEvent evt) {
        MenuSelectionManager.defaultManager().processKeyEvent(evt);
        if (evt.isConsumed()) {
            return;
        }
        super.processKeyEvent(evt);
    }

    public SingleSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(SingleSelectionModel model) {
        selectionModel = model;
    }

    public JMenuItem add(JMenuItem menuItem) {
        super.add(menuItem);
        return menuItem;
    }

    /** Agrega una opcion con ese texto. */
    public JMenuItem add(String s) {
        return add(new JMenuItem(s));
    }

    /** Agrega una opcion que dispara esa accion. */
    public JMenuItem add(Action a) {
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        add(mi);
        return mi;
    }

    protected JMenuItem createActionComponent(Action a) {
        JMenuItem mi = new JMenuItem();
        mi.setHorizontalTextPosition(JButton.TRAILING);
        mi.setVerticalTextPosition(JButton.CENTER);
        return mi;
    }

    /**
     * El puente entre una accion y la opcion que la muestra.
     *
     * <p>Devuelve nulo: la opcion escucha a la accion ella misma desde
     * {@link JMenuItem#setAction}. El metodo queda porque es protegido y alguien puede
     * sobrescribirlo.
     */
    protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
        return null;
    }

    public void remove(int pos) {
        super.remove(pos);
    }

    /** Si se pide la forma liviana; ver la nota de la clase. */
    public void setLightWeightPopupEnabled(boolean aFlag) {
        lightWeightPopup = aFlag;
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopup;
    }

    /** El titulo que algunos aspectos dibujan arriba del menu. */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        String oldValue = this.label;
        this.label = label;
        firePropertyChange("label", oldValue, label);
        invalidate();
        repaint();
    }

    /** Agrega una raya separadora. */
    public void addSeparator() {
        add(new Separator());
    }

    /** Inserta una opcion que dispara esa accion, en esa posicion. */
    public void insert(Action a, int index) {
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        insert(mi, index);
    }

    public void insert(Component component, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        super.add(component, index);
    }

    public void addPopupMenuListener(PopupMenuListener l) {
        listenerList.add(PopupMenuListener.class, l);
    }

    public void removePopupMenuListener(PopupMenuListener l) {
        listenerList.remove(PopupMenuListener.class, l);
    }

    public PopupMenuListener[] getPopupMenuListeners() {
        return listenerList.getListeners(PopupMenuListener.class);
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

    /**
     * Avisa que el menu esta por abrirse.
     *
     * <p>Llega <em>antes</em> de mostrarlo, que es lo que permite armar las opciones segun donde se
     * hizo clic. Un menu contextual que se arma despues de aparecer se veria cambiar.
     */
    protected void firePopupMenuWillBecomeVisible() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuWillBecomeVisible(e);
            }
        }
    }

    protected void firePopupMenuWillBecomeInvisible() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuWillBecomeInvisible(e);
            }
        }
    }

    /** Avisa que el menu se cerro sin que se eligiera nada. */
    protected void firePopupMenuCanceled() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuCanceled(e);
            }
        }
    }

    /** Le da al menu el tamano que piden sus opciones. */
    public void pack() {
        if (popupSize == null) {
            setSize(getPreferredSize());
        } else {
            setSize(popupSize);
        }
    }

    /**
     * Muestra o esconde el menu.
     *
     * <p>Mostrarlo no es hacerlo visible: hay que pedirle una {@link Popup} al aspecto. Ver la nota
     * de la clase.
     */
    public void setVisible(boolean b) {
        if (b == isVisible()) {
            return;
        }
        if (b) {
            firePopupMenuWillBecomeVisible();
            PopupMenuUI ui = getUI();
            if (ui != null) {
                popup = ui.getPopup(this, desiredLocationX, desiredLocationY);
                pack();
                popup.show();
            }
            visible = true;
            firePropertyChange("visible", Boolean.FALSE, Boolean.TRUE);
        } else {
            firePopupMenuWillBecomeInvisible();
            if (popup != null) {
                popup.hide();
                popup = null;
            }
            visible = false;
            firePropertyChange("visible", Boolean.TRUE, Boolean.FALSE);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    /** Donde aparecera, en coordenadas de pantalla. */
    public void setLocation(int x, int y) {
        desiredLocationX = x;
        desiredLocationY = y;
        super.setLocation(x, y);
    }

    /** El componente desde el que se abrio; ver la nota de la clase. */
    public Component getInvoker() {
        return this.invoker;
    }

    public void setInvoker(Component invoker) {
        Component oldInvoker = this.invoker;
        this.invoker = invoker;
        firePropertyChange("invoker", oldInvoker, invoker);
        invalidate();
    }

    /**
     * Abre el menu en ese punto, relativo al invocador.
     *
     * <p>Las coordenadas son del invocador y no de la pantalla: un menu contextual se pide donde se
     * hizo clic, y ese punto viene en coordenadas del componente que recibio el clic.
     */
    public void show(Component invoker, int x, int y) {
        setInvoker(invoker);
        if (invoker != null) {
            java.awt.Point p = invoker.getLocationOnScreen();
            setLocation(p.x + x, p.y + y);
        } else {
            setLocation(x, y);
        }
        setVisible(true);
    }

    /**
     * El componente numero tal.
     *
     * @deprecated Usar {@link java.awt.Container#getComponent(int)}.
     */
    @Deprecated
    public Component getComponentAtIndex(int i) {
        return getComponent(i);
    }

    public int getComponentIndex(Component c) {
        int ncomponents = this.getComponentCount();
        Component[] component = this.getComponents();
        for (int i = 0; i < ncomponents; i++) {
            if (component[i] == c) {
                return i;
            }
        }
        return -1;
    }

    /** Fija el tamano del menu en lugar de dejar que lo pidan las opciones. */
    public void setPopupSize(Dimension d) {
        popupSize = d;
        if (popup != null) {
            pack();
        }
    }

    public void setPopupSize(int width, int height) {
        setPopupSize(new Dimension(width, height));
    }

    /** Marca ese componente como el elegido del recorrido. */
    public void setSelected(Component sel) {
        SingleSelectionModel model = getSelectionModel();
        int index = getComponentIndex(sel);
        model.setSelectedIndex(index);
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        paintBorder = b;
        repaint();
    }

    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    public Insets getMargin() {
        if (margin == null) {
            return new Insets(0, 0, 0, 0);
        }
        return margin;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    public void processMouseEvent(MouseEvent event, MenuElement[] path,
            MenuSelectionManager manager) {
    }

    public void processKeyEvent(KeyEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
    }

    /** El recorrido del menu paso por aca, o dejo de pasar. */
    public void menuSelectionChanged(boolean isIncluded) {
        if (invoker instanceof JMenu) {
            // Un submenu se abre y se cierra siguiendo al recorrido; no lo decide el mouse.
            setVisible(isIncluded);
        } else if (!isIncluded) {
            setVisible(false);
        }
    }

    /** Las opciones de adentro que participan del recorrido. */
    public MenuElement[] getSubElements() {
        java.util.Vector<MenuElement> tmp = new java.util.Vector<MenuElement>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof MenuElement) {
                tmp.addElement((MenuElement) c);
            }
        }
        MenuElement[] result = new MenuElement[tmp.size()];
        tmp.copyInto(result);
        return result;
    }

    public Component getComponent() {
        return this;
    }

    /** Si ese evento es el gesto que abre el menu; lo contesta el aspecto. */
    public boolean isPopupTrigger(MouseEvent e) {
        PopupMenuUI ui = getUI();
        return (ui != null) && ui.isPopupTrigger(e);
    }

    /**
     * La raya que separa grupos de opciones.
     *
     * <p>Es un {@link JSeparator} con otro nombre de aspecto: la raya de un menu se dibuja distinto
     * de una suelta -- con los margenes del menu -- y el nombre es lo que permite que el aspecto
     * las distinga.
     */
    public static class Separator extends JSeparator {

        public Separator() {
            super(JSeparator.HORIZONTAL);
        }

        public String getUIClassID() {
            return "PopupMenuSeparatorUI";
        }
    }
}
