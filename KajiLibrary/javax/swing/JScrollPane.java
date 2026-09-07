package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;

import javax.accessibility.AccessibleContext;

import javax.swing.border.Border;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/**
 * Un panel que muestra un pedazo de algo grande, con barras para moverse.
 *
 * <h2>No desplaza nada</h2>
 *
 * <p>El panel no mueve el contenido: lo mueve el {@link JViewport}, y las barras solo escriben en
 * su posicion. El panel es el que junta las piezas —ventana, dos barras, dos cabeceras, cuatro
 * esquinas—, les da una distribucion y mantiene sincronizados los modelos de las barras con lo que
 * la ventana muestra. Esa sincronizacion la hace el aspecto, no esta clase.
 *
 * <p>De ahi que casi todos los metodos sean pares {@code get}/{@code set} de piezas: agregarle un
 * componente al panel es agregarselo a su ventana ({@link #setViewportView}), y lo demas se
 * acomoda solo.
 *
 * <h2>El contenido puede opinar</h2>
 *
 * <p>Si el contenido implementa {@link Scrollable}, las barras le preguntan cuanto avanzar y la
 * distribucion le pregunta si quiere seguir al tamano de la ventana. La barra que hace esas
 * preguntas es {@link ScrollBar}, la que el panel crea; una barra puesta a mano con
 * {@link #setVerticalScrollBar} no las hace.
 *
 * <p>No esta el desplazamiento por rueda: {@link #setWheelScrollingEnabled} guarda la propiedad,
 * pero sin eventos de rueda que despachar no hay a que responder.
 */
public class JScrollPane extends JComponent implements ScrollPaneConstants, Accessible {

    private static final String uiClassID = "ScrollPaneUI";

    private Border viewportBorder;

    protected int verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;

    protected int horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    protected JViewport viewport;

    protected JScrollBar verticalScrollBar;

    protected JScrollBar horizontalScrollBar;

    protected JViewport rowHeader;

    protected JViewport columnHeader;

    protected Component lowerLeft;

    protected Component lowerRight;

    protected Component upperLeft;

    protected Component upperRight;

    private boolean wheelScrollState = true;

    /** Un panel con ese contenido y esas dos politicas. */
    public JScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        setLayout(new ScrollPaneLayout$UIResource());
        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);
        setViewport(createViewport());
        setVerticalScrollBar(createVerticalScrollBar());
        setHorizontalScrollBar(createHorizontalScrollBar());
        if (view != null) {
            setViewportView(view);
        }
        setUIProperty("opaque", Boolean.TRUE);
        updateUI();

        if (!this.getComponentOrientation().isLeftToRight()) {
            viewport.setViewPosition(new Point(Integer.MAX_VALUE, 0));
        }
    }

    /** Un panel con ese contenido y las politicas "cuando haga falta". */
    public JScrollPane(Component view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public JScrollPane(int vsbPolicy, int hsbPolicy) {
        this(null, vsbPolicy, hsbPolicy);
    }

    /** Un panel vacio. */
    public JScrollPane() {
        this(null, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public ScrollPaneUI getUI() {
        return (ScrollPaneUI) ui;
    }

    public void setUI(ScrollPaneUI ui) {
        super.setUI(ui);
    }

    /** Instala el aspecto basico; ver {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ScrollPaneUI) BasicScrollPaneUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** La distribucion tiene que ser una {@link ScrollPaneLayout}, o {@code null}. */
    public void setLayout(LayoutManager layout) {
        if (layout instanceof ScrollPaneLayout) {
            super.setLayout(layout);
            ((ScrollPaneLayout) layout).syncWithScrollPane(this);
        } else if (layout == null) {
            super.setLayout(layout);
        } else {
            String s = "layout of JScrollPane must be a ScrollPaneLayout";
            throw new ClassCastException(s);
        }
    }

    /**
     * Si: el maquetado para aca.
     *
     * <p>Un panel con barras tiene tamano propio, y lo que pase adentro no tiene por que obligar a
     * recalcular la ventana entera.
     */
    public boolean isValidateRoot() {
        return true;
    }

    public int getVerticalScrollBarPolicy() {
        return verticalScrollBarPolicy;
    }

    public void setVerticalScrollBarPolicy(int policy) {
        if (policy != VERTICAL_SCROLLBAR_AS_NEEDED && policy != VERTICAL_SCROLLBAR_NEVER
                && policy != VERTICAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid verticalScrollBarPolicy");
        }
        int old = verticalScrollBarPolicy;
        verticalScrollBarPolicy = policy;
        firePropertyChange("verticalScrollBarPolicy", old, policy);
        revalidate();
        repaint();
    }

    public int getHorizontalScrollBarPolicy() {
        return horizontalScrollBarPolicy;
    }

    public void setHorizontalScrollBarPolicy(int policy) {
        if (policy != HORIZONTAL_SCROLLBAR_AS_NEEDED && policy != HORIZONTAL_SCROLLBAR_NEVER
                && policy != HORIZONTAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid horizontalScrollBarPolicy");
        }
        int old = horizontalScrollBarPolicy;
        horizontalScrollBarPolicy = policy;
        firePropertyChange("horizontalScrollBarPolicy", old, policy);
        revalidate();
        repaint();
    }

    /** El borde que se pinta alrededor de la ventana, por dentro del borde del panel. */
    public Border getViewportBorder() {
        return viewportBorder;
    }

    public void setViewportBorder(Border viewportBorder) {
        Border oldValue = this.viewportBorder;
        this.viewportBorder = viewportBorder;
        firePropertyChange("viewportBorder", oldValue, viewportBorder);
    }

    /** El rectangulo donde va ese borde: lo que queda sin cabeceras ni barras. */
    public Rectangle getViewportBorderBounds() {
        Rectangle borderR = new Rectangle(getSize());

        java.awt.Insets insets = getInsets();
        borderR.x = insets.left;
        borderR.y = insets.top;
        borderR.width = borderR.width - (insets.left + insets.right);
        borderR.height = borderR.height - (insets.top + insets.bottom);

        boolean leftToRight = getComponentOrientation().isLeftToRight();

        JViewport colHead = getColumnHeader();
        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = colHead.getHeight();
            borderR.y = borderR.y + colHeadHeight;
            borderR.height = borderR.height - colHeadHeight;
        }

        JViewport rowHead = getRowHeader();
        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = rowHead.getWidth();
            if (leftToRight) {
                borderR.x = borderR.x + rowHeadWidth;
            }
            borderR.width = borderR.width - rowHeadWidth;
        }

        JScrollBar vsb = getVerticalScrollBar();
        if ((vsb != null) && (vsb.isVisible())) {
            int vsbWidth = vsb.getWidth();
            if (!leftToRight) {
                borderR.x = borderR.x + vsbWidth;
            }
            borderR.width = borderR.width - vsbWidth;
        }

        JScrollBar hsb = getHorizontalScrollBar();
        if ((hsb != null) && (hsb.isVisible())) {
            int hsbHeight = hsb.getHeight();
            borderR.height = borderR.height - hsbHeight;
        }

        return borderR;
    }

    /** La barra que sabe preguntarle al contenido; ver {@link ScrollBar}. */
    public JScrollBar createHorizontalScrollBar() {
        return new ScrollBar(JScrollBar.HORIZONTAL);
    }

    public JScrollBar getHorizontalScrollBar() {
        return horizontalScrollBar;
    }

    public void setHorizontalScrollBar(JScrollBar horizontalScrollBar) {
        JScrollBar old = getHorizontalScrollBar();
        this.horizontalScrollBar = horizontalScrollBar;
        if (horizontalScrollBar != null) {
            add(horizontalScrollBar, HORIZONTAL_SCROLLBAR);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("horizontalScrollBar", old, horizontalScrollBar);
        revalidate();
        repaint();
    }

    public JScrollBar createVerticalScrollBar() {
        return new ScrollBar(JScrollBar.VERTICAL);
    }

    public JScrollBar getVerticalScrollBar() {
        return verticalScrollBar;
    }

    public void setVerticalScrollBar(JScrollBar verticalScrollBar) {
        JScrollBar old = getVerticalScrollBar();
        this.verticalScrollBar = verticalScrollBar;
        if (verticalScrollBar != null) {
            add(verticalScrollBar, VERTICAL_SCROLLBAR);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("verticalScrollBar", old, verticalScrollBar);
        revalidate();
        repaint();
    }

    protected JViewport createViewport() {
        return new JViewport();
    }

    public JViewport getViewport() {
        return viewport;
    }

    public void setViewport(JViewport viewport) {
        JViewport old = getViewport();
        this.viewport = viewport;
        if (viewport != null) {
            add(viewport, VIEWPORT);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("viewport", old, viewport);

        if (accessibleContextExiste()) {
            // El JDK reengancha aca el escucha de accesibilidad; sin contexto no hay nada.
        }
        revalidate();
        repaint();
    }

    private boolean accessibleContextExiste() {
        return false;
    }

    /** Poner contenido es ponerselo a la ventana; si no hay ventana, se crea una. */
    public void setViewportView(Component view) {
        if (getViewport() == null) {
            setViewport(createViewport());
        }
        getViewport().setView(view);
    }

    public JViewport getRowHeader() {
        return rowHeader;
    }

    public void setRowHeader(JViewport rowHeader) {
        JViewport old = getRowHeader();
        this.rowHeader = rowHeader;
        if (rowHeader != null) {
            add(rowHeader, ROW_HEADER);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("rowHeader", old, rowHeader);
        revalidate();
        repaint();
    }

    /** Envuelve ese componente en una ventana y lo pone de cabecera de filas. */
    public void setRowHeaderView(Component view) {
        if (getRowHeader() == null) {
            setRowHeader(createViewport());
        }
        getRowHeader().setView(view);
    }

    public JViewport getColumnHeader() {
        return columnHeader;
    }

    public void setColumnHeader(JViewport columnHeader) {
        JViewport old = getColumnHeader();
        this.columnHeader = columnHeader;
        if (columnHeader != null) {
            add(columnHeader, COLUMN_HEADER);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("columnHeader", old, columnHeader);
        revalidate();
        repaint();
    }

    public void setColumnHeaderView(Component view) {
        if (getColumnHeader() == null) {
            setColumnHeader(createViewport());
        }
        getColumnHeader().setView(view);
    }

    /**
     * La pieza de esa esquina.
     *
     * <p>Las esquinas "inicial" y "final" se resuelven segun la orientacion: en un idioma que se
     * lee de derecha a izquierda, la inicial de arriba es la de arriba a la derecha.
     */
    public Component getCorner(String key) {
        boolean isLeftToRight = getComponentOrientation().isLeftToRight();
        if (key.equals(LOWER_LEADING_CORNER)) {
            key = isLeftToRight ? LOWER_LEFT_CORNER : LOWER_RIGHT_CORNER;
        } else if (key.equals(LOWER_TRAILING_CORNER)) {
            key = isLeftToRight ? LOWER_RIGHT_CORNER : LOWER_LEFT_CORNER;
        } else if (key.equals(UPPER_LEADING_CORNER)) {
            key = isLeftToRight ? UPPER_LEFT_CORNER : UPPER_RIGHT_CORNER;
        } else if (key.equals(UPPER_TRAILING_CORNER)) {
            key = isLeftToRight ? UPPER_RIGHT_CORNER : UPPER_LEFT_CORNER;
        }

        if (key.equals(LOWER_LEFT_CORNER)) {
            return lowerLeft;
        } else if (key.equals(LOWER_RIGHT_CORNER)) {
            return lowerRight;
        } else if (key.equals(UPPER_LEFT_CORNER)) {
            return upperLeft;
        } else if (key.equals(UPPER_RIGHT_CORNER)) {
            return upperRight;
        }
        return null;
    }

    /** Pone una pieza en una esquina; una esquina solo se ve si las dos barras que la rodean estan. */
    public void setCorner(String key, Component corner) {
        Component old;
        boolean isLeftToRight = getComponentOrientation().isLeftToRight();
        if (key.equals(LOWER_LEADING_CORNER)) {
            key = isLeftToRight ? LOWER_LEFT_CORNER : LOWER_RIGHT_CORNER;
        } else if (key.equals(LOWER_TRAILING_CORNER)) {
            key = isLeftToRight ? LOWER_RIGHT_CORNER : LOWER_LEFT_CORNER;
        } else if (key.equals(UPPER_LEADING_CORNER)) {
            key = isLeftToRight ? UPPER_LEFT_CORNER : UPPER_RIGHT_CORNER;
        } else if (key.equals(UPPER_TRAILING_CORNER)) {
            key = isLeftToRight ? UPPER_RIGHT_CORNER : UPPER_LEFT_CORNER;
        }

        if (key.equals(LOWER_LEFT_CORNER)) {
            old = lowerLeft;
            lowerLeft = corner;
        } else if (key.equals(LOWER_RIGHT_CORNER)) {
            old = lowerRight;
            lowerRight = corner;
        } else if (key.equals(UPPER_LEFT_CORNER)) {
            old = upperLeft;
            upperLeft = corner;
        } else if (key.equals(UPPER_RIGHT_CORNER)) {
            old = upperRight;
            upperRight = corner;
        } else {
            throw new IllegalArgumentException("invalid corner key");
        }
        if (old != null) {
            remove(old);
        }
        if (corner != null) {
            add(corner, key);
        }
        firePropertyChange(key, old, corner);
        revalidate();
        repaint();
    }

    /** Le pasa la orientacion a la ventana y a las dos barras. */
    public void setComponentOrientation(ComponentOrientation co) {
        super.setComponentOrientation(co);
        if (viewport != null) {
            viewport.setComponentOrientation(co);
        }
        if (verticalScrollBar != null) {
            verticalScrollBar.setComponentOrientation(co);
        }
        if (horizontalScrollBar != null) {
            horizontalScrollBar.setComponentOrientation(co);
        }
    }

    /** Si la rueda desplaza; ver la nota de la clase. */
    public boolean isWheelScrollingEnabled() {
        return wheelScrollState;
    }

    public void setWheelScrollingEnabled(boolean handleWheel) {
        boolean old = wheelScrollState;
        wheelScrollState = handleWheel;
        firePropertyChange("wheelScrollingEnabled", old, handleWheel);
    }

    protected String paramString() {
        String viewportBorderString = (viewportBorder != null ? viewportBorder.toString() : "");
        String viewportString = (viewport != null ? viewport.toString() : "");
        String verticalScrollBarPolicyString;
        if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_AS_NEEDED";
        } else if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_NEVER) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_NEVER";
        } else if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_ALWAYS";
        } else {
            verticalScrollBarPolicyString = "";
        }
        String horizontalScrollBarPolicyString;
        if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_AS_NEEDED";
        } else if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_NEVER";
        } else if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_ALWAYS";
        } else {
            horizontalScrollBarPolicyString = "";
        }

        return super.paramString() + ",columnHeader=" + (columnHeader != null ? "" : "")
                + ",horizontalScrollBar=" + (horizontalScrollBar != null ? "" : "")
                + ",horizontalScrollBarPolicy=" + horizontalScrollBarPolicyString
                + ",rowHeader=" + (rowHeader != null ? "" : "")
                + ",verticalScrollBar=" + (verticalScrollBar != null ? "" : "")
                + ",verticalScrollBarPolicy=" + verticalScrollBarPolicyString
                + ",viewport=" + viewportString + ",viewportBorder=" + viewportBorderString;
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * La barra que crea el panel: la que le pregunta al contenido cuanto avanzar.
     *
     * <p>Si el contenido es {@link Scrollable}, los dos escalones salen de el; si no, el chico es
     * uno y el grande es una pantalla. Fijar un escalon a mano corta la pregunta: a partir de ahi
     * manda el numero fijado, que es lo que espera quien lo fijo.
     */
    // `UIResource` sin calificar se resuelve a `ScrollPaneLayout.UIResource`, del mismo paquete,
    // y no a la interfaz importada (#493). Va calificada.
    protected class ScrollBar extends JScrollBar implements javax.swing.plaf.UIResource {

        private boolean unitIncrementSet;
        private boolean blockIncrementSet;

        public ScrollBar(int orientation) {
            super(orientation);
        }

        public void setUnitIncrement(int unitIncrement) {
            unitIncrementSet = true;
            super.setUnitIncrement(unitIncrement);
        }

        public int getUnitIncrement(int direction) {
            JViewport vp = getViewport();
            if (!unitIncrementSet && (vp != null) && (vp.getView() instanceof Scrollable)) {
                Scrollable view = (Scrollable) (vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableUnitIncrement(vr, getOrientation(), direction);
            }
            return super.getUnitIncrement(direction);
        }

        public void setBlockIncrement(int blockIncrement) {
            blockIncrementSet = true;
            super.setBlockIncrement(blockIncrement);
        }

        public int getBlockIncrement(int direction) {
            JViewport vp = getViewport();
            if (blockIncrementSet || vp == null) {
                return super.getBlockIncrement(direction);
            } else if (vp.getView() instanceof Scrollable) {
                Scrollable view = (Scrollable) (vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableBlockIncrement(vr, getOrientation(), direction);
            } else if (getOrientation() == VERTICAL) {
                return vp.getExtentSize().height;
            } else {
                return vp.getExtentSize().width;
            }
        }
    }
}
