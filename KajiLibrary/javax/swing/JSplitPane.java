package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics;

import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SplitPaneUI;

/**
 * Dos componentes separados por una division que se puede arrastrar.
 *
 * <h2>Los hijos se ponen por posicion, no por orden</h2>
 *
 * <p>{@code add(comp, JSplitPane.LEFT)} y {@code setLeftComponent(comp)} son lo mismo. Agregar sin
 * decir donde pone el primero a la izquierda y el segundo a la derecha, y el tercero reemplaza al
 * primero: un panel dividido tiene exactamente dos lugares.
 *
 * <h2>Donde queda la division al cambiar de tamano</h2>
 *
 * <p>Lo decide {@link #setResizeWeight}: cero le da todo el espacio nuevo al de la derecha, uno al
 * de la izquierda, y un medio lo reparte. Es la propiedad que mas se olvida y la que explica por
 * que un panel dividido "no respeta" el tamano que uno le puso.
 *
 * <h2>Continuo o no</h2>
 *
 * <p>Con {@link #setContinuousLayout} prendido, arrastrar la division reacomoda los dos lados a
 * cada pixel. Apagado, solo se mueve una raya y el reacomodo ocurre al soltar. Apagado existe
 * porque reacomodar contenido caro sesenta veces por segundo se siente peor que una raya.
 */
public class JSplitPane extends JComponent implements javax.accessibility.Accessible {

    private static final String uiClassID = "SplitPaneUI";

    /** Los componentes van uno arriba del otro. */
    public static final int VERTICAL_SPLIT = 0;

    /** Uno al lado del otro. */
    public static final int HORIZONTAL_SPLIT = 1;

    /** El lugar de la izquierda. */
    public static final String LEFT = "left";

    /** El de la derecha. */
    public static final String RIGHT = "right";

    /** El de arriba; es el mismo lugar que {@link #LEFT}. */
    public static final String TOP = "top";

    /** El de abajo; el mismo que {@link #RIGHT}. */
    public static final String BOTTOM = "bottom";

    /** La division misma, que el aspecto agrega como hijo. */
    public static final String DIVIDER = "divider";

    public static final String ORIENTATION_PROPERTY = "orientation";
    public static final String CONTINUOUS_LAYOUT_PROPERTY = "continuousLayout";
    public static final String DIVIDER_SIZE_PROPERTY = "dividerSize";
    public static final String ONE_TOUCH_EXPANDABLE_PROPERTY = "oneTouchExpandable";
    public static final String LAST_DIVIDER_LOCATION_PROPERTY = "lastDividerLocation";
    public static final String DIVIDER_LOCATION_PROPERTY = "dividerLocation";
    public static final String RESIZE_WEIGHT_PROPERTY = "resizeWeight";

    /** {@link #HORIZONTAL_SPLIT} o {@link #VERTICAL_SPLIT}. */
    protected int orientation;

    /** Si arrastrar reacomoda a cada paso; ver la nota de la clase. */
    protected boolean continuousLayout;

    /** El componente de la izquierda o de arriba. */
    protected Component leftComponent;

    /** El de la derecha o de abajo. */
    protected Component rightComponent;

    /** El ancho de la division. */
    protected int dividerSize;

    /** Si la division tiene flechitas para plegar un lado de un clic. */
    protected boolean oneTouchExpandable;

    /** Donde estaba la division antes del ultimo movimiento. */
    protected int lastDividerLocation;

    private double resizeWeight;
    private boolean dividerSizeSet = false;
    private AccessibleContext accessibleContext;

    /** Un panel dividido en dos a lo ancho, vacio. */
    public JSplitPane() {
        this(HORIZONTAL_SPLIT, false, new JButton("left"), new JButton("right"));
    }

    /** Un panel dividido con esa orientacion. */
    public JSplitPane(int newOrientation) {
        this(newOrientation, false);
    }

    /** Con esa orientacion y ese modo de arrastre. */
    public JSplitPane(int newOrientation, boolean newContinuousLayout) {
        this(newOrientation, newContinuousLayout, null, null);
    }

    /** Con esa orientacion y esos dos componentes. */
    public JSplitPane(int newOrientation, Component newLeftComponent,
            Component newRightComponent) {
        this(newOrientation, false, newLeftComponent, newRightComponent);
    }

    /**
     * El constructor completo.
     *
     * @throws IllegalArgumentException si la orientacion no es una de las dos.
     */
    public JSplitPane(int newOrientation, boolean newContinuousLayout,
            Component newLeftComponent, Component newRightComponent) {
        super();
        dividerLocation = -1;
        setLayout(null);
        setUIProperty("opaque", Boolean.TRUE);
        orientation = newOrientation;
        if (orientation != HORIZONTAL_SPLIT && orientation != VERTICAL_SPLIT) {
            throw new IllegalArgumentException("cannot create JSplitPane, "
                    + "orientation must be one of "
                    + "JSplitPane.HORIZONTAL_SPLIT or JSplitPane.VERTICAL_SPLIT");
        }
        continuousLayout = newContinuousLayout;
        if (newLeftComponent != null) {
            setLeftComponent(newLeftComponent);
        }
        if (newRightComponent != null) {
            setRightComponent(newRightComponent);
        }
        updateUI();
    }

    private int dividerLocation;

    public void setComponentOrientation(ComponentOrientation orientation) {
        super.setComponentOrientation(orientation);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    public void setUI(SplitPaneUI ui) {
        if ((SplitPaneUI) this.ui != ui) {
            super.setUI(ui);
            revalidate();
        }
    }

    public SplitPaneUI getUI() {
        return (SplitPaneUI) ui;
    }

    public void updateUI() {
        revalidate();
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** El ancho de la division, en pixeles. */
    public void setDividerSize(int newSize) {
        int oldSize = dividerSize;
        dividerSizeSet = true;
        if (oldSize != newSize) {
            dividerSize = newSize;
            firePropertyChange(DIVIDER_SIZE_PROPERTY, oldSize, newSize);
        }
    }

    public int getDividerSize() {
        return dividerSize;
    }

    /** El componente de la izquierda o de arriba. */
    public void setLeftComponent(Component comp) {
        if (comp == null) {
            if (leftComponent != null) {
                remove(leftComponent);
                leftComponent = null;
            }
        } else {
            add(comp, JSplitPane.LEFT);
        }
    }

    public Component getLeftComponent() {
        return leftComponent;
    }

    /** El mismo lugar que {@link #setLeftComponent}. */
    public void setTopComponent(Component comp) {
        setLeftComponent(comp);
    }

    public Component getTopComponent() {
        return leftComponent;
    }

    public void setRightComponent(Component comp) {
        if (comp == null) {
            if (rightComponent != null) {
                remove(rightComponent);
                rightComponent = null;
            }
        } else {
            add(comp, JSplitPane.RIGHT);
        }
    }

    public Component getRightComponent() {
        return rightComponent;
    }

    public void setBottomComponent(Component comp) {
        setRightComponent(comp);
    }

    public Component getBottomComponent() {
        return rightComponent;
    }

    /** Si la division lleva flechitas para plegar un lado. */
    public void setOneTouchExpandable(boolean newValue) {
        boolean oldValue = oneTouchExpandable;
        oneTouchExpandable = newValue;
        firePropertyChange(ONE_TOUCH_EXPANDABLE_PROPERTY, oldValue, newValue);
        repaint();
    }

    public boolean isOneTouchExpandable() {
        return oneTouchExpandable;
    }

    /**
     * Donde estaba la division antes.
     *
     * <p>Es lo que usan las flechitas para volver: plegar y desplegar tiene que dejar la division
     * donde estaba, no en un lugar calculado.
     */
    public void setLastDividerLocation(int newLastLocation) {
        int oldLocation = lastDividerLocation;
        lastDividerLocation = newLastLocation;
        firePropertyChange(LAST_DIVIDER_LOCATION_PROPERTY, oldLocation, newLastLocation);
    }

    public int getLastDividerLocation() {
        return lastDividerLocation;
    }

    /**
     * Si los componentes van al lado o uno sobre otro.
     *
     * @throws IllegalArgumentException si no es una de las dos.
     */
    public void setOrientation(int orientation) {
        if ((orientation != VERTICAL_SPLIT) && (orientation != HORIZONTAL_SPLIT)) {
            throw new IllegalArgumentException("JSplitPane: orientation must be "
                    + "one of JSplitPane.VERTICAL_SPLIT or JSplitPane.HORIZONTAL_SPLIT");
        }
        int oldOrientation = this.orientation;
        this.orientation = orientation;
        firePropertyChange(ORIENTATION_PROPERTY, oldOrientation, orientation);
    }

    public int getOrientation() {
        return orientation;
    }

    /** Si arrastrar reacomoda a cada paso; ver la nota de la clase. */
    public void setContinuousLayout(boolean newContinuousLayout) {
        boolean oldCD = continuousLayout;
        continuousLayout = newContinuousLayout;
        firePropertyChange(CONTINUOUS_LAYOUT_PROPERTY, oldCD, newContinuousLayout);
    }

    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    /**
     * Como se reparte el espacio nuevo al agrandar.
     *
     * @throws IllegalArgumentException si no esta entre cero y uno.
     */
    public void setResizeWeight(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("JSplitPane weight must be between 0 and 1");
        }
        double oldWeight = resizeWeight;
        resizeWeight = value;
        firePropertyChange(RESIZE_WEIGHT_PROPERTY, oldWeight, value);
    }

    public double getResizeWeight() {
        return resizeWeight;
    }

    /** Pone la division donde los dos lados tengan su tamano preferido. */
    public void resetToPreferredSizes() {
        SplitPaneUI ui = getUI();
        if (ui != null) {
            ui.resetToPreferredSizes(this);
        }
    }

    /**
     * Mueve la division a ese pixel.
     *
     * <p>Un valor negativo significa "acomodala sola", que es lo que hace un panel recien armado.
     */
    /**
     * La posicion del divisor como proporcion del espacio.
     *
     * <p>Cero lo pega al principio, uno al final, {@code 0.5} lo deja al medio. Se traduce a pixeles
     * <strong>ahora</strong>, con el tamano que el panel tiene en este momento: no es una
     * proporcion que se mantenga al cambiar de tamano -- para eso esta el peso de redimensionado.
     *
     * @throws IllegalArgumentException si no esta entre cero y uno
     */
    public void setDividerLocation(double proportionalLocation) {
        if (proportionalLocation < 0.0 || proportionalLocation > 1.0) {
            throw new IllegalArgumentException(
                    "proportional location must be between 0.0 and 1.0.");
        }
        if (getOrientation() == VERTICAL_SPLIT) {
            setDividerLocation((int) ((double) (getHeight() - getDividerSize())
                    * proportionalLocation));
        } else {
            setDividerLocation((int) ((double) (getWidth() - getDividerSize())
                    * proportionalLocation));
        }
    }

    public void setDividerLocation(int location) {
        int oldValue = dividerLocation;
        dividerLocation = location;
        SplitPaneUI ui = getUI();
        if (ui != null) {
            ui.setDividerLocation(this, location);
        }
        firePropertyChange(DIVIDER_LOCATION_PROPERTY, oldValue, location);
    }

    public int getDividerLocation() {
        return dividerLocation;
    }

    /** Lo mas a la izquierda que la division puede ir sin achicar de mas al primero. */
    public int getMinimumDividerLocation() {
        SplitPaneUI ui = getUI();
        return (ui != null) ? ui.getMinimumDividerLocation(this) : -1;
    }

    public int getMaximumDividerLocation() {
        SplitPaneUI ui = getUI();
        return (ui != null) ? ui.getMaximumDividerLocation(this) : -1;
    }

    /** Saca un componente y olvida su lugar. */
    public void remove(Component component) {
        if (component == leftComponent) {
            leftComponent = null;
        } else if (component == rightComponent) {
            rightComponent = null;
        }
        super.remove(component);
        revalidate();
        repaint();
    }

    public void remove(int index) {
        Component comp = getComponent(index);
        if (comp == leftComponent) {
            leftComponent = null;
        } else if (comp == rightComponent) {
            rightComponent = null;
        }
        super.remove(index);
        revalidate();
        repaint();
    }

    public void removeAll() {
        leftComponent = null;
        rightComponent = null;
        super.removeAll();
        revalidate();
        repaint();
    }

    /**
     * Si un cambio adentro se puede acomodar sin rehacer la ventana entera.
     *
     * <p>Siempre cierto. El panel dividido reparte un espacio fijo entre dos lados: lo que pase
     * adentro de cualquiera de ellos no cambia lo que el panel ocupa, asi que el reacomodo puede
     * frenar aca en lugar de subir hasta la ventana.
     */
    public boolean isValidateRoot() {
        return true;
    }

    /**
     * Agrega un hijo en el lugar que diga la restriccion.
     *
     * <p>Sin restriccion, el primero va a la izquierda y el segundo a la derecha. Es lo que hace
     * que {@code add(a); add(b);} arme el panel que uno espera.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        Component toRemove;
        if (constraints != null && !(constraints instanceof String)) {
            throw new IllegalArgumentException("cannot add to layout: "
                    + "constraint must be a string (or null)");
        }
        if (constraints == null) {
            if (getLeftComponent() == null) {
                constraints = JSplitPane.LEFT;
            } else if (getRightComponent() == null) {
                constraints = JSplitPane.RIGHT;
            }
        }
        if (constraints != null && (constraints.equals(JSplitPane.LEFT)
                || constraints.equals(JSplitPane.TOP))) {
            toRemove = getLeftComponent();
            if (toRemove != null) {
                remove(toRemove);
            }
            leftComponent = comp;
            index = -1;
        } else if (constraints != null && (constraints.equals(JSplitPane.RIGHT)
                || constraints.equals(JSplitPane.BOTTOM))) {
            toRemove = getRightComponent();
            if (toRemove != null) {
                remove(toRemove);
            }
            rightComponent = comp;
            index = -1;
        } else if (constraints != null && constraints.equals(JSplitPane.DIVIDER)) {
            index = -1;
        }
        super.addImpl(comp, constraints, index);
        revalidate();
        repaint();
    }

    /** Dibuja los hijos y despues deja que el aspecto termine la division. */
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        SplitPaneUI ui = getUI();
        if (ui != null) {
            Graphics tempG = g.create();
            ui.finishedPaintingChildren(this, tempG);
            tempG.dispose();
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
