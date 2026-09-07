package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un panel dividido.
 *
 * <h2>Tres componentes y un acomodador con memoria</h2>
 *
 * <p>El panel tiene dos hijos y un divisor en el medio, y el reparto no se recalcula de cero cada
 * vez: el acomodador se acuerda de cuanto le dio a cada uno --{@link BasicHorizontalLayoutManager}
 * y su tabla {@code sizes}-- y al cambiar el tamano del panel reparte la diferencia. Sin esa
 * memoria, agrandar la ventana devolveria el divisor al medio y se perderia donde lo dejo el
 * usuario.
 *
 * <h2>Arrastre continuo o con sombra</h2>
 *
 * <p>Ver la nota de {@link BasicSplitPaneDivider}. Aca esta la otra mitad: el "divisor no continuo"
 * ({@link #getNonContinuousLayoutDivider}) es el rectangulo que se dibuja mientras se arrastra y que
 * desaparece al soltar. Se agrega a la capa de arriba del panel para que tape a los dos hijos.
 *
 * <h2>Los doce campos que quedaron en nulo</h2>
 *
 * <p>Siete {@link KeyStroke} y cinco {@link ActionListener}, todos protegidos, todos en
 * {@code null}. Son de cuando el UI ataba las teclas a mano; ahora las ata la tabla del aspecto.
 * Esta medido, y es la misma historia que {@code shadow} y {@code highlight} en
 * {@link BasicSeparatorUI}.
 *
 * <h2>Sin insets</h2>
 *
 * <p>{@link #getInsets} devuelve {@code null}, no un {@code Insets} en cero: deja que conteste el
 * borde del componente. Medido.
 */
public class BasicSplitPaneUI extends SplitPaneUI {

    /** El nombre con el que se agrega el divisor de arrastre a la capa de arriba. */
    protected static final String NON_CONTINUOUS_DIVIDER = "nonContinuousDivider";

    /** Cuanto se mueve el divisor con una flecha del teclado. */
    protected static int KEYBOARD_DIVIDER_MOVE_OFFSET = 3;

    protected JSplitPane splitPane;
    protected BasicHorizontalLayoutManager layoutManager;
    protected BasicSplitPaneDivider divider;
    protected PropertyChangeListener propertyChangeListener;
    protected FocusListener focusListener;
    protected int dividerSize;
    protected Component nonContinuousLayoutDivider;
    protected boolean draggingHW;
    protected int beginDragDividerLocation;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke upKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke downKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke leftKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke rightKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke homeKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke endKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke dividerResizeToggleKey;

    /** Sin uso; ver la nota de la clase. */
    protected ActionListener keyboardUpLeftListener;

    /** Sin uso; ver la nota de la clase. */
    protected ActionListener keyboardDownRightListener;

    /** Sin uso; ver la nota de la clase. */
    protected ActionListener keyboardHomeListener;

    /** Sin uso; ver la nota de la clase. */
    protected ActionListener keyboardEndListener;

    /** Sin uso; ver la nota de la clase. */
    protected ActionListener keyboardResizeToggleListener;

    private int lastDragLocation = -1;
    private boolean continuousLayout;
    private int orientation;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);

    public BasicSplitPaneUI() {
    }

    /** Uno nuevo por panel: guarda el panel, su divisor y el reparto. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicSplitPaneUI();
    }

    public void installUI(JComponent c) {
        splitPane = (JSplitPane) c;
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();
        splitPane = null;
    }

    /** Colores, borde, divisor y acomodador; los valores son los de {@code SplitPane.*} en Metal. */
    protected void installDefaults() {
        orientation = splitPane.getOrientation();
        continuousLayout = splitPane.isContinuousLayout();
        resetLayoutManager();

        if (divider == null) {
            divider = createDefaultDivider();
        }
        divider.setBasicSplitPaneUI(this);

        Color fondo = splitPane.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            splitPane.setBackground(FONDO);
        }
        Border b = splitPane.getBorder();
        if (b == null || b instanceof UIResource) {
            splitPane.setBorder(BasicBorders.getSplitPaneBorder());
        }
        Border db = divider.getBorder();
        if (db == null || db instanceof UIResource) {
            divider.setBorder(BasicBorders.getSplitPaneDividerBorder());
        }
        LookAndFeel.installProperty(splitPane, "opaque", Boolean.TRUE);

        dividerSize = splitPane.getDividerSize();
        if (dividerSize == 0) {
            dividerSize = 10;
            splitPane.setDividerSize(dividerSize);
        }
        divider.setDividerSize(splitPane.getDividerSize());
        dividerSize = divider.getDividerSize();
        splitPane.add(divider, JSplitPane.DIVIDER);

        setNonContinuousLayoutDivider(createDefaultNonContinuousLayoutDivider(), true);
    }

    /** Saca el divisor y el borde que puso este UI. */
    protected void uninstallDefaults() {
        if (splitPane.getLayout() == layoutManager) {
            splitPane.setLayout(null);
        }
        if (nonContinuousLayoutDivider != null) {
            splitPane.remove(nonContinuousLayoutDivider);
        }
        LookAndFeel.uninstallBorder(splitPane);
        if (divider != null) {
            splitPane.remove(divider);
            divider.setBasicSplitPaneUI(null);
        }
        layoutManager = null;
        divider = null;
        nonContinuousLayoutDivider = null;
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        splitPane.addPropertyChangeListener(propertyChangeListener);
        focusListener = createFocusListener();
        splitPane.addFocusListener(focusListener);
    }

    protected void uninstallListeners() {
        splitPane.removePropertyChangeListener(propertyChangeListener);
        splitPane.removeFocusListener(focusListener);
        propertyChangeListener = null;
        focusListener = null;
    }

    /** Sin atajos propios; ver la nota de la clase sobre los campos en nulo. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    protected FocusListener createFocusListener() {
        return new Handler();
    }

    /** Ninguno; ver la nota de la clase. */
    protected ActionListener createKeyboardUpLeftListener() {
        return null;
    }

    /** Ninguno; ver la nota de la clase. */
    protected ActionListener createKeyboardDownRightListener() {
        return null;
    }

    /** Ninguno; ver la nota de la clase. */
    protected ActionListener createKeyboardHomeListener() {
        return null;
    }

    /** Ninguno; ver la nota de la clase. */
    protected ActionListener createKeyboardEndListener() {
        return null;
    }

    /** Ninguno; ver la nota de la clase. */
    protected ActionListener createKeyboardResizeToggleListener() {
        return null;
    }

    public int getOrientation() {
        return orientation;
    }

    /** Cambia el eje y rehace el acomodador. */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
        resetLayoutManager();
    }

    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    public void setContinuousLayout(boolean b) {
        continuousLayout = b;
    }

    public int getLastDragLocation() {
        return lastDragLocation;
    }

    public void setLastDragLocation(int l) {
        lastDragLocation = l;
    }

    public BasicSplitPaneDivider getDivider() {
        return divider;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    /** El divisor: uno por panel. */
    public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this);
    }

    /** El rectangulo que se dibuja mientras se arrastra; ver la nota de la clase. */
    protected Component createDefaultNonContinuousLayoutDivider() {
        return new SombraDeArrastre(this);
    }

    protected void setNonContinuousLayoutDivider(Component newDivider) {
        setNonContinuousLayoutDivider(newDivider, true);
    }

    protected void setNonContinuousLayoutDivider(Component newDivider, boolean rememberSizes) {
        if (nonContinuousLayoutDivider != null && splitPane != null) {
            splitPane.remove(nonContinuousLayoutDivider);
        }
        nonContinuousLayoutDivider = newDivider;
    }

    public Component getNonContinuousLayoutDivider() {
        return nonContinuousLayoutDivider;
    }

    /** Un pixel: lo que ocupa la linea del borde del divisor. */
    protected int getDividerBorderSize() {
        return 1;
    }

    /** Vuelve a armar el acomodador que corresponde al eje. */
    protected void resetLayoutManager() {
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            layoutManager = new BasicHorizontalLayoutManager(this);
        } else {
            layoutManager = new BasicVerticalLayoutManager(this);
        }
        splitPane.setLayout(layoutManager);
        layoutManager.updateComponents();
        splitPane.revalidate();
        splitPane.repaint();
    }

    /** Vuelve a repartir segun los tamanos preferidos de los hijos. */
    public void resetToPreferredSizes(JSplitPane jc) {
        if (layoutManager != null) {
            layoutManager.resetToPreferredSizes();
            splitPane.revalidate();
        }
    }

    /** Empieza un arrastre; guarda donde estaba el divisor por si hay que volver. */
    protected void startDragging() {
        beginDragDividerLocation = getDividerLocation(splitPane);
        draggingHW = false;
        if (!isContinuousLayout() && nonContinuousLayoutDivider != null) {
            splitPane.add(nonContinuousLayoutDivider, JSplitPane.DIVIDER);
        }
    }

    /** Mueve el divisor, o la sombra si el arrastre no es continuo. */
    protected void dragDividerTo(int location) {
        setLastDragLocation(location);
        if (isContinuousLayout()) {
            splitPane.setDividerLocation(location);
        } else if (nonContinuousLayoutDivider != null) {
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                nonContinuousLayoutDivider.setLocation(location, 0);
            } else {
                nonContinuousLayoutDivider.setLocation(0, location);
            }
        }
    }

    /** Termina: saca la sombra y deja el divisor donde quedo. */
    protected void finishDraggingTo(int location) {
        dragDividerTo(location);
        setLastDragLocation(-1);
        if (!isContinuousLayout()) {
            if (nonContinuousLayoutDivider != null) {
                splitPane.remove(nonContinuousLayoutDivider);
            }
            splitPane.setDividerLocation(location);
        }
    }

    /** Donde esta el divisor, en pixeles desde el borde. */
    public int getDividerLocation(JSplitPane jc) {
        if (divider == null) {
            return 0;
        }
        return (orientation == JSplitPane.HORIZONTAL_SPLIT)
                ? divider.getLocation().x : divider.getLocation().y;
    }

    /** Y hasta donde se puede llevar sin achicar el primer hijo por debajo de su minimo. */
    public int getMinimumDividerLocation(JSplitPane jc) {
        int minLoc = 0;
        Component leftC = splitPane.getLeftComponent();
        if ((leftC != null) && (leftC.isVisible())) {
            Insets insets = splitPane.getInsets();
            Dimension minSize = leftC.getMinimumSize();
            minLoc = (orientation == JSplitPane.HORIZONTAL_SPLIT)
                    ? minSize.width : minSize.height;
            if (insets != null) {
                minLoc += (orientation == JSplitPane.HORIZONTAL_SPLIT)
                        ? insets.left : insets.top;
            }
        }
        return minLoc;
    }

    /**
     * Y del otro lado.
     *
     * <p>Nunca menos que el minimo: en un panel que todavia no tiene tamano la cuenta da negativo,
     * y un maximo por debajo del minimo no le sirve a nadie.
     */
    public int getMaximumDividerLocation(JSplitPane jc) {
        Dimension splitPaneSize = splitPane.getSize();
        int maxLoc = 0;
        Component rightC = splitPane.getRightComponent();
        if (rightC != null) {
            Insets insets = splitPane.getInsets();
            Dimension minSize = new Dimension(0, 0);
            if (rightC.isVisible()) {
                minSize = rightC.getMinimumSize();
            }
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                maxLoc = splitPaneSize.width - minSize.width;
                if (insets != null) {
                    maxLoc -= insets.right;
                }
            } else {
                maxLoc = splitPaneSize.height - minSize.height;
                if (insets != null) {
                    maxLoc -= insets.bottom;
                }
            }
            maxLoc -= dividerSize;
        }
        return Math.max(getMinimumDividerLocation(splitPane), maxLoc);
    }

    /** Pone el divisor ahi. */
    public void setDividerLocation(JSplitPane jc, int location) {
        if (layoutManager != null) {
            layoutManager.setDividerLocation(location);
            splitPane.revalidate();
            splitPane.repaint();
        }
    }

    /** {@code null}; ver la nota de la clase. */
    public Insets getInsets(JComponent jc) {
        return null;
    }

    public Dimension getPreferredSize(JComponent jc) {
        if (splitPane == null || layoutManager == null) {
            return new Dimension(0, 0);
        }
        return layoutManager.preferredLayoutSize(splitPane);
    }

    public Dimension getMinimumSize(JComponent jc) {
        if (splitPane == null || layoutManager == null) {
            return new Dimension(0, 0);
        }
        return layoutManager.minimumLayoutSize(splitPane);
    }

    /** Sin tope: un panel dividido se estira todo lo que le den. */
    public Dimension getMaximumSize(JComponent jc) {
        if (splitPane == null || layoutManager == null) {
            return new Dimension(0, 0);
        }
        return layoutManager.maximumLayoutSize(splitPane);
    }

    /** Nada: los hijos y el divisor se pintan solos. */
    public void paint(Graphics g, JComponent jc) {
    }

    /** Despues de los hijos: aca se dibujaria la sombra de arrastre por hardware. */
    public void finishedPaintingChildren(JSplitPane jc, Graphics g) {
        if (jc == splitPane && getLastDragLocation() != -1
                && !isContinuousLayout() && !draggingHW) {
            Dimension size = splitPane.getSize();
            g.setColor(Color.darkGray);
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                g.fillRect(getLastDragLocation(), 0, dividerSize - 1, size.height - 1);
            } else {
                g.fillRect(0, getLastDragLocation(), size.width - 1, dividerSize - 1);
            }
        }
    }

    /**
     * El reparto horizontal, con memoria; ver la nota de la clase.
     *
     * <p>{@link #sizes} guarda tres numeros: lo que ocupa el primer hijo, lo que ocupa el divisor y
     * lo que ocupa el segundo. Es la unica manera de que agrandar el panel no le devuelva el
     * divisor al medio.
     */
    public static class BasicHorizontalLayoutManager implements LayoutManager2 {

        /** El UI dueno; ver el hallazgo #518 sobre por que va como parametro. */
        final BasicSplitPaneUI ui;

        /** El primer hijo, el segundo y el divisor, en ese orden. */
        protected Component[] components = new Component[3];

        /** Lo que ocupa cada uno; ver la nota de la clase. */
        protected int[] sizes = new int[3];

        private final int eje;

        BasicHorizontalLayoutManager(BasicSplitPaneUI ui) {
            this(ui, JSplitPane.HORIZONTAL_SPLIT);
        }

        BasicHorizontalLayoutManager(BasicSplitPaneUI ui, int eje) {
            this.ui = ui;
            this.eje = eje;
        }

        /** Lo que mide ese componente en el eje que reparte. */
        protected int getSizeOfComponent(Component comp) {
            Dimension d = comp.getSize();
            return (eje == JSplitPane.HORIZONTAL_SPLIT) ? d.width : d.height;
        }

        protected int getPreferredSizeOfComponent(Component comp) {
            Dimension d = comp.getPreferredSize();
            return (eje == JSplitPane.HORIZONTAL_SPLIT) ? d.width : d.height;
        }

        private int getMinimumSizeOfComponent(Component comp) {
            Dimension d = comp.getMinimumSize();
            return (eje == JSplitPane.HORIZONTAL_SPLIT) ? d.width : d.height;
        }

        /** Cuanto lugar hay para repartir, sacando los margenes. */
        protected int getAvailableSize(Dimension containerSize, Insets insets) {
            if (insets == null) {
                return (eje == JSplitPane.HORIZONTAL_SPLIT)
                        ? containerSize.width : containerSize.height;
            }
            return (eje == JSplitPane.HORIZONTAL_SPLIT)
                    ? (containerSize.width - insets.left - insets.right)
                    : (containerSize.height - insets.top - insets.bottom);
        }

        /** Donde empieza el reparto. */
        protected int getInitialLocation(Insets insets) {
            if (insets == null) {
                return 0;
            }
            return (eje == JSplitPane.HORIZONTAL_SPLIT) ? insets.left : insets.top;
        }

        protected int[] getSizes() {
            int[] copia = new int[3];
            System.arraycopy(sizes, 0, copia, 0, 3);
            return copia;
        }

        protected void setSizes(int[] newSizes) {
            System.arraycopy(newSizes, 0, sizes, 0, 3);
        }

        protected void resetSizeAt(int index) {
            sizes[index] = 0;
        }

        /** Vuelve a leer que componente ocupa cada lugar. */
        protected void updateComponents() {
            Component comp = ui.splitPane.getLeftComponent();
            if (components[0] != comp) {
                components[0] = comp;
                if (comp == null) {
                    sizes[0] = 0;
                }
            }
            comp = ui.splitPane.getRightComponent();
            if (components[1] != comp) {
                components[1] = comp;
                if (comp == null) {
                    sizes[1] = 0;
                }
            }
            components[2] = ui.divider;
            if (ui.divider != null) {
                sizes[2] = ui.divider.getDividerSize();
            }
        }

        public void resetToPreferredSizes() {
            for (int i = 0; i < 2; i++) {
                sizes[i] = (components[i] != null)
                        ? getPreferredSizeOfComponent(components[i]) : 0;
            }
        }

        /** Pone el divisor en esa posicion y reparte lo que queda. */
        void setDividerLocation(int location) {
            updateComponents();
            Insets insets = ui.splitPane.getInsets();
            int inicio = getInitialLocation(insets);
            int total = getAvailableSize(ui.splitPane.getSize(), insets);
            sizes[0] = Math.max(0, location - inicio);
            sizes[2] = (ui.divider != null) ? ui.divider.getDividerSize() : 0;
            sizes[1] = Math.max(0, total - sizes[0] - sizes[2]);
        }

        public void addLayoutComponent(String place, Component component) {
            addLayoutComponent(component, place);
        }

        public void addLayoutComponent(Component comp, Object constraints) {
            updateComponents();
        }

        public void removeLayoutComponent(Component component) {
            for (int i = 0; i < 3; i++) {
                if (components[i] == component) {
                    components[i] = null;
                    sizes[i] = 0;
                }
            }
        }

        public void invalidateLayout(Container c) {
        }

        public float getLayoutAlignmentX(Container target) {
            return 0.0f;
        }

        public float getLayoutAlignmentY(Container target) {
            return 0.0f;
        }

        private Dimension medir(Container container, boolean minimo) {
            updateComponents();
            int largo = 0;
            int grueso = 0;
            for (int i = 0; i < 3; i++) {
                Component c = components[i];
                if (c == null || !c.isVisible()) {
                    continue;
                }
                Dimension d = minimo ? c.getMinimumSize() : c.getPreferredSize();
                if (eje == JSplitPane.HORIZONTAL_SPLIT) {
                    largo += d.width;
                    grueso = Math.max(grueso, d.height);
                } else {
                    largo += d.height;
                    grueso = Math.max(grueso, d.width);
                }
            }
            Insets insets = container.getInsets();
            int w;
            int h;
            if (eje == JSplitPane.HORIZONTAL_SPLIT) {
                w = largo;
                h = grueso;
            } else {
                w = grueso;
                h = largo;
            }
            if (insets != null) {
                w += insets.left + insets.right;
                h += insets.top + insets.bottom;
            }
            return new Dimension(w, h);
        }

        public Dimension preferredLayoutSize(Container container) {
            return medir(container, false);
        }

        public Dimension minimumLayoutSize(Container container) {
            return medir(container, true);
        }

        /** Sin tope. */
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        /** Le da a cada uno lo que dice la tabla, repartiendo la diferencia. */
        public void layoutContainer(Container container) {
            updateComponents();
            Insets insets = container.getInsets();
            int total = getAvailableSize(container.getSize(), insets);
            int inicio = getInitialLocation(insets);
            int divisor = (ui.divider != null) ? ui.divider.getDividerSize() : 0;
            sizes[2] = divisor;
            int ocupado = sizes[0] + sizes[1] + divisor;
            if (ocupado <= 0) {
                // Primera vez: la mitad para cada uno.
                sizes[0] = Math.max(0, (total - divisor) / 2);
                sizes[1] = Math.max(0, total - divisor - sizes[0]);
            } else if (ocupado != total) {
                // Toda la diferencia va al segundo, que es lo que hace que el divisor se quede
                // donde el usuario lo dejo.
                sizes[1] = Math.max(0, total - divisor - sizes[0]);
            }
            int pos = inicio;
            for (int i = 0; i < 3; i++) {
                int idx = (i == 0) ? 0 : ((i == 1) ? 2 : 1);
                Component c = components[idx];
                int tam = sizes[idx];
                if (c != null) {
                    setComponentToSize(c, tam, pos, insets, container.getSize());
                }
                pos += tam;
            }
        }

        /** Le da a un componente ese tamano en el eje que reparte, y todo el otro. */
        protected void setComponentToSize(Component c, int size, int location, Insets insets,
                Dimension containerSize) {
            if (insets == null) {
                insets = new Insets(0, 0, 0, 0);
            }
            if (eje == JSplitPane.HORIZONTAL_SPLIT) {
                c.setBounds(location, insets.top, size,
                        containerSize.height - insets.top - insets.bottom);
            } else {
                c.setBounds(insets.left, location,
                        containerSize.width - insets.left - insets.right, size);
            }
        }
    }

    /** Lo mismo en el otro eje; ver {@link BasicHorizontalLayoutManager}. */
    public static class BasicVerticalLayoutManager extends BasicHorizontalLayoutManager {

        public BasicVerticalLayoutManager(BasicSplitPaneUI ui) {
            super(ui, JSplitPane.VERTICAL_SPLIT);
        }
    }

    /**
     * La sombra que se ve mientras se arrastra sin acomodar; ver la nota de la clase.
     *
     * <p>Es un componente opaco de color oscuro y nada mas: lo unico que tiene que hacer es taparse
     * a si mismo y moverse.
     */
    private static class SombraDeArrastre extends java.awt.Canvas {

        private final BasicSplitPaneUI ui;

        SombraDeArrastre(BasicSplitPaneUI ui) {
            this.ui = ui;
            setBackground(Color.darkGray);
        }

        public Dimension getPreferredSize() {
            if (ui.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                return new Dimension(ui.dividerSize, 1);
            }
            return new Dimension(1, ui.dividerSize);
        }
    }

    /** Reacciona a los cambios del panel: eje, divisor, hijos, y modo de arrastre. */
    private class Handler implements PropertyChangeListener, FocusListener {

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() != splitPane) {
                return;
            }
            String nombre = e.getPropertyName();
            if (JSplitPane.ORIENTATION_PROPERTY.equals(nombre)) {
                orientation = splitPane.getOrientation();
                resetLayoutManager();
            } else if (JSplitPane.CONTINUOUS_LAYOUT_PROPERTY.equals(nombre)) {
                setContinuousLayout(splitPane.isContinuousLayout());
            } else if (JSplitPane.DIVIDER_SIZE_PROPERTY.equals(nombre)) {
                divider.setDividerSize(splitPane.getDividerSize());
                dividerSize = divider.getDividerSize();
                splitPane.revalidate();
                splitPane.repaint();
            } else if (JSplitPane.LEFT.equals(nombre) || JSplitPane.RIGHT.equals(nombre)
                    || JSplitPane.TOP.equals(nombre) || JSplitPane.BOTTOM.equals(nombre)) {
                if (layoutManager != null) {
                    layoutManager.updateComponents();
                }
                splitPane.revalidate();
                splitPane.repaint();
            } else if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(nombre)) {
                Object nuevo = e.getNewValue();
                if (nuevo instanceof Number && layoutManager != null) {
                    layoutManager.setDividerLocation(((Number) nuevo).intValue());
                    splitPane.revalidate();
                    splitPane.repaint();
                }
            }
        }

        public void focusGained(FocusEvent e) {
            if (divider != null) {
                divider.repaint();
            }
        }

        public void focusLost(FocusEvent e) {
            if (divider != null) {
                divider.repaint();
            }
        }
    }
}
