package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.border.Border;

/**
 * La barra que separa las dos mitades de un {@link JSplitPane}.
 *
 * <h2>Es un contenedor, no un componente pintado</h2>
 *
 * <p>Hereda de {@link Container} porque puede tener hijos: los dos botoncitos de "un toque" que
 * pliegan el panel de golpe hacia un lado. Sin esa opcion prendida no tiene ninguno, y ahi si es
 * solo un rectangulo con borde.
 *
 * <h2>El arrastre en dos formas</h2>
 *
 * <p>Arrastrar el divisor puede acomodar los dos hijos en cada movimiento --"continuo"-- o dibujar
 * una sombra y acomodar recien al soltar. La segunda existe porque acomodar dos arboles de
 * componentes sesenta veces por segundo es caro; la primera porque se ve mucho mejor. Quien decide
 * es {@code JSplitPane.setContinuousLayout}, y {@link DragController} implementa las dos: mueve la
 * sombra o mueve el divisor, y avisa al UI.
 *
 * <h2>El borde se pone y no se cambia</h2>
 *
 * <p>{@link #setBorder} acepta cualquiera, pero el que importa es el que pone el UI: dibuja la linea
 * de cada lado, y de el salen los insets que hacen que el divisor tenga un pixel de aire. Medido:
 * insets (0, 1, 0, 1) en horizontal.
 *
 * <h2>Tamano</h2>
 *
 * <p>El grueso es {@link #getDividerSize} y el largo es cero: lo estira el acomodador del panel. Un
 * divisor horizontal mide 10 x 1 y uno vertical 1 x 10.
 */
public class BasicSplitPaneDivider extends Container implements PropertyChangeListener {

    /** Cuanto miden los botoncitos de un toque. */
    protected static final int ONE_TOUCH_SIZE = 6;

    /** Cuanto se corren del borde. */
    protected static final int ONE_TOUCH_OFFSET = 2;

    protected DragController dragger;
    protected BasicSplitPaneUI splitPaneUI;
    protected int dividerSize = 0;
    protected Component hiddenDivider;
    protected JSplitPane splitPane;
    protected MouseHandler mouseHandler;
    protected int orientation;
    protected JButton leftButton;
    protected JButton rightButton;

    private Border border;
    private boolean mouseOver;

    /** Para ese UI; se queda con su panel y su orientacion. */
    public BasicSplitPaneDivider(BasicSplitPaneUI ui) {
        setLayout(null);
        setBasicSplitPaneUI(ui);
        orientation = splitPane.getOrientation();
        setCursor((orientation == JSplitPane.HORIZONTAL_SPLIT)
                ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR)
                : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR));
        setBackground(ui.getSplitPane().getBackground());
    }

    /** Cambia de UI; deja de escuchar al panel anterior y empieza con el nuevo. */
    public void setBasicSplitPaneUI(BasicSplitPaneUI newUI) {
        if (splitPane != null) {
            splitPane.removePropertyChangeListener(this);
            if (mouseHandler != null) {
                splitPane.removeMouseListener(mouseHandler);
                splitPane.removeMouseMotionListener(mouseHandler);
                removeMouseListener(mouseHandler);
                removeMouseMotionListener(mouseHandler);
                mouseHandler = null;
            }
        }
        splitPaneUI = newUI;
        if (newUI != null) {
            splitPane = newUI.getSplitPane();
            if (splitPane != null) {
                if (mouseHandler == null) {
                    mouseHandler = new MouseHandler(this);
                }
                splitPane.addMouseListener(mouseHandler);
                splitPane.addMouseMotionListener(mouseHandler);
                addMouseListener(mouseHandler);
                addMouseMotionListener(mouseHandler);
                splitPane.addPropertyChangeListener(this);
                if (splitPane.isOneTouchExpandable()) {
                    oneTouchExpandableChanged();
                }
            }
        } else {
            splitPane = null;
        }
    }

    public BasicSplitPaneUI getBasicSplitPaneUI() {
        return splitPaneUI;
    }

    /** El grueso; menor que cero se toma como cero. */
    public void setDividerSize(int newSize) {
        dividerSize = newSize;
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setBorder(Border border) {
        Border oldBorder = this.border;
        this.border = border;
        firePropertyChange("border", oldBorder, border);
    }

    public Border getBorder() {
        return border;
    }

    /** Los del borde, o cero si no hay borde. */
    public Insets getInsets() {
        Border b = getBorder();
        if (b != null) {
            return b.getBorderInsets(this);
        }
        return super.getInsets();
    }

    /** Si el mouse esta encima; un aspecto puede dibujarlo distinto. */
    protected void setMouseOver(boolean mouseOver) {
        this.mouseOver = mouseOver;
    }

    public boolean isMouseOver() {
        return mouseOver;
    }

    /** El grueso a lo ancho y nada a lo largo; ver la nota de la clase. */
    public Dimension getPreferredSize() {
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            return new Dimension(getDividerSize(), 1);
        }
        return new Dimension(1, getDividerSize());
    }

    /** El mismo que el preferido: el divisor no se achica. */
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (leftButton != null) {
            leftButton.setEnabled(enabled);
        }
        if (rightButton != null) {
            rightButton.setEnabled(enabled);
        }
        setCursor(enabled
                ? ((orientation == JSplitPane.HORIZONTAL_SPLIT)
                        ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR)
                        : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR))
                : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /** Reacciona a la orientacion, al tamano y a la opcion de un toque. */
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == splitPane) {
            String nombre = e.getPropertyName();
            if (JSplitPane.ORIENTATION_PROPERTY.equals(nombre)) {
                orientation = splitPane.getOrientation();
                setCursor((orientation == JSplitPane.HORIZONTAL_SPLIT)
                        ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR)
                        : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR));
                invalidate();
                validate();
            } else if (JSplitPane.ONE_TOUCH_EXPANDABLE_PROPERTY.equals(nombre)) {
                oneTouchExpandableChanged();
            }
        }
    }

    /** El fondo, el borde, y los botoncitos si estan. */
    public void paint(Graphics g) {
        super.paint(g);
        Border b = getBorder();
        if (b != null) {
            Dimension size = getSize();
            b.paintBorder(this, g, 0, 0, size.width, size.height);
        }
    }

    /**
     * Pone los dos botoncitos de un toque la primera vez que hacen falta.
     *
     * <p>Y no los saca nunca: apagar la opcion no los quita del divisor. Parece un descuido y esta
     * medido -- con la opcion apagada el divisor sigue teniendo dos hijos --, y tiene su logica:
     * volver a prenderla no cuesta nada, y los botones no se dibujan si el acomodador no les da
     * lugar.
     */
    protected void oneTouchExpandableChanged() {
        if (splitPane.isOneTouchExpandable() && leftButton == null && rightButton == null) {
            leftButton = createLeftOneTouchButton();
            rightButton = createRightOneTouchButton();
            if (leftButton != null && rightButton != null) {
                add(leftButton);
                add(rightButton);
            }
        }
        invalidate();
        validate();
    }

    /** El botoncito que pliega hacia arriba o hacia la izquierda. */
    protected JButton createLeftOneTouchButton() {
        return new BotonDeUnToque(this, true);
    }

    /** Y el que pliega hacia el otro lado. */
    protected JButton createRightOneTouchButton() {
        return new BotonDeUnToque(this, false);
    }

    /** Avisa al UI que empieza el arrastre. */
    protected void prepareForDragging() {
        splitPaneUI.startDragging();
    }

    /** Y que va por ahi. */
    protected void dragDividerTo(int location) {
        splitPaneUI.dragDividerTo(location);
    }

    /** Y que termino. */
    protected void finishDraggingTo(int location) {
        splitPaneUI.finishDraggingTo(location);
    }

    /**
     * El que sigue un arrastre horizontal.
     *
     * <p>Guarda cuanto hay entre donde se apreto y donde empieza el divisor, para que el divisor no
     * salte al agarrarlo del medio. Y guarda los topes: hasta donde se puede llevar sin achicar un
     * hijo por debajo de su minimo.
     *
     * <p>Es estatica y toma el divisor como primer parametro. Esa es la firma que el JDK genera
     * para una clase interna, y es la unica que compila aca; ver el hallazgo #518.
     */
    protected static class DragController {

        final BasicSplitPaneDivider divisor;
        int initialX;
        int maxX;
        int minX;
        int offset;

        protected DragController(BasicSplitPaneDivider divisor, MouseEvent e) {
            this.divisor = divisor;
            JSplitPane splitPane = divisor.getBasicSplitPaneUI().getSplitPane();
            Component leftC = splitPane.getLeftComponent();
            Component rightC = splitPane.getRightComponent();
            initialX = divisor.getLocation().x;
            offset = e.getX();
            if (leftC == null || rightC == null || !leftC.isVisible() || !rightC.isVisible()) {
                maxX = -1;
                return;
            }
            Insets insets = splitPane.getInsets();
            minX = (insets != null) ? insets.left : 0;
            minX += leftC.getMinimumSize().width;
            maxX = splitPane.getWidth() - ((insets != null) ? insets.right : 0)
                    - rightC.getMinimumSize().width - divisor.getDividerSize();
            if (maxX < minX) {
                minX = 0;
                maxX = splitPane.getWidth() - divisor.getDividerSize();
            }
        }

        /** Si el arrastre tiene sentido; si no, se ignora. */
        protected boolean isValid() {
            return maxX > 0;
        }

        /** A que posicion corresponde ese evento. */
        protected int positionForMouseEvent(MouseEvent e) {
            int newX = (e.getSource() == divisor) ? (e.getX() + divisor.getLocation().x) : e.getX();
            return Math.min(maxX, Math.max(minX, newX - offset));
        }

        /** Lo mismo con coordenadas sueltas. */
        protected int getNeededLocation(int x, int y) {
            return Math.min(maxX, Math.max(minX, x - offset));
        }

        protected void continueDrag(int newX, int newY) {
            divisor.dragDividerTo(getNeededLocation(newX, newY));
        }

        protected void continueDrag(MouseEvent e) {
            divisor.dragDividerTo(positionForMouseEvent(e));
        }

        protected void completeDrag(int x, int y) {
            divisor.finishDraggingTo(getNeededLocation(x, y));
        }

        protected void completeDrag(MouseEvent e) {
            divisor.finishDraggingTo(positionForMouseEvent(e));
        }
    }

    /** Lo mismo para el otro eje; ver {@link DragController}. */
    protected static class VerticalDragController extends DragController {

        protected VerticalDragController(BasicSplitPaneDivider divisor, MouseEvent e) {
            super(divisor, e);
            JSplitPane splitPane = divisor.getBasicSplitPaneUI().getSplitPane();
            Component leftC = splitPane.getLeftComponent();
            Component rightC = splitPane.getRightComponent();
            initialX = divisor.getLocation().y;
            offset = e.getY();
            if (leftC == null || rightC == null || !leftC.isVisible() || !rightC.isVisible()) {
                maxX = -1;
                return;
            }
            Insets insets = splitPane.getInsets();
            minX = (insets != null) ? insets.top : 0;
            minX += leftC.getMinimumSize().height;
            maxX = splitPane.getHeight() - ((insets != null) ? insets.bottom : 0)
                    - rightC.getMinimumSize().height - divisor.getDividerSize();
            if (maxX < minX) {
                minX = 0;
                maxX = splitPane.getHeight() - divisor.getDividerSize();
            }
        }

        protected int getNeededLocation(int x, int y) {
            return Math.min(maxX, Math.max(minX, y - offset));
        }

        protected int positionForMouseEvent(MouseEvent e) {
            int newY = (e.getSource() == divisor) ? (e.getY() + divisor.getLocation().y) : e.getY();
            return Math.min(maxX, Math.max(minX, newY - offset));
        }
    }

    /** El que traduce los eventos del mouse en arrastre; ver la nota de la clase. */
    protected static class MouseHandler extends MouseAdapter implements MouseMotionListener {

        private final BasicSplitPaneDivider divisor;

        protected MouseHandler(BasicSplitPaneDivider divisor) {
            this.divisor = divisor;
        }

        public void mousePressed(MouseEvent e) {
            JSplitPane splitPane = divisor.splitPane;
            if ((e.getSource() != divisor && e.getSource() != splitPane)
                    || divisor.dragger != null || splitPane == null || !splitPane.isEnabled()) {
                return;
            }
            if (splitPane.getLeftComponent() == null || splitPane.getRightComponent() == null) {
                return;
            }
            DragController d = (divisor.orientation == JSplitPane.HORIZONTAL_SPLIT)
                    ? new DragController(divisor, e)
                    : new VerticalDragController(divisor, e);
            if (!d.isValid()) {
                return;
            }
            divisor.dragger = d;
            divisor.prepareForDragging();
            d.continueDrag(e);
        }

        public void mouseReleased(MouseEvent e) {
            DragController d = divisor.dragger;
            if (d == null) {
                return;
            }
            if (e.getSource() == divisor.splitPane) {
                d.completeDrag(e.getX(), e.getY());
            } else if (e.getSource() == divisor) {
                java.awt.Point ourLoc = divisor.getLocation();
                d.completeDrag(e.getX() + ourLoc.x, e.getY() + ourLoc.y);
            }
            divisor.dragger = null;
        }

        public void mouseDragged(MouseEvent e) {
            DragController d = divisor.dragger;
            if (d == null) {
                return;
            }
            if (e.getSource() == divisor.splitPane) {
                d.continueDrag(e.getX(), e.getY());
            } else if (e.getSource() == divisor) {
                java.awt.Point ourLoc = divisor.getLocation();
                d.continueDrag(e.getX() + ourLoc.x, e.getY() + ourLoc.y);
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            if (e.getSource() == divisor) {
                divisor.setMouseOver(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            if (e.getSource() == divisor) {
                divisor.setMouseOver(false);
            }
        }
    }

    /**
     * Un botoncito de un toque.
     *
     * <p>No pinta borde ni fondo: lo unico que se ve es la flechita, y la dibuja el aspecto. El
     * basico no dibuja ninguna, y esta dicho: sin ella el boton es un cuadradito invisible que
     * igual funciona.
     */
    private static class BotonDeUnToque extends JButton {

        private final BasicSplitPaneDivider divisor;
        private final boolean haciaElPrincipio;

        BotonDeUnToque(BasicSplitPaneDivider divisor, boolean haciaElPrincipio) {
            this.divisor = divisor;
            this.haciaElPrincipio = haciaElPrincipio;
            setMinimumSize(new Dimension(ONE_TOUCH_SIZE, ONE_TOUCH_SIZE));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
            setFocusPainted(false);
            setBorderPainted(false);
            setRequestFocusEnabled(false);
            addActionListener(new EscuchaDeUnToque(this));
        }

        void plegar() {
            JSplitPane splitPane = divisor.splitPane;
            Insets insets = splitPane.getInsets();
            if (haciaElPrincipio) {
                int borde = 0;
                if (insets != null) {
                    borde = (divisor.orientation == JSplitPane.HORIZONTAL_SPLIT)
                            ? insets.left : insets.top;
                }
                splitPane.setDividerLocation(borde);
            } else {
                splitPane.setDividerLocation(
                        divisor.getBasicSplitPaneUI().getMaximumDividerLocation(splitPane));
            }
        }

        public boolean isFocusTraversable() {
            return false;
        }

        public void setBorder(Border b) {
            // Ningun borde: ver la nota de la clase.
        }
    }

    /** El disparo del botoncito; aparte por lo mismo que las otras anidadas. */
    private static class EscuchaDeUnToque implements java.awt.event.ActionListener {

        private final BotonDeUnToque boton;

        EscuchaDeUnToque(BotonDeUnToque boton) {
            this.boton = boton;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            boton.plegar();
        }
    }
}
