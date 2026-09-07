package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.RootPaneUI;

/**
 * El panel que toda ventana de Swing tiene adentro.
 *
 * <h2>Cuatro piezas apiladas</h2>
 *
 * <p>De abajo hacia arriba: el <em>panel de capas</em>, que contiene todo; adentro, la <em>barra de
 * menu</em> y el <em>panel de contenido</em>, uno al lado del otro; y encima de todo, el
 * <em>vidrio</em>, transparente y que atrapa el mouse.
 *
 * <p>Es la razon de que a una ventana de Swing no se le agreguen componentes directamente sino a su
 * {@code getContentPane()}. Agregarlos a la ventana los pondria al lado del panel raiz, no adentro.
 *
 * <h2>Por que el vidrio</h2>
 *
 * <p>Un componente encima de todo, normalmente invisible, sirve para tapar la ventana mientras
 * carga, dibujar encima sin tocar nada, o atrapar el mouse durante un arrastre. Sin el, cada una de
 * esas cosas obligaria a tocar todos los componentes de abajo.
 *
 * <h2>El boton por omision</h2>
 *
 * <p>{@link #setDefaultButton} es el que responde al Enter. Vive aca y no en la ventana porque es
 * una propiedad del contenido: cambiar de panel cambia cual es el boton principal.
 */
public class JRootPane extends JComponent implements Accessible {

    private static final String uiClassID = "RootPaneUI";

    /** Sin adorno propio: lo dibuja el sistema. */
    public static final int NONE = 0;

    /** Adorno de ventana comun. */
    public static final int FRAME = 1;

    /** Adorno de dialogo. */
    public static final int PLAIN_DIALOG = 2;

    /** Adorno de dialogo de informacion. */
    public static final int INFORMATION_DIALOG = 3;

    /** Adorno de dialogo de error. */
    public static final int ERROR_DIALOG = 4;

    /** Adorno del selector de color. */
    public static final int COLOR_CHOOSER_DIALOG = 5;

    /** Adorno del selector de archivos. */
    public static final int FILE_CHOOSER_DIALOG = 6;

    /** Adorno de dialogo de pregunta. */
    public static final int QUESTION_DIALOG = 7;

    /** Adorno de dialogo de advertencia. */
    public static final int WARNING_DIALOG = 8;

    /** La barra de menu. */
    protected JMenuBar menuBar;

    /** Donde va lo que agrega el programa. */
    protected Container contentPane;

    /** El que contiene a todos los demas. */
    protected JLayeredPane layeredPane;

    /** El de arriba de todo; ver la nota de la clase. */
    protected Component glassPane;

    /** El boton que responde al Enter. */
    protected JButton defaultButton;

    private int windowDecorationStyle = NONE;
    private AccessibleContext accessibleContext;

    /** Un panel raiz con sus cuatro piezas armadas. */
    public JRootPane() {
        setGlassPane(createGlassPane());
        setLayeredPane(createLayeredPane());
        setContentPane(createContentPane());
        setLayout(createRootLayout());
        setDoubleBuffered(true);
        updateUI();
    }

    public void setDoubleBuffered(boolean aFlag) {
        super.setDoubleBuffered(aFlag);
    }

    public int getWindowDecorationStyle() {
        return windowDecorationStyle;
    }

    /**
     * Si Swing dibuja el marco de la ventana en lugar del sistema.
     *
     * @throws IllegalArgumentException si el valor no es uno de los nueve.
     */
    public void setWindowDecorationStyle(int windowDecorationStyle) {
        if (windowDecorationStyle < 0 || windowDecorationStyle > WARNING_DIALOG) {
            throw new IllegalArgumentException("Invalid decoration style");
        }
        int oldWindowDecorationStyle = getWindowDecorationStyle();
        this.windowDecorationStyle = windowDecorationStyle;
        firePropertyChange("windowDecorationStyle", oldWindowDecorationStyle,
                windowDecorationStyle);
    }

    public RootPaneUI getUI() {
        return (RootPaneUI) ui;
    }

    public void setUI(RootPaneUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** El panel de capas de siempre, con el contenido en la capa mas baja. */
    protected JLayeredPane createLayeredPane() {
        JLayeredPane p = new JLayeredPane();
        p.setName(this.getName() + ".layeredPane");
        return p;
    }

    /** El panel de contenido de siempre: opaco y con acomodador de bordes. */
    protected Container createContentPane() {
        JComponent c = new JPanel();
        c.setName(this.getName() + ".contentPane");
        c.setLayout(new java.awt.BorderLayout());
        return c;
    }

    /** El vidrio de siempre: transparente e invisible. */
    protected Component createGlassPane() {
        JComponent c = new JPanel();
        c.setName(this.getName() + ".glassPane");
        c.setVisible(false);
        ((JPanel) c).setOpaque(false);
        return c;
    }

    /** El acomodador que apila las cuatro piezas; ver la nota de la clase. */
    protected LayoutManager createRootLayout() {
        return new RootLayout(this);
    }

    /** La barra de menu; va adentro del panel de capas, arriba del contenido. */
    public void setJMenuBar(JMenuBar menu) {
        if (menuBar != null && menuBar.getParent() == layeredPane) {
            layeredPane.remove(menuBar);
        }
        menuBar = menu;
        if (menuBar != null) {
            layeredPane.add(menuBar, JLayeredPane.FRAME_CONTENT_LAYER);
        }
    }

    /**
     * La barra de menu.
     *
     * @deprecated Usar {@link #setJMenuBar}.
     */
    @Deprecated
    public void setMenuBar(JMenuBar menu) {
        setJMenuBar(menu);
    }

    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    /**
     * La barra de menu.
     *
     * @deprecated Usar {@link #getJMenuBar}.
     */
    @Deprecated
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Donde va lo que agrega el programa.
     *
     * @throws IllegalComponentStateException si es nulo.
     */
    public void setContentPane(Container content) {
        if (content == null) {
            throw new java.awt.IllegalComponentStateException(
                    "contentPane cannot be set to null.");
        }
        if (contentPane != null && contentPane.getParent() == layeredPane) {
            layeredPane.remove(contentPane);
        }
        contentPane = content;
        layeredPane.add(contentPane, JLayeredPane.FRAME_CONTENT_LAYER);
    }

    public Container getContentPane() {
        return contentPane;
    }

    /**
     * El panel de capas.
     *
     * @throws IllegalComponentStateException si es nulo.
     */
    public void setLayeredPane(JLayeredPane layered) {
        if (layered == null) {
            throw new java.awt.IllegalComponentStateException(
                    "layeredPane cannot be set to null.");
        }
        if (layeredPane != null && layeredPane.getParent() == this) {
            this.remove(layeredPane);
        }
        layeredPane = layered;
        this.add(layeredPane, -1);
    }

    public JLayeredPane getLayeredPane() {
        return layeredPane;
    }

    /**
     * El vidrio de arriba.
     *
     * <p>Se conserva si estaba visible: reemplazarlo mientras tapa la ventana no deberia destapar
     * lo de abajo.
     *
     * @throws NullPointerException si es nulo.
     */
    public void setGlassPane(Component glass) {
        if (glass == null) {
            throw new NullPointerException("glassPane cannot be set to null.");
        }
        boolean visible = false;
        if (glassPane != null && glassPane.getParent() == this) {
            this.remove(glassPane);
            visible = glassPane.isVisible();
        }
        glass.setVisible(visible);
        glassPane = glass;
        this.add(glassPane, 0);
    }

    public Component getGlassPane() {
        return glassPane;
    }

    /**
     * Siempre cierto.
     *
     * <p>Es el punto donde el reacomodo deja de subir: lo que pase adentro de una ventana no cambia
     * el tamano de la ventana. Sin este corte, escribir una letra en un campo reacomodaria todo
     * hasta la raiz.
     */
    public boolean isValidateRoot() {
        return true;
    }

    /** Falso: el vidrio esta encima del contenido, por definicion se pisan. */
    public boolean isOptimizedDrawingEnabled() {
        return !glassPane.isVisible();
    }

    public void addNotify() {
        super.addNotify();
    }

    public void removeNotify() {
        super.removeNotify();
    }

    /** El boton que responde al Enter; ver la nota de la clase. */
    public void setDefaultButton(JButton defaultButton) {
        JButton oldDefault = this.defaultButton;
        if (oldDefault != defaultButton) {
            this.defaultButton = defaultButton;
            firePropertyChange("defaultButton", oldDefault, defaultButton);
        }
    }

    public JButton getDefaultButton() {
        return defaultButton;
    }

    /**
     * Agrega un hijo; el vidrio queda siempre primero.
     *
     * <p>Primero en la lista es encima en la pantalla. Sin esta regla, poner el panel de capas
     * despues del vidrio lo taparia.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        if (glassPane != null && glassPane.getParent() == this
                && getComponent(0) != glassPane) {
            add(glassPane, 0);
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * Apila el vidrio, el panel de capas, la barra y el contenido.
     *
     * <p>No es un acomodador comun: los cuatro no van uno al lado del otro sino unos adentro de
     * otros y con la barra arriba del contenido. Ningun acomodador de los de siempre hace eso.
     */
    static class RootLayout implements LayoutManager2, Serializable {

        private final JRootPane raiz;

        RootLayout(JRootPane raiz) {
            this.raiz = raiz;
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension rd;
            Dimension mbd;
            Insets i = raiz.getInsets();
            if (raiz.contentPane != null) {
                rd = raiz.contentPane.getPreferredSize();
            } else {
                rd = parent.getSize();
            }
            if (raiz.menuBar != null && raiz.menuBar.isVisible()) {
                mbd = raiz.menuBar.getPreferredSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                    rd.height + mbd.height + i.top + i.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension rd;
            Dimension mbd;
            Insets i = raiz.getInsets();
            if (raiz.contentPane != null) {
                rd = raiz.contentPane.getMinimumSize();
            } else {
                rd = parent.getSize();
            }
            if (raiz.menuBar != null && raiz.menuBar.isVisible()) {
                mbd = raiz.menuBar.getMinimumSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                    rd.height + mbd.height + i.top + i.bottom);
        }

        public Dimension maximumLayoutSize(Container target) {
            Dimension rd;
            Dimension mbd;
            Insets i = raiz.getInsets();
            if (raiz.menuBar != null && raiz.menuBar.isVisible()) {
                mbd = raiz.menuBar.getMaximumSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            if (raiz.contentPane != null) {
                rd = raiz.contentPane.getMaximumSize();
            } else {
                rd = new Dimension(Integer.MAX_VALUE,
                        Integer.MAX_VALUE - i.top - i.bottom - mbd.height - 1);
            }
            return new Dimension(Math.min(rd.width, mbd.width) + i.left + i.right,
                    rd.height + mbd.height + i.top + i.bottom);
        }

        public void layoutContainer(Container parent) {
            Insets i = raiz.getInsets();
            int w = parent.getWidth() - i.right - i.left;
            int h = parent.getHeight() - i.top - i.bottom;

            if (raiz.layeredPane != null) {
                raiz.layeredPane.setBounds(i.left, i.top, w, h);
            }
            if (raiz.glassPane != null) {
                raiz.glassPane.setBounds(i.left, i.top, w, h);
            }
            // La barra y el contenido van adentro del panel de capas, en su propio sistema de
            // coordenadas: por eso arrancan en cero y no en el margen.
            int contentY = 0;
            if (raiz.menuBar != null && raiz.menuBar.isVisible()) {
                Dimension mbd = raiz.menuBar.getPreferredSize();
                raiz.menuBar.setBounds(0, 0, w, mbd.height);
                contentY = mbd.height;
            }
            if (raiz.contentPane != null) {
                raiz.contentPane.setBounds(0, contentY, w, h - contentY);
            }
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public void addLayoutComponent(Component comp, Object constraints) {
        }

        public float getLayoutAlignmentX(Container target) {
            return 0.0f;
        }

        public float getLayoutAlignmentY(Container target) {
            return 0.0f;
        }

        public void invalidateLayout(Container target) {
        }
    }
}
