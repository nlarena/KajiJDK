package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.MenuItemUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un item de menu.
 *
 * <h2>Un item de menu no es un boton con otro borde</h2>
 *
 * <p>Se le parece --tiene modelo, texto e icono-- pero tiene tres columnas propias que un boton no
 * tiene: la del tilde a la izquierda, la del acelerador a la derecha, y la de la flecha de submenu
 * al final. Ubicar esas tres es casi todo lo que hace esta clase.
 *
 * <h2>La cuenta del tamano, y por que termina en impar</h2>
 *
 * <p>El ancho se arma sumando de izquierda a derecha: los margenes, el tilde con su separacion, el
 * icono con la suya, el texto con la suya, el acelerador con la suya, la flecha con la suya, y un
 * ultimo respiro. Todas las separaciones son el mismo {@link #defaultTextIconGap}.
 *
 * <p>Y despues, si el ancho o el alto quedaron pares, se les suma uno. Esa es una rareza del JDK
 * que <em>hay</em> que copiar: los iconos de tilde se dibujan centrados, y un ancho par los deja
 * medio pixel corridos y con el borde comido. Esta medido en doce casos --texto vacio, con icono,
 * con tilde, con tres aceleradores distintos, menu suelto y menu de barra-- y la formula da el
 * numero exacto en los doce.
 *
 * <p>El texto vacio no reserva alto de linea: un item sin texto mide lo que mida su flecha.
 *
 * <h2>Ni minimo ni maximo</h2>
 *
 * <p>{@link #getMinimumSize} y {@link #getMaximumSize} devuelven {@code null}. No es un olvido: un
 * item de menu no se estira ni se achica solo, y quien lo acomoda es {@code DefaultMenuLayout},
 * que reparte el ancho de la ventana emergente entera. Contestar un numero seria mentirle.
 *
 * <h2>Lo que instala, y de donde sale</h2>
 *
 * <p>Los valores de {@code MenuItem.*} medidos en Metal (JDK 25): fuente Dialog negrita 12, frente
 * (51, 51, 51), fondo (238, 238, 238), seleccion (163, 184, 204) sobre (51, 51, 51), apagado
 * (153, 153, 153), acelerador (99, 130, 191) en Dialog 10, separador de acelerador "-", separacion
 * 4, margen (2, 2, 2, 2), y sin tilde --el tilde solo lo tienen los items marcables--.
 *
 * <h2>Cuando no hay tilde ni flecha</h2>
 *
 * <p>Un menu que cuelga directamente de la barra no lleva ninguna de las dos: no tiene estado que
 * marcar y su submenu se abre para abajo, no para el costado. {@link #useCheckAndArrow} es quien lo
 * decide, y de ahi salen los ocho pixeles de diferencia entre un menu de barra y uno de adentro.
 */
public class BasicMenuItemUI extends MenuItemUI {

    protected JMenuItem menuItem = null;
    protected Color selectionBackground;
    protected Color selectionForeground;
    protected Color disabledForeground;
    protected Color acceleratorForeground;
    protected Color acceleratorSelectionForeground;

    /** Lo que va entre las teclas de un acelerador: {@code "Ctrl-O"}. */
    protected String acceleratorDelimiter;

    protected int defaultTextIconGap;
    protected Font acceleratorFont;

    protected MouseInputListener mouseInputListener;
    protected MenuDragMouseListener menuDragMouseListener;
    protected MenuKeyListener menuKeyListener;
    protected PropertyChangeListener propertyChangeListener;

    protected Icon arrowIcon = null;
    protected Icon checkIcon = null;

    /** Si el item pintaba su borde antes de que lo instalaran; se restaura al sacarlo. */
    protected boolean oldBorderPainted;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource SELECCION = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource APAGADO = new ColorUIResource(153, 153, 153);
    private static final ColorUIResource ACELERADOR = new ColorUIResource(99, 130, 191);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource FUENTE_ACELERADOR =
            new FontUIResource("Dialog", Font.PLAIN, 10);

    public BasicMenuItemUI() {
    }

    /** Uno nuevo por item: guarda el componente y sus escuchas. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicMenuItemUI();
    }

    public void installUI(JComponent c) {
        menuItem = (JMenuItem) c;
        installDefaults();
        installComponents(menuItem);
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        menuItem = (JMenuItem) c;
        uninstallDefaults();
        uninstallComponents(menuItem);
        uninstallListeners();
        uninstallKeyboardActions();
        menuItem = null;
    }

    /** El prefijo con el que se buscan los valores del aspecto. */
    protected String getPropertyPrefix() {
        return "MenuItem";
    }

    /** Colores, fuentes, iconos y margen; ver la nota de la clase. */
    protected void installDefaults() {
        Color fondo = menuItem.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            menuItem.setBackground(FONDO);
        }
        Color frente = menuItem.getForeground();
        if (frente == null || frente instanceof UIResource) {
            menuItem.setForeground(FRENTE);
        }
        Font fuente = menuItem.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            menuItem.setFont(FUENTE);
        }
        Insets margen = menuItem.getMargin();
        if (margen == null || margen instanceof UIResource) {
            menuItem.setMargin(new InsetsUIResource(2, 2, 2, 2));
        }
        if (menuItem.getBorder() == null || menuItem.getBorder() instanceof UIResource) {
            menuItem.setBorder(new BasicBorders.MarginBorder());
        }

        selectionBackground = SELECCION;
        selectionForeground = FRENTE;
        disabledForeground = APAGADO;
        acceleratorForeground = ACELERADOR;
        acceleratorSelectionForeground = FRENTE;
        acceleratorFont = FUENTE_ACELERADOR;
        acceleratorDelimiter = "-";
        defaultTextIconGap = 4;

        // El item comun no lleva tilde; el marcable y el de opcion lo ponen en su propia clase.
        checkIcon = null;
        arrowIcon = BasicIconFactory.getMenuItemArrowIcon();

        oldBorderPainted = menuItem.isBorderPainted();
        LookAndFeel.installProperty(menuItem, "borderPainted", Boolean.TRUE);
        LookAndFeel.installProperty(menuItem, "opaque", Boolean.TRUE);
        menuItem.setHorizontalTextPosition(SwingConstants.TRAILING);
        menuItem.setHorizontalAlignment(SwingConstants.LEADING);
    }

    /** Devuelve el pintado de borde a como estaba; ver {@link #oldBorderPainted}. */
    protected void uninstallDefaults() {
        LookAndFeel.installProperty(menuItem, "borderPainted",
                Boolean.valueOf(oldBorderPainted));
        selectionBackground = null;
        selectionForeground = null;
        disabledForeground = null;
        acceleratorForeground = null;
        acceleratorSelectionForeground = null;
        acceleratorFont = null;
        arrowIcon = null;
        checkIcon = null;
    }

    /** Arma la vista de HTML si el texto la necesita. */
    protected void installComponents(JMenuItem menuItem) {
        BasicHTML.updateRenderer(menuItem, menuItem.getText());
    }

    protected void uninstallComponents(JMenuItem menuItem) {
        BasicHTML.updateRenderer(menuItem, "");
    }

    protected void installListeners() {
        mouseInputListener = createMouseInputListener(menuItem);
        if (mouseInputListener != null) {
            menuItem.addMouseListener(mouseInputListener);
            menuItem.addMouseMotionListener(mouseInputListener);
        }
        menuDragMouseListener = createMenuDragMouseListener(menuItem);
        if (menuDragMouseListener != null) {
            menuItem.addMenuDragMouseListener(menuDragMouseListener);
        }
        menuKeyListener = createMenuKeyListener(menuItem);
        if (menuKeyListener != null) {
            menuItem.addMenuKeyListener(menuKeyListener);
        }
        propertyChangeListener = createPropertyChangeListener(menuItem);
        if (propertyChangeListener != null) {
            menuItem.addPropertyChangeListener(propertyChangeListener);
        }
    }

    protected void uninstallListeners() {
        if (mouseInputListener != null) {
            menuItem.removeMouseListener(mouseInputListener);
            menuItem.removeMouseMotionListener(mouseInputListener);
        }
        if (menuDragMouseListener != null) {
            menuItem.removeMenuDragMouseListener(menuDragMouseListener);
        }
        if (menuKeyListener != null) {
            menuItem.removeMenuKeyListener(menuKeyListener);
        }
        if (propertyChangeListener != null) {
            menuItem.removePropertyChangeListener(propertyChangeListener);
        }
        mouseInputListener = null;
        menuDragMouseListener = null;
        menuKeyListener = null;
        propertyChangeListener = null;
    }

    /** Sin atajos propios: los de un item los maneja el acelerador del propio {@link JMenuItem}. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected MouseInputListener createMouseInputListener(JComponent c) {
        return new Handler();
    }

    protected MenuDragMouseListener createMenuDragMouseListener(JComponent c) {
        return new Handler();
    }

    /**
     * Ninguno.
     *
     * <p>Un item comun no escucha teclas: la letra subrayada la maneja el menu que lo contiene, y
     * el acelerador el propio {@link JMenuItem}. Devolver {@code null} deja el campo en nulo, y eso
     * es lo que se ve del otro lado. Medido.
     */
    protected MenuKeyListener createMenuKeyListener(JComponent c) {
        return null;
    }

    protected PropertyChangeListener createPropertyChangeListener(JComponent c) {
        return new Handler();
    }

    /** Ver la nota de la clase: lo calcula {@link #getPreferredMenuItemSize}. */
    public Dimension getPreferredSize(JComponent c) {
        return getPreferredMenuItemSize(c, checkIcon, arrowIcon, defaultTextIconGap);
    }

    /** {@code null}; ver la nota de la clase. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** {@code null}; ver la nota de la clase. */
    public Dimension getMaximumSize(JComponent c) {
        return null;
    }

    /**
     * Si este item lleva columna de tilde y de flecha.
     *
     * <p>No la lleva el menu que cuelga de la barra; ver la nota de la clase.
     */
    private boolean useCheckAndArrow() {
        return !(menuItem instanceof JMenu && ((JMenu) menuItem).isTopLevelMenu());
    }

    /** El texto del acelerador, ya armado: {@code "Ctrl-O"}. */
    private String textoDelAcelerador() {
        KeyStroke accelerator = menuItem.getAccelerator();
        if (accelerator == null) {
            return "";
        }
        String s = "";
        int modifiers = accelerator.getModifiers();
        if (modifiers > 0) {
            s = KeyEvent.getKeyModifiersText(modifiers) + acceleratorDelimiter;
        }
        int keyCode = accelerator.getKeyCode();
        if (keyCode != 0) {
            s += KeyEvent.getKeyText(keyCode);
        } else {
            s += accelerator.getKeyChar();
        }
        return s;
    }

    /** La cuenta entera; ver la nota de la clase. */
    protected Dimension getPreferredMenuItemSize(JComponent c, Icon checkIcon, Icon arrowIcon,
            int defaultTextIconGap) {
        JMenuItem b = (JMenuItem) c;
        Icon icon = b.getIcon();
        String text = b.getText();
        String acceleratorText = textoDelAcelerador();
        boolean hayTexto = text != null && !text.equals("");
        boolean conColumnas = useCheckAndArrow();

        FontMetrics fm = b.getFontMetrics(b.getFont());
        FontMetrics fmAccel = b.getFontMetrics(acceleratorFont);

        int anchoTexto = 0;
        int altoTexto = 0;
        if (hayTexto) {
            javax.swing.text.View v =
                    (javax.swing.text.View) b.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                anchoTexto = (int) v.getPreferredSpan(javax.swing.text.View.X_AXIS);
                altoTexto = (int) v.getPreferredSpan(javax.swing.text.View.Y_AXIS);
            } else {
                anchoTexto = fm.stringWidth(text);
                altoTexto = fm.getHeight();
            }
        }

        Insets insets = b.getInsets();
        int w = insets.left + insets.right;
        int alto = altoTexto;

        if (conColumnas && checkIcon != null) {
            w += checkIcon.getIconWidth() + defaultTextIconGap;
            alto = Math.max(alto, checkIcon.getIconHeight());
        }
        if (icon != null) {
            w += icon.getIconWidth() + defaultTextIconGap;
            alto = Math.max(alto, icon.getIconHeight());
        }
        w += anchoTexto;
        if (hayTexto) {
            w += defaultTextIconGap;
        }
        if (!acceleratorText.equals("")) {
            w += fmAccel.stringWidth(acceleratorText) + defaultTextIconGap;
            alto = Math.max(alto, fmAccel.getHeight());
        }
        if (conColumnas && arrowIcon != null) {
            w += arrowIcon.getIconWidth() + defaultTextIconGap;
            alto = Math.max(alto, arrowIcon.getIconHeight());
        }
        // El respiro del final.
        w += defaultTextIconGap;

        int h = alto + insets.top + insets.bottom;

        // Ver la nota de la clase: par se vuelve impar.
        if (w % 2 == 0) {
            w++;
        }
        if (h % 2 == 0) {
            h++;
        }
        return new Dimension(w, h);
    }

    /** Rellena el fondo --con el color de seleccion si esta elegido-- y despues pinta el item. */
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }

    public void paint(Graphics g, JComponent c) {
        paintMenuItem(g, c, checkIcon, arrowIcon, selectionBackground, selectionForeground,
                defaultTextIconGap);
    }

    /**
     * El item entero: fondo, tilde, icono, texto, acelerador y flecha.
     *
     * <p>Las cinco piezas se ubican con la misma cuenta que {@link #getPreferredMenuItemSize}, en
     * el mismo orden.
     */
    protected void paintMenuItem(Graphics g, JComponent c, Icon checkIcon, Icon arrowIcon,
            Color background, Color foreground, int defaultTextIconGap) {
        JMenuItem b = (JMenuItem) c;
        ButtonModel model = b.getModel();
        int menuWidth = b.getWidth();
        int menuHeight = b.getHeight();
        Insets i = c.getInsets();

        Color viejoColor = g.getColor();
        Font viejaFuente = g.getFont();
        g.setFont(b.getFont());

        paintBackground(g, b, background);

        Rectangle viewRect = new Rectangle(i.left, i.top,
                menuWidth - (i.right + i.left), menuHeight - (i.bottom + i.top));
        boolean conColumnas = useCheckAndArrow();
        int x = viewRect.x;

        if (conColumnas && checkIcon != null) {
            checkIcon.paintIcon(c, g,
                    x, viewRect.y + (viewRect.height - checkIcon.getIconHeight()) / 2);
            x += checkIcon.getIconWidth() + defaultTextIconGap;
        }
        Icon icon = b.getIcon();
        if (!model.isEnabled()) {
            icon = b.getDisabledIcon();
        } else if (model.isPressed() && model.isArmed()) {
            Icon apretado = b.getPressedIcon();
            if (apretado != null) {
                icon = apretado;
            }
        }
        if (icon != null) {
            icon.paintIcon(c, g, x, viewRect.y + (viewRect.height - icon.getIconHeight()) / 2);
            x += icon.getIconWidth() + defaultTextIconGap;
        }

        String text = b.getText();
        if (text != null && !text.equals("")) {
            FontMetrics fm = b.getFontMetrics(b.getFont());
            Rectangle textRect = new Rectangle(x,
                    viewRect.y + (viewRect.height - fm.getHeight()) / 2,
                    fm.stringWidth(text), fm.getHeight());
            paintText(g, b, textRect, text);
        }

        String acceleratorText = textoDelAcelerador();
        if (!acceleratorText.equals("")) {
            FontMetrics fmAccel = b.getFontMetrics(acceleratorFont);
            int ancho = fmAccel.stringWidth(acceleratorText);
            int ax = viewRect.x + viewRect.width - ancho;
            if (conColumnas && arrowIcon != null) {
                ax -= arrowIcon.getIconWidth() + defaultTextIconGap;
            }
            g.setFont(acceleratorFont);
            g.setColor(model.isArmed() || (b instanceof JMenu && model.isSelected())
                    ? acceleratorSelectionForeground : acceleratorForeground);
            g.drawString(acceleratorText, ax,
                    viewRect.y + (viewRect.height - fmAccel.getHeight()) / 2
                            + fmAccel.getAscent());
        }

        if (conColumnas && arrowIcon != null) {
            arrowIcon.paintIcon(c, g,
                    viewRect.x + viewRect.width - arrowIcon.getIconWidth(),
                    viewRect.y + (viewRect.height - arrowIcon.getIconHeight()) / 2);
        }

        g.setColor(viejoColor);
        g.setFont(viejaFuente);
    }

    /**
     * El fondo: el color de seleccion si el item esta apuntado, y el suyo si no.
     *
     * <p>Un item transparente no pinta nada salvo que este apuntado: ahi si, porque la barra que
     * marca donde esta el mouse tiene que verse aunque el menu de atras sea transparente.
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        ButtonModel model = menuItem.getModel();
        Color viejo = g.getColor();
        int menuWidth = menuItem.getWidth();
        int menuHeight = menuItem.getHeight();

        if (menuItem.isOpaque()) {
            if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
                g.setColor(bgColor);
            } else {
                g.setColor(menuItem.getBackground());
            }
            g.fillRect(0, 0, menuWidth, menuHeight);
        } else if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
            g.setColor(bgColor);
            g.fillRect(0, 0, menuWidth, menuHeight);
        }
        g.setColor(viejo);
    }

    /** El texto, con el color que corresponda al estado. */
    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
        ButtonModel model = menuItem.getModel();
        FontMetrics fm = menuItem.getFontMetrics(menuItem.getFont());

        javax.swing.text.View v =
                (javax.swing.text.View) menuItem.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            v.paint(g, textRect);
            return;
        }
        if (!model.isEnabled()) {
            g.setColor(disabledForeground != null
                    ? disabledForeground : menuItem.getBackground().darker());
        } else if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
            g.setColor(selectionForeground);
        } else {
            g.setColor(menuItem.getForeground());
        }
        g.drawString(text, textRect.x, textRect.y + fm.getAscent());
    }

    /**
     * Aprieta el item y cierra el menu.
     *
     * <p>Primero se cierra y despues se dispara: al reves, la accion correria con el menu todavia
     * abierto, y una accion que muestre un dialogo lo dejaria colgado encima.
     */
    protected void doClick(MenuSelectionManager msm) {
        if (msm == null) {
            msm = MenuSelectionManager.defaultManager();
        }
        msm.clearSelectedPath();
        menuItem.doClick(0);
    }

    /**
     * El camino de menus hasta este item.
     *
     * <p>Vacio si no hay ningun menu abierto. Si lo hay, es el camino que ya estaba mas este item;
     * y si el camino terminaba en un hermano, se corta hasta el padre comun antes de agregarlo.
     */
    public MenuElement[] getPath() {
        MenuSelectionManager m = MenuSelectionManager.defaultManager();
        MenuElement[] oldPath = m.getSelectedPath();
        MenuElement[] newPath;
        int i = oldPath.length;
        if (i == 0) {
            return new MenuElement[0];
        }
        java.awt.Component parent = menuItem.getParent();
        if (oldPath[i - 1].getComponent() == parent) {
            newPath = new MenuElement[i + 1];
            System.arraycopy(oldPath, 0, newPath, 0, i);
            newPath[i] = menuItem;
        } else {
            int j;
            for (j = oldPath.length - 1; j >= 0; j--) {
                if (oldPath[j].getComponent() == parent) {
                    break;
                }
            }
            newPath = new MenuElement[j + 2];
            System.arraycopy(oldPath, 0, newPath, 0, j + 1);
            newPath[j + 1] = menuItem;
        }
        return newPath;
    }

    /**
     * El que escucha todo: mouse, arrastre sobre el menu y cambios de propiedad.
     *
     * <p>Uno solo en vez de tres porque los tres reaccionan a lo mismo --donde esta el mouse-- y
     * separarlos obligaria a compartir estado entre ellos.
     */
    private class Handler implements MouseInputListener, MenuDragMouseListener,
            PropertyChangeListener {

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            MenuSelectionManager.defaultManager().setSelectedPath(getPath());
        }

        public void mouseReleased(MouseEvent e) {
            if (!menuItem.isEnabled()) {
                return;
            }
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            java.awt.Point p = e.getPoint();
            if (p.x >= 0 && p.x < menuItem.getWidth()
                    && p.y >= 0 && p.y < menuItem.getHeight()) {
                doClick(manager);
            } else {
                manager.processMouseEvent(e);
            }
        }

        public void mouseEntered(MouseEvent e) {
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            int modifiers = e.getModifiersEx();
            if ((modifiers & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK
                    | MouseEvent.BUTTON3_DOWN_MASK)) != 0) {
                manager.processMouseEvent(e);
            } else {
                manager.setSelectedPath(getPath());
            }
        }

        public void mouseExited(MouseEvent e) {
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            int modifiers = e.getModifiersEx();
            if ((modifiers & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK
                    | MouseEvent.BUTTON3_DOWN_MASK)) != 0) {
                manager.processMouseEvent(e);
            } else {
                MenuElement[] path = manager.getSelectedPath();
                if (path.length > 1) {
                    MenuElement[] newPath = new MenuElement[path.length - 1];
                    System.arraycopy(path, 0, newPath, 0, path.length - 1);
                    manager.setSelectedPath(newPath);
                }
            }
        }

        public void mouseDragged(MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void menuDragMouseEntered(MenuDragMouseEvent e) {
            MenuSelectionManager manager = e.getMenuSelectionManager();
            MenuElement[] path = e.getPath();
            manager.setSelectedPath(path);
        }

        public void menuDragMouseDragged(MenuDragMouseEvent e) {
        }

        public void menuDragMouseExited(MenuDragMouseEvent e) {
        }

        public void menuDragMouseReleased(MenuDragMouseEvent e) {
            if (!menuItem.isEnabled()) {
                return;
            }
            MenuSelectionManager manager = e.getMenuSelectionManager();
            java.awt.Point p = e.getPoint();
            if (p.x >= 0 && p.x < menuItem.getWidth()
                    && p.y >= 0 && p.y < menuItem.getHeight()) {
                doClick(manager);
            } else {
                manager.clearSelectedPath();
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ("labelFor".equals(name) || "text".equals(name)) {
                BasicHTML.updateRenderer(menuItem, menuItem.getText());
            } else if ("accelerator".equals(name) || "horizontalTextPosition".equals(name)
                    || "verticalTextPosition".equals(name)) {
                menuItem.revalidate();
                menuItem.repaint();
            }
        }
    }
}
