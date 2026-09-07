package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ToolBarUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de una barra de herramientas.
 *
 * <h2>Una barra que se puede arrancar de su lugar</h2>
 *
 * <p>Lo que distingue a una barra de herramientas de una fila de botones es que se puede agarrar del
 * borde y llevar a otro lado: a otro costado de la ventana, o afuera, flotando en una ventanita
 * propia. Eso es casi todo lo que hace esta clase, y es la razon de sus cuatro colores --dos para
 * la barra anclada y dos para la que flota-- y de la {@code DragWindow}, el rectangulo que se ve
 * mientras se arrastra.
 *
 * <p>{@link #constraintBeforeFloating} guarda de que lado estaba antes de salir volando, para poder
 * devolverla ahi. Arranca en {@code "North"}, que es donde va una barra que nadie movio.
 *
 * <h2>Bordes que aparecen al pasar el mouse</h2>
 *
 * <p>Los botones de una barra no llevan borde hasta que el mouse pasa por encima; eso es lo que hace
 * que una barra se vea como una fila de iconos y no como una fila de botones.
 * {@link #setRolloverBorders} cambia entre los dos juegos, y los dos bordes se crean una sola vez y
 * se comparten -- {@link #getRolloverBorder} devuelve siempre el mismo objeto --.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Sacar la barra a flotar necesita una ventana de verdad: {@link #createFloatingWindow} y
 * {@link #floatAt} estan escritos y no se pueden probar sin pantalla. Lo que si se prueba es el
 * estado: los colores, los bordes, y que {@link #isFloating} diga que no.
 *
 * <p>{@link #canDock} revienta con un componente nulo, igual que el JDK: pregunta si el punto cae
 * adentro sin comprobar nada primero.
 *
 * <p>Las cuatro teclas protegidas quedan en nulo, como en {@link BasicSplitPaneUI}.
 */
public class BasicToolBarUI extends ToolBarUI implements SwingConstants {

    protected JToolBar toolBar;

    /** Cual boton tenia el foco antes de arrastrar; -1 si ninguno. */
    protected int focusedCompIndex = -1;

    protected Color dockingColor;
    protected Color floatingColor;
    protected Color dockingBorderColor;
    protected Color floatingBorderColor;

    protected MouseInputListener dockingListener;
    protected PropertyChangeListener propertyListener;
    protected ContainerListener toolBarContListener;
    protected FocusListener toolBarFocusListener;

    /** De que lado estaba antes de flotar; ver la nota de la clase. */
    protected String constraintBeforeFloating = BorderLayoutNorte.NORTH;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke upKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke downKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke leftKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke rightKey;

    /** El rectangulo que se ve al arrastrar; nulo hasta que alguien arrastra. */
    protected DragWindow dragWindow;

    private boolean rolloverBorders = true;
    private boolean floating;
    private Border rolloverBorder;
    private Border nonRolloverBorder;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource BORDE_ANCLAJE = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource BORDE_FLOTANTE = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicToolBarUI() {
    }

    /** Uno nuevo por barra: guarda la barra, sus colores y el estado del arrastre. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicToolBarUI();
    }

    public void installUI(JComponent c) {
        toolBar = (JToolBar) c;
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
        floating = false;
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallComponents();
        uninstallListeners();
        uninstallKeyboardActions();
        dragWindow = null;
        toolBar = null;
    }

    /** Colores, fuente y bordes; los valores son los de {@code ToolBar.*} en Metal. */
    protected void installDefaults() {
        Color fondo = toolBar.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            toolBar.setBackground(FONDO);
        }
        Color frente = toolBar.getForeground();
        if (frente == null || frente instanceof UIResource) {
            toolBar.setForeground(FRENTE);
        }
        Font fuente = toolBar.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            toolBar.setFont(FUENTE);
        }
        LookAndFeel.installProperty(toolBar, "opaque", Boolean.TRUE);
        dockingColor = FONDO;
        floatingColor = FONDO;
        dockingBorderColor = BORDE_ANCLAJE;
        floatingBorderColor = BORDE_FLOTANTE;
        rolloverBorders = true;
        setRolloverBorders(rolloverBorders);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** Nada: los botones los pone el programa. */
    protected void installComponents() {
    }

    protected void uninstallComponents() {
    }

    protected void installListeners() {
        dockingListener = createDockingListener();
        if (dockingListener != null) {
            toolBar.addMouseListener(dockingListener);
            toolBar.addMouseMotionListener(dockingListener);
        }
        propertyListener = createPropertyListener();
        if (propertyListener != null) {
            toolBar.addPropertyChangeListener(propertyListener);
        }
        toolBarContListener = createToolBarContListener();
        if (toolBarContListener != null) {
            toolBar.addContainerListener(toolBarContListener);
        }
        toolBarFocusListener = createToolBarFocusListener();
        if (toolBarFocusListener != null) {
            for (int i = 0; i < toolBar.getComponentCount(); i++) {
                toolBar.getComponent(i).addFocusListener(toolBarFocusListener);
            }
        }
    }

    protected void uninstallListeners() {
        if (dockingListener != null) {
            toolBar.removeMouseListener(dockingListener);
            toolBar.removeMouseMotionListener(dockingListener);
        }
        if (propertyListener != null) {
            toolBar.removePropertyChangeListener(propertyListener);
        }
        if (toolBarContListener != null) {
            toolBar.removeContainerListener(toolBarContListener);
        }
        if (toolBarFocusListener != null) {
            for (int i = 0; i < toolBar.getComponentCount(); i++) {
                toolBar.getComponent(i).removeFocusListener(toolBarFocusListener);
            }
        }
        dockingListener = null;
        propertyListener = null;
        toolBarContListener = null;
        toolBarFocusListener = null;
    }

    /** Sin atajos propios; ver la nota de la clase sobre las cuatro teclas. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected MouseInputListener createDockingListener() {
        return new Handler(this);
    }

    protected PropertyChangeListener createPropertyListener() {
        return new Handler(this);
    }

    protected ContainerListener createToolBarContListener() {
        return new Handler(this);
    }

    protected FocusListener createToolBarFocusListener() {
        return new Handler(this);
    }

    protected WindowListener createFrameListener() {
        return new Handler(this);
    }

    /** El borde que se ve al pasar el mouse; ver la nota de la clase. */
    protected Border createRolloverBorder() {
        return new CompoundBorder(
                new javax.swing.border.EtchedBorder(),
                new BasicBorders.MarginBorder());
    }

    /** Y el que se ve cuando no. */
    protected Border createNonRolloverBorder() {
        return new CompoundBorder(
                new javax.swing.border.EmptyBorder(2, 2, 2, 2),
                new BasicBorders.MarginBorder());
    }

    /** El mismo objeto siempre; ver la nota de la clase. */
    protected Border getRolloverBorder(AbstractButton b) {
        if (rolloverBorder == null) {
            rolloverBorder = createRolloverBorder();
        }
        return rolloverBorder;
    }

    /** Idem. */
    protected Border getNonRolloverBorder(AbstractButton b) {
        if (nonRolloverBorder == null) {
            nonRolloverBorder = createNonRolloverBorder();
        }
        return nonRolloverBorder;
    }

    public boolean isRolloverBorders() {
        return rolloverBorders;
    }

    /** Cambia los botones de un juego de bordes al otro. */
    public void setRolloverBorders(boolean rollover) {
        rolloverBorders = rollover;
        if (toolBar == null) {
            return;
        }
        if (rolloverBorders) {
            installRolloverBorders(toolBar);
        } else {
            installNonRolloverBorders(toolBar);
        }
    }

    protected void installRolloverBorders(JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JComponent) {
                installRolloverBorders((JComponent) comp);
                setBorderToRollover(comp);
            }
        }
    }

    protected void installNonRolloverBorders(JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JComponent) {
                installNonRolloverBorders((JComponent) comp);
                setBorderToNonRollover(comp);
            }
        }
    }

    /** Les devuelve a los botones el borde que traian. */
    protected void installNormalBorders(JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JComponent) {
                installNormalBorders((JComponent) comp);
                setBorderToNormal(comp);
            }
        }
    }

    /**
     * Le pone al boton el borde de pasar el mouse.
     *
     * <p>Solo si el que tiene lo puso un aspecto. Eso tiene una consecuencia que sorprende y esta
     * medida: <strong>una vez cambiado, no se vuelve atras</strong>. El borde que pone este metodo
     * no es {@link UIResource}, asi que la proxima llamada --a este o a
     * {@link #setBorderToNonRollover}-- ya no lo toca. Cambiar de juego de bordes en caliente
     * funciona una sola vez por boton.
     */
    protected void setBorderToRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            if (b.getBorder() instanceof UIResource) {
                b.setBorder(getRolloverBorder(b));
            }
            b.setRolloverEnabled(true);
        }
    }

    /** Idem al reves; ver {@link #setBorderToRollover}. */
    protected void setBorderToNonRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            if (b.getBorder() instanceof UIResource) {
                b.setBorder(getNonRolloverBorder(b));
            }
            b.setRolloverEnabled(false);
        }
    }

    protected void setBorderToNormal(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            if (b.getBorder() == rolloverBorder || b.getBorder() == nonRolloverBorder) {
                b.setBorder(null);
            }
        }
    }

    public Color getDockingColor() {
        return dockingColor;
    }

    public void setDockingColor(Color c) {
        this.dockingColor = c;
    }

    public Color getFloatingColor() {
        return floatingColor;
    }

    public void setFloatingColor(Color c) {
        this.floatingColor = c;
    }

    public boolean isFloating() {
        return floating;
    }

    /** Saca la barra a flotar o la devuelve; ver la nota de la clase. */
    public void setFloating(boolean b, Point p) {
        if (toolBar.isFloatable()) {
            floating = b;
        }
    }

    public void setFloatingLocation(int x, int y) {
    }

    /** Cambia el eje de la barra. */
    public void setOrientation(int orientation) {
        toolBar.setOrientation(orientation);
        if (dragWindow != null) {
            dragWindow.setOrientation(orientation);
        }
    }

    /**
     * Si la barra se puede anclar en ese componente y en ese punto.
     *
     * @throws NullPointerException si el componente es nulo; el JDK tampoco lo comprueba
     */
    public boolean canDock(Component c, Point p) {
        return p != null && c.contains(p);
    }

    /** La ventanita en la que flota la barra; ver la nota de la clase. */
    protected JFrame createFloatingFrame(JToolBar toolbar) {
        return new JFrame(toolbar.getName());
    }

    /** Idem, cuando el aspecto prefiere un dialogo. */
    protected RootPaneContainer createFloatingWindow(JToolBar toolbar) {
        return createFloatingFrame(toolbar);
    }

    protected DragWindow createDragWindow(JToolBar toolbar) {
        return new DragWindow(this);
    }

    /** Mueve el rectangulo de arrastre. */
    protected void dragTo(Point position, Point origin) {
        if (!toolBar.isFloatable()) {
            return;
        }
        if (dragWindow == null) {
            dragWindow = createDragWindow(toolBar);
        }
        dragWindow.setOrientation(toolBar.getOrientation());
        dragWindow.setBorderColor(dockingBorderColor);
    }

    /** Suelta la barra donde este; ver la nota de la clase. */
    protected void floatAt(Point position, Point origin) {
        if (toolBar.isFloatable()) {
            setFloating(true, position);
        }
    }

    /** El rectangulo del arrastre. */
    protected void paintDragWindow(Graphics g) {
        g.setColor(dragWindow.getBorderColor());
        int w = dragWindow.getWidth();
        int h = dragWindow.getHeight();
        g.drawRect(0, 0, w - 1, h - 1);
    }

    /** Mueve el foco al boton siguiente o al anterior. */
    protected void navigateFocusedComp(int direction) {
        int nComp = toolBar.getComponentCount();
        if (focusedCompIndex < 0 || focusedCompIndex >= nComp) {
            return;
        }
        int paso = (direction == EAST || direction == SOUTH) ? 1 : -1;
        for (int i = 1; i < nComp; i++) {
            int j = ((focusedCompIndex + paso * i) % nComp + nComp) % nComp;
            Component c = toolBar.getComponent(j);
            if (c != null && c.isFocusable() && c.isEnabled()) {
                c.requestFocus();
                return;
            }
        }
    }

    /**
     * El rectangulo que se ve mientras se arrastra la barra.
     *
     * <p>Es una ventana propia en el JDK. Aca es un componente suelto: sin pantalla no hay ventana
     * que mostrar, y lo unico que se puede probar de el es su color y su orientacion.
     */
    protected class DragWindow extends java.awt.Window {

        private Color borderColor;
        private int orientation;

        DragWindow(BasicToolBarUI ui) {
            super(new java.awt.Frame());
            this.orientation = SwingConstants.HORIZONTAL;
        }

        public void setOrientation(int o) {
            this.orientation = o;
        }

        public int getOrientation() {
            return orientation;
        }

        public Color getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(Color c) {
            this.borderColor = c;
        }

        public void setOffset(Point p) {
        }

        public Point getOffset() {
            return new Point(0, 0);
        }
    }

    /** Los nombres de los cuatro costados, sin depender de {@code java.awt.BorderLayout}. */
    private static final class BorderLayoutNorte {
        static final String NORTH = "North";

        private BorderLayoutNorte() {
        }
    }

    /**
     * El que escucha todo: el arrastre, los cambios de la barra, y los botones que entran y salen.
     *
     * <p>Estatico y con el UI como campo, por lo mismo que en todo el paquete; ver el hallazgo #518.
     */
    private static class Handler implements MouseInputListener, PropertyChangeListener,
            ContainerListener, FocusListener, WindowListener {

        private final BasicToolBarUI ui;
        private Point origin;

        Handler(BasicToolBarUI ui) {
            this.ui = ui;
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!ui.toolBar.isEnabled() || !ui.toolBar.isFloatable()) {
                return;
            }
            origin = e.getPoint();
        }

        public void mouseReleased(MouseEvent e) {
            if (origin == null) {
                return;
            }
            ui.floatAt(e.getPoint(), origin);
            origin = null;
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (origin != null) {
                ui.dragTo(e.getPoint(), origin);
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void propertyChange(PropertyChangeEvent e) {
            String nombre = e.getPropertyName();
            if ("rollover".equals(nombre)) {
                ui.setRolloverBorders(Boolean.TRUE.equals(e.getNewValue()));
            } else if ("orientation".equals(nombre) && ui.dragWindow != null) {
                ui.dragWindow.setOrientation(ui.toolBar.getOrientation());
            }
        }

        public void componentAdded(ContainerEvent e) {
            Component c = e.getChild();
            if (ui.toolBarFocusListener != null) {
                c.addFocusListener(ui.toolBarFocusListener);
            }
            if (ui.isRolloverBorders()) {
                ui.setBorderToRollover(c);
            } else {
                ui.setBorderToNonRollover(c);
            }
        }

        public void componentRemoved(ContainerEvent e) {
            Component c = e.getChild();
            if (ui.toolBarFocusListener != null) {
                c.removeFocusListener(ui.toolBarFocusListener);
            }
            ui.setBorderToNormal(c);
        }

        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            for (int i = 0; i < ui.toolBar.getComponentCount(); i++) {
                if (ui.toolBar.getComponent(i) == c) {
                    ui.focusedCompIndex = i;
                    return;
                }
            }
        }

        public void focusLost(FocusEvent e) {
        }

        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            ui.setFloating(false, null);
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }
}
