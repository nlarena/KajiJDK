package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;

/**
 * La barra de titulo de una ventana interna.
 *
 * <h2>Un componente, no una parte del dibujo</h2>
 *
 * <p>La barra es un {@link JComponent} de verdad, con hijos: el menu de sistema a la izquierda y los
 * tres botones a la derecha. Podria ser un rectangulo pintado por el UI de la ventana, y no lo es
 * por una razon concreta: los botones tienen que recibir clicks, y el menu de sistema tiene que
 * poder desplegarse. Un dibujo no hace ninguna de las dos cosas.
 *
 * <h2>El titulo se corta, no se achica</h2>
 *
 * <p>{@link #getTitle} devuelve el texto entero si entra y, si no, el prefijo mas largo que entre
 * seguido de tres puntos. Un titulo nulo da la cadena vacia, no {@code "null"}. Es lo unico
 * razonable: achicar la letra haria que dos ventanas del mismo escritorio tuvieran titulos de
 * distinto tamano.
 *
 * <h2>Seis acciones y siete items</h2>
 *
 * <p>Las acciones son restaurar, mover, redimensionar, minimizar, maximizar y cerrar. El menu de
 * sistema tiene esas seis mas un separador antes de cerrar -- siete elementos --, y
 * {@link #enableActions} prende y apaga cada una segun lo que la ventana permita: una ventana que no
 * se puede cerrar tiene el item de cerrar apagado, no ausente.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Los cuatro iconos de los botones --maximizar, restaurar, minimizar, cerrar-- vienen de la tabla
 * del aspecto, que esta biblioteca no tiene. Los botones estan y andan; lo que no hay es el dibujo
 * adentro, y por eso la barra mide menos de ancho que la del JDK.
 *
 * <p>Mover y redimensionar con el teclado necesitan un bucle de eventos: las acciones existen y no
 * hacen nada.
 */
public class BasicInternalFrameTitlePane extends JComponent {

    /** Los nombres de los seis comandos; se comparan por nombre en {@code actionPerformed}. */
    protected static final String CLOSE_CMD = "Close";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String ICONIFY_CMD = "Minimize";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String RESTORE_CMD = "Restore";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String MAXIMIZE_CMD = "Maximize";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String MOVE_CMD = "Move";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String SIZE_CMD = "Size";

    protected JInternalFrame frame;

    protected Color selectedTitleColor;
    protected Color selectedTextColor;
    protected Color notSelectedTitleColor;
    protected Color notSelectedTextColor;

    protected Icon maxIcon;
    protected Icon minIcon;
    protected Icon iconIcon;
    protected Icon closeIcon;

    protected PropertyChangeListener propertyChangeListener;

    protected JMenuBar menuBar;
    protected JMenu windowMenu;

    protected JButton iconButton;
    protected JButton maxButton;
    protected JButton closeButton;

    protected Action closeAction;
    protected Action maximizeAction;
    protected Action iconifyAction;
    protected Action restoreAction;
    protected Action moveAction;
    protected Action sizeAction;

    private static final ColorUIResource TITULO_ELEGIDO = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource TEXTO = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource TITULO_NO_ELEGIDO = new ColorUIResource(238, 238, 238);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    /** Para esa ventana; arma acciones, botones y menu de sistema. */
    public BasicInternalFrameTitlePane(JInternalFrame f) {
        this.frame = f;
        installTitlePane();
    }

    /** El orden importa: las acciones primero, porque los botones las usan. */
    protected void installTitlePane() {
        installDefaults();
        installListeners();
        createActions();
        enableActions();
        createButtons();
        setLayout(createLayout());
        assembleSystemMenu();
        addSubComponents();
    }

    /** Colores y fuente; los valores son los de {@code InternalFrame.*} en Metal. */
    protected void installDefaults() {
        selectedTitleColor = TITULO_ELEGIDO;
        selectedTextColor = TEXTO;
        notSelectedTitleColor = TITULO_NO_ELEGIDO;
        notSelectedTextColor = TEXTO;
        Font fuente = getFont();
        if (fuente == null || fuente instanceof UIResource) {
            setFont(FUENTE);
        }
        setOpaque(false);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        if (propertyChangeListener == null) {
            propertyChangeListener = createPropertyChangeListener();
        }
        frame.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        frame.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    /** Las seis; ver la nota de la clase. */
    protected void createActions() {
        maximizeAction = new AccionDeVentana(this, MAXIMIZE_CMD);
        iconifyAction = new AccionDeVentana(this, ICONIFY_CMD);
        closeAction = new AccionDeVentana(this, CLOSE_CMD);
        restoreAction = new AccionDeVentana(this, RESTORE_CMD);
        moveAction = new AccionDeVentana(this, MOVE_CMD);
        sizeAction = new AccionDeVentana(this, SIZE_CMD);
    }

    /** Prende y apaga cada accion segun lo que la ventana permita; ver la nota de la clase. */
    protected void enableActions() {
        restoreAction.setEnabled(frame.isMaximum() || frame.isIcon());
        maximizeAction.setEnabled((frame.isMaximizable() && !frame.isMaximum() && !frame.isIcon())
                || (frame.isMaximizable() && frame.isIcon()));
        iconifyAction.setEnabled(frame.isIconifiable() && !frame.isIcon());
        closeAction.setEnabled(frame.isClosable());
        sizeAction.setEnabled(false);
        moveAction.setEnabled(false);
    }

    /** El de sistema; el que se despliega con el icono de la izquierda. */
    protected JMenu createSystemMenu() {
        JMenu menu = new JMenu("    ");
        menu.setName("InternalFrameTitlePane.menuButton");
        return menu;
    }

    protected JMenuBar createSystemMenuBar() {
        menuBar = new SystemMenuBar(this);
        menuBar.setBorderPainted(false);
        return menuBar;
    }

    /** Arma el menu y le cuelga los items. */
    protected void assembleSystemMenu() {
        menuBar = createSystemMenuBar();
        windowMenu = createSystemMenu();
        menuBar.add(windowMenu);
        addSystemMenuItems(windowMenu);
        enableActions();
    }

    /** Los siete elementos; ver la nota de la clase. */
    protected void addSystemMenuItems(JMenu systemMenu) {
        JMenuItem mi = systemMenu.add(restoreAction);
        mi.setMnemonic('R');
        mi = systemMenu.add(moveAction);
        mi.setMnemonic('M');
        mi = systemMenu.add(sizeAction);
        mi.setMnemonic('S');
        mi = systemMenu.add(iconifyAction);
        mi.setMnemonic('n');
        mi = systemMenu.add(maximizeAction);
        mi.setMnemonic('x');
        systemMenu.add(new javax.swing.JSeparator());
        mi = systemMenu.add(closeAction);
        mi.setMnemonic('C');
    }

    /** Despliega el menu de sistema. */
    protected void showSystemMenu() {
        if (windowMenu != null) {
            windowMenu.doClick();
        }
    }

    /** Los tres botones de la derecha. */
    protected void createButtons() {
        iconButton = new JButton();
        iconButton.setName("InternalFrameTitlePane.iconifyButton");
        iconButton.setFocusPainted(false);
        iconButton.setOpaque(false);
        iconButton.addActionListener(new DisparoDeAccion(iconifyAction));

        maxButton = new JButton();
        maxButton.setName("InternalFrameTitlePane.maximizeButton");
        maxButton.setFocusPainted(false);
        maxButton.setOpaque(false);
        maxButton.addActionListener(new DisparoDeAccion(maximizeAction));

        closeButton = new JButton();
        closeButton.setName("InternalFrameTitlePane.closeButton");
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(false);
        closeButton.addActionListener(new DisparoDeAccion(closeAction));

        setButtonIcons();
    }

    /**
     * Les pone a los botones el icono que corresponda al estado.
     *
     * <p>El de maximizar cambia por el de restaurar cuando la ventana ya esta maximizada, y el de
     * minimizar por el de restaurar cuando esta hecha icono. Sin iconos --ver la nota de la clase--
     * no hay nada que poner, y los botones quedan vacios.
     */
    protected void setButtonIcons() {
        if (frame.isIcon()) {
            if (iconButton != null) {
                iconButton.setIcon(minIcon);
            }
            if (maxButton != null) {
                maxButton.setIcon(maxIcon);
            }
        } else if (frame.isMaximum()) {
            if (iconButton != null) {
                iconButton.setIcon(iconIcon);
            }
            if (maxButton != null) {
                maxButton.setIcon(minIcon);
            }
        } else {
            if (iconButton != null) {
                iconButton.setIcon(iconIcon);
            }
            if (maxButton != null) {
                maxButton.setIcon(maxIcon);
            }
        }
        if (closeButton != null) {
            closeButton.setIcon(closeIcon);
        }
    }

    /** Cuelga el menu y los botones. */
    protected void addSubComponents() {
        add(menuBar);
        add(iconButton);
        add(maxButton);
        add(closeButton);
    }

    protected LayoutManager createLayout() {
        return new Handler(this);
    }

    /** El fondo de la barra: el color que corresponda a si la ventana esta elegida. */
    protected void paintTitleBackground(Graphics g) {
        Color color = frame.isSelected() ? selectedTitleColor : notSelectedTitleColor;
        g.setColor(color);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** El fondo y el titulo. */
    public void paintComponent(Graphics g) {
        paintTitleBackground(g);
        String title = frame.getTitle();
        if (title == null) {
            return;
        }
        Font f = getFont();
        g.setFont(f);
        FontMetrics fm = getFontMetrics(f);
        g.setColor(frame.isSelected() ? selectedTextColor : notSelectedTextColor);
        int baseline = (getHeight() + fm.getAscent() - fm.getLeading() - fm.getDescent()) / 2;
        int titleX = (menuBar == null) ? 2 : menuBar.getX() + menuBar.getWidth() + 2;
        int ancho = anchoDisponibleParaTitulo();
        g.drawString(getTitle(title, fm, ancho), titleX, baseline);
    }

    private int anchoDisponibleParaTitulo() {
        int usado = 0;
        if (menuBar != null) {
            usado += menuBar.getWidth();
        }
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JButton) {
                usado += c.getWidth();
            }
        }
        return Math.max(0, getWidth() - usado - 4);
    }

    /** El titulo entero si entra, y si no cortado con puntos; ver la nota de la clase. */
    protected String getTitle(String text, FontMetrics fm, int availTextWidth) {
        if (text == null || text.equals("")) {
            return "";
        }
        int textWidth = fm.stringWidth(text);
        if (textWidth <= availTextWidth) {
            return text;
        }
        String clipString = "...";
        int totalWidth = fm.stringWidth(clipString);
        int nChars;
        for (nChars = 0; nChars < text.length(); nChars++) {
            totalWidth += fm.charWidth(text.charAt(nChars));
            if (totalWidth > availTextWidth) {
                break;
            }
        }
        return text.substring(0, nChars) + clipString;
    }

    /**
     * Le manda a la ventana el aviso de que se esta cerrando.
     *
     * <p>Va como evento y no como llamada directa porque el programa puede vetarlo: quien escucha
     * {@code internalFrameClosing} tiene la oportunidad de preguntar "queres guardar?" antes.
     */
    protected void postClosingEvent(JInternalFrame frame) {
        InternalFrameEvent e = new InternalFrameEvent(frame,
                InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
        } catch (Exception ex) {
            // Sin cola de eventos --headless-- el aviso se pierde; cerrar sigue andando.
        }
    }

    /**
     * La barra de menu que contiene el menu de sistema.
     *
     * <p>Es una clase aparte y no un {@code JMenuBar} pelado porque tiene que no pintar borde y no
     * llevarse el foco: en una barra de titulo, el menu de sistema es un icono, no una barra.
     */
    static class SystemMenuBar extends JMenuBar {

        private final BasicInternalFrameTitlePane barra;

        SystemMenuBar(BasicInternalFrameTitlePane barra) {
            this.barra = barra;
        }

        public boolean isFocusTraversable() {
            return false;
        }

        public void requestFocus() {
        }

        public boolean isOpaque() {
            return true;
        }
    }

    /** Cada uno de los seis comandos; el nombre dice cual. */
    private static class AccionDeVentana extends AbstractAction {

        private final BasicInternalFrameTitlePane barra;
        private final String comando;

        AccionDeVentana(BasicInternalFrameTitlePane barra, String comando) {
            super(comando);
            this.barra = barra;
            this.comando = comando;
        }

        public void actionPerformed(ActionEvent e) {
            JInternalFrame frame = barra.frame;
            try {
                if (CLOSE_CMD.equals(comando)) {
                    if (frame.isClosable()) {
                        barra.postClosingEvent(frame);
                        frame.doDefaultCloseAction();
                    }
                } else if (ICONIFY_CMD.equals(comando)) {
                    if (frame.isIconifiable() && !frame.isIcon()) {
                        frame.setIcon(true);
                    }
                } else if (MAXIMIZE_CMD.equals(comando)) {
                    if (frame.isMaximizable()) {
                        if (frame.isIcon()) {
                            frame.setIcon(false);
                        }
                        if (!frame.isMaximum()) {
                            frame.setMaximum(true);
                        }
                    }
                } else if (RESTORE_CMD.equals(comando)) {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    } else if (frame.isMaximum()) {
                        frame.setMaximum(false);
                    }
                }
                // Mover y redimensionar: ver la nota de la clase.
            } catch (PropertyVetoException ex) {
                // Alguien dijo que no. Es una respuesta valida, no un error.
            }
        }
    }

    /** El puente entre un boton y su accion. */
    private static class DisparoDeAccion implements java.awt.event.ActionListener {

        private final Action accion;

        DisparoDeAccion(Action accion) {
            this.accion = accion;
        }

        public void actionPerformed(ActionEvent e) {
            if (accion.isEnabled()) {
                accion.actionPerformed(e);
            }
        }
    }

    /**
     * El acomodador y el escucha de propiedades.
     *
     * <p>El menu a la izquierda, los tres botones a la derecha en orden inverso, y el titulo se
     * queda con lo que sobra. No hay acomodador de los que vienen hechos que haga eso: el titulo no
     * es un componente, es lo que se pinta en el hueco.
     */
    private static class Handler implements LayoutManager, PropertyChangeListener {

        private final BasicInternalFrameTitlePane barra;

        Handler(BasicInternalFrameTitlePane barra) {
            this.barra = barra;
        }

        public void addLayoutComponent(String name, Component c) {
        }

        public void removeLayoutComponent(Component c) {
        }

        public Dimension preferredLayoutSize(Container c) {
            return minimumLayoutSize(c);
        }

        public Dimension minimumLayoutSize(Container c) {
            int alto = 0;
            int ancho = 0;
            if (barra.menuBar != null) {
                Dimension d = barra.menuBar.getPreferredSize();
                ancho += d.width;
                alto = Math.max(alto, d.height);
            }
            JButton[] botones = {barra.iconButton, barra.maxButton, barra.closeButton};
            for (int i = 0; i < botones.length; i++) {
                if (botones[i] != null) {
                    Dimension d = botones[i].getPreferredSize();
                    ancho += d.width;
                    alto = Math.max(alto, d.height);
                }
            }
            FontMetrics fm = barra.getFontMetrics(barra.getFont());
            alto = Math.max(alto, fm.getHeight());
            // El hueco del titulo: lo que necesite el texto, con un tope razonable.
            String t = barra.frame.getTitle();
            if (t != null) {
                ancho += Math.min(fm.stringWidth(t), 100);
            }
            Insets in = barra.getInsets();
            return new Dimension(ancho + in.left + in.right, alto + in.top + in.bottom);
        }

        public void layoutContainer(Container c) {
            Insets in = barra.getInsets();
            int w = barra.getWidth() - in.left - in.right;
            int h = barra.getHeight() - in.top - in.bottom;
            int x = in.left;
            if (barra.menuBar != null) {
                int mw = barra.menuBar.getPreferredSize().width;
                barra.menuBar.setBounds(x, in.top, mw, h);
                x += mw;
            }
            int derecha = in.left + w;
            JButton[] botones = {barra.closeButton, barra.maxButton, barra.iconButton};
            for (int i = 0; i < botones.length; i++) {
                if (botones[i] == null) {
                    continue;
                }
                int bw = botones[i].getPreferredSize().width;
                derecha -= bw;
                botones[i].setBounds(derecha, in.top, bw, h);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (JInternalFrame.IS_SELECTED_PROPERTY.equals(prop)
                    || JInternalFrame.IS_MAXIMUM_PROPERTY.equals(prop)
                    || JInternalFrame.IS_ICON_PROPERTY.equals(prop)
                    || JInternalFrame.IS_CLOSED_PROPERTY.equals(prop)
                    || JInternalFrame.TITLE_PROPERTY.equals(prop)) {
                barra.enableActions();
                barra.setButtonIcons();
                barra.revalidate();
                barra.repaint();
            }
        }
    }
}
