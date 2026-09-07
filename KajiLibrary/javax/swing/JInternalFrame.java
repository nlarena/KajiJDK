package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopIconUI;
import javax.swing.plaf.InternalFrameUI;

/**
 * Una ventana dentro de otra.
 *
 * <h2>No es una ventana del sistema</h2>
 *
 * <p>Es un {@link JComponent} que se dibuja con marco, titulo y botones. Vive adentro de un
 * {@link JDesktopPane}; el sistema operativo no sabe que existe. De ahi que
 * {@link #getWarningString} devuelva nulo -- no hay ventana real que marcar -- y que se pueda tener
 * cien de estas sin gastar cien ventanas del sistema.
 *
 * <h2>Los cambios de estado se pueden vetar</h2>
 *
 * <p>{@link #setClosed}, {@link #setIcon}, {@link #setMaximum} y {@link #setSelected} lanzan
 * {@link PropertyVetoException}. Antes de cambiar avisan a los oyentes de veto, y cualquiera puede
 * negarse. Es el mecanismo con el que un editor impide que se cierre una ventana con cambios sin
 * guardar, sin tener que interceptar el boton de cerrar.
 *
 * <p>El estado se cambia <em>despues</em> de que paso el veto y <em>antes</em> de avisar el cambio,
 * asi el que escucha el cambio ya ve el valor nuevo.
 *
 * <h2>Quien mueve la ventana</h2>
 *
 * <p>Casi nadie llama a {@code setBounds} sobre una ventana interna: el aspecto le pide al
 * {@link DesktopManager} del escritorio, y ese decide. Ver la nota de {@link DesktopManager}.
 *
 * <h2>Los hijos van al contenido</h2>
 *
 * <p>Como en {@link JDialog}, adentro hay un {@link JRootPane} y {@code add} redirige a su panel de
 * contenido.
 */
public class JInternalFrame extends JComponent implements Accessible, WindowConstants,
        RootPaneContainer {

    private static final String uiClassID = "InternalFrameUI";

    /** El panel raiz; ver la nota de la clase. */
    protected JRootPane rootPane;

    /** Si agregar redirige al contenido. */
    protected boolean rootPaneCheckingEnabled = false;

    /** Si tiene boton de cerrar. */
    protected boolean closable;

    /** Si ya se cerro. */
    protected boolean isClosed;

    /** Si tiene boton de maximizar. */
    protected boolean maximizable;

    /** Si esta maximizada. */
    protected boolean isMaximum;

    /** Si tiene boton de minimizar. */
    protected boolean iconable;

    /** Si esta hecha icono. */
    protected boolean isIcon;

    /** Si se puede cambiar de tamano. */
    protected boolean resizable;

    /** Si es la ventana activa del escritorio. */
    protected boolean isSelected;

    /** El icono del titulo. */
    protected Icon frameIcon;

    /** El titulo. */
    protected String title;

    /** El icono que la reemplaza cuando esta minimizada. */
    protected JDesktopIcon desktopIcon;

    public static final String CONTENT_PANE_PROPERTY = "contentPane";
    public static final String MENU_BAR_PROPERTY = "JMenuBar";
    public static final String TITLE_PROPERTY = "title";
    public static final String LAYERED_PANE_PROPERTY = "layeredPane";
    public static final String ROOT_PANE_PROPERTY = "rootPane";
    public static final String GLASS_PANE_PROPERTY = "glassPane";
    public static final String FRAME_ICON_PROPERTY = "frameIcon";
    public static final String IS_SELECTED_PROPERTY = "selected";
    public static final String IS_CLOSED_PROPERTY = "closed";
    public static final String IS_MAXIMUM_PROPERTY = "maximum";
    public static final String IS_ICON_PROPERTY = "icon";

    private int defaultCloseOperation = DISPOSE_ON_CLOSE;
    private Rectangle normalBounds = null;
    private Component lastFocusOwner;
    private Cursor lastCursor;
    private boolean opened = false;

    /** Una ventana sin titulo, que no se cierra ni se agranda ni se achica ni se redimensiona. */
    public JInternalFrame() {
        this("", false, false, false, false);
    }

    /** Con ese titulo. */
    public JInternalFrame(String title) {
        this(title, false, false, false, false);
    }

    /** Con ese titulo, y redimensionable o no. */
    public JInternalFrame(String title, boolean resizable) {
        this(title, resizable, false, false, false);
    }

    public JInternalFrame(String title, boolean resizable, boolean closable) {
        this(title, resizable, closable, false, false);
    }

    public JInternalFrame(String title, boolean resizable, boolean closable,
            boolean maximizable) {
        this(title, resizable, closable, maximizable, false);
    }

    /** Con ese titulo y esas cuatro capacidades. */
    public JInternalFrame(String title, boolean resizable, boolean closable,
            boolean maximizable, boolean iconifiable) {
        setRootPane(createRootPane());
        setLayout(new java.awt.BorderLayout());
        this.title = title;
        this.resizable = resizable;
        this.closable = closable;
        this.maximizable = maximizable;
        this.iconable = iconifiable;
        isClosed = false;
        isSelected = false;
        isIcon = false;
        isMaximum = false;
        setVisible(false);
        setRootPaneCheckingEnabled(true);
        desktopIcon = new JDesktopIcon(this);
        updateUI();
    }

    protected JRootPane createRootPane() {
        return new JRootPane();
    }

    public InternalFrameUI getUI() {
        return (InternalFrameUI) ui;
    }

    public void setUI(InternalFrameUI ui) {
        boolean checkingEnabled = isRootPaneCheckingEnabled();
        try {
            setRootPaneCheckingEnabled(false);
            super.setUI(ui);
        } finally {
            setRootPaneCheckingEnabled(checkingEnabled);
        }
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * Agrega al contenido, no a la ventana.
     *
     * @throws Error si se intenta agregar el panel raiz con la redireccion prendida.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    /** Saca del contenido, salvo que sea el panel raiz. */
    public void remove(Component comp) {
        int oldCount = getComponentCount();
        super.remove(comp);
        if (oldCount == getComponentCount()) {
            getContentPane().remove(comp);
        }
    }

    /** Le pone acomodador al contenido, no a la ventana. */
    public void setLayout(LayoutManager manager) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(manager);
        } else {
            super.setLayout(manager);
        }
    }

    /**
     * @deprecated Usar {@link #getJMenuBar}.
     */
    @Deprecated
    public JMenuBar getMenuBar() {
        return getRootPane().getMenuBar();
    }

    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    /**
     * @deprecated Usar {@link #setJMenuBar}.
     */
    @Deprecated
    public void setMenuBar(JMenuBar m) {
        JMenuBar oldValue = getMenuBar();
        getRootPane().setJMenuBar(m);
        firePropertyChange(MENU_BAR_PROPERTY, oldValue, m);
    }

    public void setJMenuBar(JMenuBar m) {
        JMenuBar oldValue = getMenuBar();
        getRootPane().setJMenuBar(m);
        firePropertyChange(MENU_BAR_PROPERTY, oldValue, m);
    }

    public Container getContentPane() {
        return getRootPane().getContentPane();
    }

    /**
     * @throws java.awt.IllegalComponentStateException si es nulo.
     */
    public void setContentPane(Container c) {
        Container oldValue = getContentPane();
        getRootPane().setContentPane(c);
        firePropertyChange(CONTENT_PANE_PROPERTY, oldValue, c);
    }

    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    public void setLayeredPane(JLayeredPane layered) {
        JLayeredPane oldValue = getLayeredPane();
        getRootPane().setLayeredPane(layered);
        firePropertyChange(LAYERED_PANE_PROPERTY, oldValue, layered);
    }

    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    public void setGlassPane(Component glass) {
        Component oldValue = getGlassPane();
        getRootPane().setGlassPane(glass);
        firePropertyChange(GLASS_PANE_PROPERTY, oldValue, glass);
    }

    public JRootPane getRootPane() {
        return rootPane;
    }

    protected void setRootPane(JRootPane root) {
        if (rootPane != null) {
            remove(rootPane);
        }
        JRootPane oldValue = getRootPane();
        rootPane = root;
        if (rootPane != null) {
            boolean checkingEnabled = isRootPaneCheckingEnabled();
            try {
                // Apagado mientras se agrega el panel raiz: si no, se redirigiria a si mismo.
                setRootPaneCheckingEnabled(false);
                add(rootPane, java.awt.BorderLayout.CENTER);
            } finally {
                setRootPaneCheckingEnabled(checkingEnabled);
            }
        }
        firePropertyChange(ROOT_PANE_PROPERTY, oldValue, root);
    }

    public void setClosable(boolean b) {
        boolean oldValue = closable;
        closable = b;
        firePropertyChange("closable", oldValue, b);
    }

    public boolean isClosable() {
        return closable;
    }

    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Cierra o reabre la ventana.
     *
     * <p>Cerrar la esconde y la destruye. Reabrir una cerrada no la vuelve a mostrar sola: hay que
     * agregarla de nuevo al escritorio.
     *
     * @throws PropertyVetoException si algun oyente se opone.
     */
    public void setClosed(boolean b) throws PropertyVetoException {
        if (isClosed == b) {
            return;
        }
        Boolean oldValue = isClosed ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        if (b) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        }
        fireVetoableChange(IS_CLOSED_PROPERTY, oldValue, newValue);
        isClosed = b;
        if (isClosed) {
            setVisible(false);
        }
        firePropertyChange(IS_CLOSED_PROPERTY, oldValue, newValue);
        if (isClosed) {
            dispose();
        } else if (!opened) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
            opened = true;
        }
    }

    public void setResizable(boolean b) {
        boolean oldValue = resizable;
        resizable = b;
        firePropertyChange("resizable", oldValue, b);
    }

    public boolean isResizable() {
        // Una ventana maximizada ocupa todo: dejarla redimensionar no tendria a donde crecer.
        return isMaximum ? false : resizable;
    }

    public void setIconifiable(boolean b) {
        boolean oldValue = iconable;
        iconable = b;
        firePropertyChange("iconable", oldValue, b);
    }

    public boolean isIconifiable() {
        return iconable;
    }

    public boolean isIcon() {
        return isIcon;
    }

    /**
     * Convierte la ventana en icono, o la devuelve.
     *
     * @throws PropertyVetoException si algun oyente se opone.
     */
    public void setIcon(boolean b) throws PropertyVetoException {
        if (isIcon == b) {
            return;
        }
        Boolean oldValue = isIcon ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_ICON_PROPERTY, oldValue, newValue);
        isIcon = b;
        firePropertyChange(IS_ICON_PROPERTY, oldValue, newValue);
        if (b) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ICONIFIED);
        } else {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED);
        }
    }

    public void setMaximizable(boolean b) {
        boolean oldValue = maximizable;
        maximizable = b;
        firePropertyChange("maximizable", oldValue, b);
    }

    public boolean isMaximizable() {
        return maximizable;
    }

    public boolean isMaximum() {
        return isMaximum;
    }

    /**
     * Agranda la ventana a todo el escritorio, o la devuelve a su tamano.
     *
     * @throws PropertyVetoException si algun oyente se opone.
     */
    public void setMaximum(boolean b) throws PropertyVetoException {
        if (isMaximum == b) {
            return;
        }
        Boolean oldValue = isMaximum ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
        // El estado se cambia antes de avisar: el que escucha ya ve el valor nuevo.
        isMaximum = b;
        firePropertyChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange(TITLE_PROPERTY, oldValue, title);
    }

    /**
     * Activa o desactiva la ventana.
     *
     * <p>Para activarse tiene que estar visible en pantalla -- o su icono, si esta minimizada.
     * Activar una ventana que no se ve dejaria el escritorio con una ventana activa invisible y
     * ninguna de las visibles encendida. Desactivar, en cambio, se puede siempre.
     *
     * @throws PropertyVetoException si algun oyente se opone.
     */
    public void setSelected(boolean selected) throws PropertyVetoException {
        if (selected && isSelected) {
            // Ya esta activa, pero el foco puede estar afuera: se lo devuelve adentro.
            restoreSubcomponentFocus();
            return;
        }
        if (isSelected == selected) {
            return;
        }
        if (selected) {
            boolean seVe = isIcon ? desktopIcon.isShowing() : isShowing();
            if (!seVe) {
                return;
            }
        }
        Boolean oldValue = isSelected ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = selected ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_SELECTED_PROPERTY, oldValue, newValue);
        if (selected) {
            restoreSubcomponentFocus();
        }
        isSelected = selected;
        firePropertyChange(IS_SELECTED_PROPERTY, oldValue, newValue);
        if (isSelected) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ACTIVATED);
        } else {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED);
        }
        repaint();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setFrameIcon(Icon icon) {
        Icon oldIcon = frameIcon;
        frameIcon = icon;
        firePropertyChange(FRAME_ICON_PROPERTY, oldIcon, icon);
    }

    public Icon getFrameIcon() {
        return frameIcon;
    }

    /**
     * La pone adelante de las demas.
     *
     * <p>Si esta hecha icono mueve el icono, no la ventana: es lo que se ve.
     */
    public void moveToFront() {
        if (isIcon()) {
            if (getDesktopIcon().getParent() instanceof JLayeredPane) {
                JLayeredPane p = (JLayeredPane) getDesktopIcon().getParent();
                p.moveToFront(getDesktopIcon());
            }
        } else if (getParent() instanceof JLayeredPane) {
            JLayeredPane p = (JLayeredPane) getParent();
            p.moveToFront(this);
        }
    }

    /** La manda atras de las demas. */
    public void moveToBack() {
        if (isIcon()) {
            if (getDesktopIcon().getParent() instanceof JLayeredPane) {
                JLayeredPane p = (JLayeredPane) getDesktopIcon().getParent();
                p.moveToBack(getDesktopIcon());
            }
        } else if (getParent() instanceof JLayeredPane) {
            JLayeredPane p = (JLayeredPane) getParent();
            p.moveToBack(this);
        }
    }

    /**
     * El cursor que habia antes de que el aspecto lo cambiara para redimensionar.
     *
     * <p>Al pasar por un borde el aspecto pone una flecha doble; cuando se sale tiene que
     * devolver el de antes, y este es el que guarda cual era.
     */
    public Cursor getLastCursor() {
        return lastCursor;
    }

    public void setCursor(Cursor cursor) {
        lastCursor = cursor;
        super.setCursor(cursor);
    }

    /** La capa del escritorio en la que esta. */
    public void setLayer(Integer layer) {
        if (getParent() != null && getParent() instanceof JLayeredPane) {
            JLayeredPane p = (JLayeredPane) getParent();
            p.setLayer(this, layer.intValue(), p.getPosition(this));
        } else {
            JLayeredPane.putLayer(this, layer.intValue());
            if (getParent() != null) {
                getParent().repaint();
            }
        }
    }

    /** La capa, dada como entero. */
    public void setLayer(int layer) {
        this.setLayer(Integer.valueOf(layer));
    }

    public int getLayer() {
        return JLayeredPane.getLayer(this);
    }

    /**
     * El escritorio que la contiene.
     *
     * <p>Si esta hecha icono la ventana no tiene padre; entonces se busca desde el icono, que es
     * el que esta puesto en el escritorio.
     */
    public JDesktopPane getDesktopPane() {
        Container p = getParent();
        while (p != null && !(p instanceof JDesktopPane)) {
            p = p.getParent();
        }
        if (p == null) {
            p = getDesktopIcon().getParent();
            while (p != null && !(p instanceof JDesktopPane)) {
                p = p.getParent();
            }
        }
        return (JDesktopPane) p;
    }

    public void setDesktopIcon(JDesktopIcon d) {
        JDesktopIcon oldValue = getDesktopIcon();
        desktopIcon = d;
        firePropertyChange("desktopIcon", oldValue, d);
    }

    public JDesktopIcon getDesktopIcon() {
        return desktopIcon;
    }

    /**
     * El rectangulo que ocupaba antes de maximizarse.
     *
     * <p>Si no hay uno guardado devuelve el actual, que es el correcto mientras no este
     * maximizada.
     */
    public Rectangle getNormalBounds() {
        if (normalBounds != null) {
            return normalBounds;
        }
        return getBounds();
    }

    public void setNormalBounds(Rectangle r) {
        normalBounds = r;
    }

    /** Quien tiene el foco adentro, o nulo si la ventana no esta activa. */
    public Component getFocusOwner() {
        if (isSelected()) {
            return lastFocusOwner;
        }
        return null;
    }

    /** Quien tendria el foco si la ventana se activara. */
    public Component getMostRecentFocusOwner() {
        if (isSelected()) {
            return getFocusOwner();
        }
        if (lastFocusOwner != null) {
            return lastFocusOwner;
        }
        return getContentPane();
    }

    /** Le devuelve el foco al que lo tenia antes. */
    public void restoreSubcomponentFocus() {
        lastFocusOwner = getMostRecentFocusOwner();
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        }
    }

    /**
     * @deprecated Se llama sola; usar {@code setBounds}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
        validate();
        repaint();
    }

    public void addInternalFrameListener(InternalFrameListener l) {
        listenerList.add(InternalFrameListener.class, l);
    }

    public void removeInternalFrameListener(InternalFrameListener l) {
        listenerList.remove(InternalFrameListener.class, l);
    }

    public InternalFrameListener[] getInternalFrameListeners() {
        return listenerList.getListeners(InternalFrameListener.class);
    }

    /**
     * Avisa un evento de ventana interna.
     *
     * <p>El identificador decide a que metodo del oyente se llama.
     */
    protected void fireInternalFrameEvent(int id) {
        Object[] listeners = listenerList.getListenerList();
        InternalFrameEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == InternalFrameListener.class) {
                if (e == null) {
                    e = new InternalFrameEvent(this, id);
                }
                InternalFrameListener l = (InternalFrameListener) listeners[i + 1];
                repartir(l, e, id);
            }
        }
    }

    /**
     * Le da el evento al metodo que corresponde.
     *
     * <p>Es una cadena de comparaciones y no un {@code switch} porque los identificadores son
     * constantes de otra clase.
     */
    private void repartir(InternalFrameListener l, InternalFrameEvent e, int id) {
        if (id == InternalFrameEvent.INTERNAL_FRAME_OPENED) {
            l.internalFrameOpened(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_CLOSING) {
            l.internalFrameClosing(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_CLOSED) {
            l.internalFrameClosed(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_ICONIFIED) {
            l.internalFrameIconified(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED) {
            l.internalFrameDeiconified(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_ACTIVATED) {
            l.internalFrameActivated(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED) {
            l.internalFrameDeactivated(e);
        }
    }

    /**
     * Lo que pasa cuando el usuario aprieta el boton de cerrar.
     *
     * <p>Avisa que se esta cerrando y despues hace lo que diga
     * {@link #setDefaultCloseOperation}. Avisar primero es lo que le da al oyente la chance de
     * cambiar la operacion antes de que se ejecute.
     */
    public void doDefaultCloseAction() {
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        if (defaultCloseOperation == DO_NOTHING_ON_CLOSE) {
            return;
        }
        if (defaultCloseOperation == HIDE_ON_CLOSE) {
            setVisible(false);
            if (isSelected()) {
                try {
                    setSelected(false);
                } catch (PropertyVetoException pve) {
                    // Vetar la desactivacion no impide esconder: ya se escondio.
                }
            }
            return;
        }
        if (defaultCloseOperation == DISPOSE_ON_CLOSE) {
            try {
                fireVetoableChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
                isClosed = true;
                setVisible(false);
                firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
                dispose();
            } catch (PropertyVetoException pve) {
                // Alguien se opuso: la ventana queda como estaba.
            }
        }
    }

    /**
     * Que hacer al cerrar.
     *
     * <p>A diferencia de {@link JDialog}, por omision destruye: una ventana interna escondida
     * seguiria ocupando lugar en el escritorio.
     */
    public void setDefaultCloseOperation(int operation) {
        this.defaultCloseOperation = operation;
    }

    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    /** La achica al tamano que piden sus hijos. */
    public void pack() {
        try {
            if (isIcon()) {
                setIcon(false);
            } else if (isMaximum()) {
                setMaximum(false);
            }
        } catch (PropertyVetoException e) {
            // Si no se puede desiconizar o desmaximizar no tiene sentido medir: se deja como esta.
            return;
        }
        setSize(getPreferredSize());
        validate();
    }

    /**
     * @deprecated Usar {@code setVisible(true)}.
     */
    @Deprecated
    public void show() {
        if (isVisible()) {
            return;
        }
        if (!opened) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
            opened = true;
        }
        // El icono nace escondido; se prende ahora para que aparezca al minimizar.
        getDesktopIcon().setVisible(true);
        toFront();
        super.show();
        try {
            setSelected(true);
        } catch (PropertyVetoException pve) {
            // Que no se pueda activar no impide mostrarla.
        }
    }

    /**
     * @deprecated Usar {@code setVisible(false)}.
     */
    @Deprecated
    public void hide() {
        if (isIcon()) {
            getDesktopIcon().setVisible(false);
        }
        super.hide();
    }

    /** La saca del escritorio y avisa que se cerro. */
    public void dispose() {
        if (isVisible()) {
            setVisible(false);
        }
        if (isSelected()) {
            try {
                setSelected(false);
            } catch (PropertyVetoException pve) {
                // Se destruye igual.
            }
        }
        if (!isClosed) {
            firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
            isClosed = true;
        }
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
    }

    public void toFront() {
        moveToFront();
    }

    public void toBack() {
        moveToBack();
    }

    /**
     * No hace nada: una ventana interna siempre es raiz de su ciclo de foco.
     *
     * <p>Tabular adentro no debe llevar el foco al escritorio.
     */
    public final void setFocusCycleRoot(boolean value) {
    }

    /** Siempre cierto; ver {@link #setFocusCycleRoot}. */
    public final boolean isFocusCycleRoot() {
        return true;
    }

    /** Siempre nulo: no hay ciclo por encima. */
    public final Container getFocusCycleRootAncestor() {
        return null;
    }

    /** Siempre nulo: no es una ventana del sistema, no hay nada que advertir. */
    public final String getWarningString() {
        return null;
    }

    protected String paramString() {
        return super.paramString();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * El icono que reemplaza a una ventana interna minimizada.
     *
     * <p>Es un componente aparte y no una forma de dibujar la ventana: mientras la ventana esta
     * minimizada es el icono el que esta puesto en el escritorio, y la ventana no tiene padre. De
     * ahi que {@link JInternalFrame#getDesktopPane} lo consulte.
     */
    public static class JDesktopIcon extends JComponent implements Accessible {

        private JInternalFrame internalFrame;

        /** El icono de esa ventana. */
        public JDesktopIcon(JInternalFrame f) {
            setVisible(false);
            setInternalFrame(f);
            updateUI();
        }

        public DesktopIconUI getUI() {
            return (DesktopIconUI) ui;
        }

        public void setUI(DesktopIconUI ui) {
            super.setUI(ui);
        }

        public JInternalFrame getInternalFrame() {
            return internalFrame;
        }

        public void setInternalFrame(JInternalFrame f) {
            internalFrame = f;
        }

        /** El escritorio, preguntandoselo a la ventana. */
        public JDesktopPane getDesktopPane() {
            if (getInternalFrame() != null) {
                return getInternalFrame().getDesktopPane();
            }
            return null;
        }

        public void updateUI() {
        }

        public String getUIClassID() {
            return "DesktopIconUI";
        }

        public AccessibleContext getAccessibleContext() {
            return accessibleContext;
        }
    }
}
