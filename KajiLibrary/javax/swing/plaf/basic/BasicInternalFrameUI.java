package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InternalFrameUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de una ventana interna.
 *
 * <h2>Cuatro paneles alrededor del contenido, y solo uno existe</h2>
 *
 * <p>La ventana tiene lugar para un panel en cada costado -- norte, sur, este, oeste -- y el basico
 * solo pone el del norte: la barra de titulo. Los otros tres quedan en {@code null} y estan ahi para
 * el aspecto que quiera una barra de estado abajo o una regla al costado. Esta medido.
 *
 * <h2>Quien mueve la ventana no es la ventana</h2>
 *
 * <p>Mover, maximizar, iconizar y cerrar los hace el {@link DesktopManager} del escritorio, no este
 * UI: {@link #getDesktopManager} lo busca en el {@link JDesktopPane} que la contenga y, si no hay
 * ninguno --una ventana interna suelta--, arma uno propio. Eso es lo que hace que dos ventanas del
 * mismo escritorio se comporten igual aunque tengan aspectos distintos.
 *
 * <h2>El desborde de pila del JDK</h2>
 *
 * <p><strong>{@code getPreferredSize} de una ventana interna suelta desborda la pila en el
 * JDK.</strong> Esta medido: {@code StackOverflowError}, no una excepcion. El acomodador pide el
 * preferido de la ventana, que se lo vuelve a pedir al acomodador. Aca no se copia -- copiar un
 * desborde de pila no le sirve a nadie --: el acomodador mide el panel raiz y la barra de titulo,
 * que es lo que la cuenta queria decir. El minimo y el maximo si coinciden.
 *
 * <p>Para un componente que no es su ventana, los tres contestan numeros fijos: 100 x 100 el
 * preferido, cero el minimo, infinito el maximo.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Arrastrar el borde para redimensionar necesita el cursor del sistema: el escucha esta y anota
 * de que lado se apreto, pero no cambia el tamano. Y {@link #openMenuKey} queda en {@code null},
 * como los demas campos de tecla del paquete.
 */
public class BasicInternalFrameUI extends InternalFrameUI {

    protected JInternalFrame frame;
    protected MouseInputAdapter borderListener;
    protected PropertyChangeListener propertyChangeListener;
    protected LayoutManager internalFrameLayout;
    protected ComponentListener componentListener;
    protected MouseInputListener glassPaneDispatcher;

    protected JComponent northPane;
    protected JComponent southPane;
    protected JComponent westPane;
    protected JComponent eastPane;

    protected BasicInternalFrameTitlePane titlePane;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke openMenuKey;

    private boolean keyBindingRegistered;
    private boolean keyBindingActive;
    private DesktopManager sharedDesktopManager;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);

    /** Para esa ventana. */
    public BasicInternalFrameUI(JInternalFrame b) {
    }

    /** Uno nuevo por ventana: guarda sus cuatro paneles y su barra. */
    public static ComponentUI createUI(JComponent b) {
        return new BasicInternalFrameUI((JInternalFrame) b);
    }

    public void installUI(JComponent c) {
        frame = (JInternalFrame) c;
        installDefaults();
        installListeners();
        installComponents();
        installKeyboardActions();
        frame.setOpaque(true);
    }

    public void uninstallUI(JComponent c) {
        if (c != frame) {
            throw new IllegalArgumentException(c + " no es la ventana de este aspecto");
        }
        uninstallKeyboardActions();
        uninstallComponents();
        uninstallListeners();
        uninstallDefaults();
        frame = null;
    }

    /** Colores y acomodador; los valores son los de {@code InternalFrame.*} en Metal. */
    protected void installDefaults() {
        Color fondo = frame.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            frame.setBackground(FONDO);
        }
        javax.swing.border.Border b = frame.getBorder();
        if (b == null || b instanceof UIResource) {
            frame.setBorder(BasicBorders.getInternalFrameBorder());
        }
        LookAndFeel.installProperty(frame, "opaque", Boolean.TRUE);
        internalFrameLayout = createLayoutManager();
        frame.setLayout(internalFrameLayout);
    }

    /** Saca el acomodador y el borde que puso este UI. */
    protected void uninstallDefaults() {
        if (frame.getLayout() == internalFrameLayout) {
            frame.setLayout(null);
        }
        LookAndFeel.uninstallBorder(frame);
        internalFrameLayout = null;
    }

    /** Pone la barra de titulo como panel norte; ver la nota de la clase. */
    protected void installComponents() {
        setNorthPane(createNorthPane(frame));
        setSouthPane(createSouthPane(frame));
        setEastPane(createEastPane(frame));
        setWestPane(createWestPane(frame));
    }

    protected void uninstallComponents() {
        setNorthPane(null);
        setSouthPane(null);
        setEastPane(null);
        setWestPane(null);
        titlePane = null;
    }

    protected void installListeners() {
        borderListener = createBorderListener(frame);
        propertyChangeListener = createPropertyChangeListener();
        frame.addPropertyChangeListener(propertyChangeListener);
        componentListener = createComponentListener();
        if (frame.getParent() != null) {
            frame.getParent().addComponentListener(componentListener);
        }
        installMouseHandlers(frame);
    }

    protected void uninstallListeners() {
        if (componentListener != null && frame.getParent() != null) {
            frame.getParent().removeComponentListener(componentListener);
        }
        deinstallMouseHandlers(frame);
        frame.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
        borderListener = null;
        componentListener = null;
        glassPaneDispatcher = null;
    }

    /** Sin atajos propios; ver la nota de la clase sobre {@link #openMenuKey}. */
    protected void installKeyboardActions() {
        setupMenuOpenKey();
        setupMenuCloseKey();
    }

    protected void uninstallKeyboardActions() {
    }

    /** Gancho para el aspecto que quiera atar una tecla al menu de sistema; el basico no ata. */
    protected void setupMenuOpenKey() {
    }

    /** Idem para cerrarlo. */
    protected void setupMenuCloseKey() {
    }

    protected final boolean isKeyBindingRegistered() {
        return keyBindingRegistered;
    }

    protected final void setKeyBindingRegistered(boolean b) {
        keyBindingRegistered = b;
    }

    public final boolean isKeyBindingActive() {
        return keyBindingActive;
    }

    protected final void setKeyBindingActive(boolean b) {
        keyBindingActive = b;
    }

    protected LayoutManager createLayoutManager() {
        return new Handler(this);
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    protected ComponentListener createComponentListener() {
        return new Handler(this);
    }

    protected MouseInputAdapter createBorderListener(JInternalFrame w) {
        return new EscuchaDeBorde(this);
    }

    /**
     * El que reparte los eventos del cristal.
     *
     * <p>Devuelve uno, pero {@link #installComponents} no lo guarda: el campo
     * {@link #glassPaneDispatcher} queda en {@code null} despues de instalar. Esta medido, y tiene
     * sentido -- sin ventana de verdad no hay cristal que despachar --.
     */
    protected MouseInputListener createGlassPaneDispatcher() {
        return new EscuchaDeBorde(this);
    }

    /** Gancho: el basico no arma ningun escucha de ventana interna. */
    protected void createInternalFrameListener() {
    }

    protected void installMouseHandlers(JComponent c) {
        if (borderListener != null) {
            c.addMouseListener(borderListener);
            c.addMouseMotionListener(borderListener);
        }
    }

    protected void deinstallMouseHandlers(JComponent c) {
        if (borderListener != null) {
            c.removeMouseListener(borderListener);
            c.removeMouseMotionListener(borderListener);
        }
    }

    /** La barra de titulo. */
    protected JComponent createNorthPane(JInternalFrame w) {
        titlePane = new BasicInternalFrameTitlePane(w);
        return titlePane;
    }

    /** Ninguno; ver la nota de la clase. */
    protected JComponent createSouthPane(JInternalFrame w) {
        return null;
    }

    /** Ninguno. */
    protected JComponent createWestPane(JInternalFrame w) {
        return null;
    }

    /** Ninguno. */
    protected JComponent createEastPane(JInternalFrame w) {
        return null;
    }

    public JComponent getNorthPane() {
        return northPane;
    }

    public void setNorthPane(JComponent c) {
        if (northPane != null && northPane instanceof BasicInternalFrameTitlePane) {
            ((BasicInternalFrameTitlePane) northPane).uninstallListeners();
        }
        replacePane(northPane, c);
        northPane = c;
        if (c instanceof BasicInternalFrameTitlePane) {
            titlePane = (BasicInternalFrameTitlePane) c;
        }
    }

    public JComponent getSouthPane() {
        return southPane;
    }

    public void setSouthPane(JComponent c) {
        replacePane(southPane, c);
        southPane = c;
    }

    public JComponent getWestPane() {
        return westPane;
    }

    public void setWestPane(JComponent c) {
        replacePane(westPane, c);
        westPane = c;
    }

    public JComponent getEastPane() {
        return eastPane;
    }

    public void setEastPane(JComponent c) {
        replacePane(eastPane, c);
        eastPane = c;
    }

    /** Saca el viejo del contenedor y pone el nuevo, con sus escuchas de mouse. */
    protected void replacePane(JComponent currentPane, JComponent newPane) {
        if (currentPane != null) {
            deinstallMouseHandlers(currentPane);
            frame.remove(currentPane);
        }
        if (newPane != null) {
            frame.add(newPane);
            installMouseHandlers(newPane);
        }
    }

    /** El del escritorio que la contenga, o uno propio; ver la nota de la clase. */
    protected DesktopManager getDesktopManager() {
        JDesktopPane pane = frame.getDesktopPane();
        if (pane != null && pane.getDesktopManager() != null) {
            return pane.getDesktopManager();
        }
        if (sharedDesktopManager == null) {
            sharedDesktopManager = createDesktopManager();
        }
        return sharedDesktopManager;
    }

    protected DesktopManager createDesktopManager() {
        return new DefaultDesktopManager();
    }

    /** Las seis operaciones, todas delegadas al administrador. */
    protected void closeFrame(JInternalFrame f) {
        getDesktopManager().closeFrame(f);
    }

    protected void maximizeFrame(JInternalFrame f) {
        getDesktopManager().maximizeFrame(f);
    }

    protected void minimizeFrame(JInternalFrame f) {
        getDesktopManager().minimizeFrame(f);
    }

    protected void iconifyFrame(JInternalFrame f) {
        getDesktopManager().iconifyFrame(f);
    }

    protected void deiconifyFrame(JInternalFrame f) {
        getDesktopManager().deiconifyFrame(f);
    }

    protected void activateFrame(JInternalFrame f) {
        getDesktopManager().activateFrame(f);
    }

    protected void deactivateFrame(JInternalFrame f) {
        getDesktopManager().deactivateFrame(f);
    }

    /** El del acomodador para su ventana, y 100 x 100 para cualquier otro componente. */
    public Dimension getPreferredSize(JComponent x) {
        if (frame == x && internalFrameLayout != null) {
            return internalFrameLayout.preferredLayoutSize(x);
        }
        return new Dimension(100, 100);
    }

    /** El del acomodador para su ventana, y cero para cualquier otro. */
    public Dimension getMinimumSize(JComponent x) {
        if (frame == x && internalFrameLayout != null) {
            return internalFrameLayout.minimumLayoutSize(x);
        }
        return new Dimension(0, 0);
    }

    /** Sin tope, siempre. */
    public Dimension getMaximumSize(JComponent x) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * El que acomoda los cinco lugares y escucha los cambios de la ventana.
     *
     * <p>El contenido va en el centro y los cuatro paneles alrededor. Es un {@code BorderLayout}
     * escrito a mano, y esta escrito porque el de verdad no sabe que el panel raiz de la ventana es
     * el centro pase lo que pase.
     */
    private static class Handler implements LayoutManager, PropertyChangeListener,
            ComponentListener {

        private final BasicInternalFrameUI ui;

        Handler(BasicInternalFrameUI ui) {
            this.ui = ui;
        }

        public void addLayoutComponent(String name, Component c) {
        }

        public void removeLayoutComponent(Component c) {
        }

        /** Ver la nota de la clase sobre el desborde de pila del JDK. */
        public Dimension preferredLayoutSize(Container c) {
            return medir(c, false);
        }

        public Dimension minimumLayoutSize(Container c) {
            return medir(c, true);
        }

        private Dimension medir(Container c, boolean minimo) {
            JInternalFrame frame = ui.frame;
            if (frame == null) {
                return new Dimension(0, 0);
            }
            Dimension d = new Dimension(0, 0);
            javax.swing.JRootPane raiz = frame.getRootPane();
            if (raiz != null) {
                Dimension r = minimo ? raiz.getMinimumSize() : raiz.getPreferredSize();
                d.width = r.width;
                d.height = r.height;
            }
            JComponent[] verticales = {ui.northPane, ui.southPane};
            for (int i = 0; i < verticales.length; i++) {
                if (verticales[i] != null) {
                    Dimension p = minimo ? verticales[i].getMinimumSize()
                            : verticales[i].getPreferredSize();
                    d.width = Math.max(d.width, p.width);
                    d.height += p.height;
                }
            }
            JComponent[] horizontales = {ui.westPane, ui.eastPane};
            for (int i = 0; i < horizontales.length; i++) {
                if (horizontales[i] != null) {
                    Dimension p = minimo ? horizontales[i].getMinimumSize()
                            : horizontales[i].getPreferredSize();
                    d.width += p.width;
                    d.height = Math.max(d.height, p.height);
                }
            }
            Insets in = frame.getInsets();
            d.width += in.left + in.right;
            d.height += in.top + in.bottom;
            return d;
        }

        public void layoutContainer(Container c) {
            JInternalFrame frame = ui.frame;
            if (frame == null) {
                return;
            }
            Insets in = frame.getInsets();
            int cx = in.left;
            int cy = in.top;
            int cw = frame.getWidth() - in.left - in.right;
            int ch = frame.getHeight() - in.top - in.bottom;

            if (ui.northPane != null) {
                Dimension d = ui.northPane.getPreferredSize();
                ui.northPane.setBounds(cx, cy, cw, d.height);
                cy += d.height;
                ch -= d.height;
            }
            if (ui.southPane != null) {
                Dimension d = ui.southPane.getPreferredSize();
                ui.southPane.setBounds(cx, cy + ch - d.height, cw, d.height);
                ch -= d.height;
            }
            if (ui.westPane != null) {
                Dimension d = ui.westPane.getPreferredSize();
                ui.westPane.setBounds(cx, cy, d.width, ch);
                cx += d.width;
                cw -= d.width;
            }
            if (ui.eastPane != null) {
                Dimension d = ui.eastPane.getPreferredSize();
                ui.eastPane.setBounds(cx + cw - d.width, cy, d.width, ch);
                cw -= d.width;
            }
            javax.swing.JRootPane raiz = frame.getRootPane();
            if (raiz != null) {
                raiz.setBounds(cx, cy, cw, ch);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            JInternalFrame frame = ui.frame;
            if (frame == null) {
                return;
            }
            String prop = e.getPropertyName();
            if (JInternalFrame.IS_CLOSED_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.closeFrame(frame);
                }
            } else if (JInternalFrame.IS_MAXIMUM_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.maximizeFrame(frame);
                } else {
                    ui.minimizeFrame(frame);
                }
            } else if (JInternalFrame.IS_ICON_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.iconifyFrame(frame);
                } else {
                    ui.deiconifyFrame(frame);
                }
            } else if (JInternalFrame.IS_SELECTED_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.activateFrame(frame);
                } else {
                    ui.deactivateFrame(frame);
                }
            }
        }

        public void componentResized(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }
    }

    /**
     * El que anota de que lado del borde se apreto.
     *
     * <p>Redimensionar de verdad necesita el cursor del sistema; ver la nota de la clase.
     */
    private static class EscuchaDeBorde extends MouseInputAdapter implements MouseInputListener {

        private final BasicInternalFrameUI ui;

        EscuchaDeBorde(BasicInternalFrameUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            JInternalFrame frame = ui.frame;
            if (frame == null || !frame.isSelected()) {
                if (frame != null) {
                    try {
                        frame.setSelected(true);
                    } catch (PropertyVetoException ex) {
                        // Alguien dijo que no. Es una respuesta valida.
                    }
                }
            }
        }
    }
}
