package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ToolBarUI;

/**
 * La fila de botones que va debajo del menu.
 *
 * <h2>Es un panel con una regla</h2>
 *
 * <p>Adentro va cualquier componente: botones, listas desplegables, campos. Lo unico que agrega
 * sobre un panel comun es que sabe acomodarse en fila o en columna y, si se lo deja, que se puede
 * arrancar de su lugar y dejar flotando en una ventanita ({@link #setFloatable}).
 *
 * <h2>Agregar una accion crea el boton</h2>
 *
 * <p>{@link #add(Action)} no agrega la accion: arma un {@link JButton} configurado desde ella y lo
 * agrega. Devuelve el boton justamente para poder retocarlo. El boton que arma no muestra el texto
 * de la accion si tiene icono -- una barra de herramientas con texto en cada boton ocupa el doble --
 * y eso lo decide {@link #createActionComponent}, que una subclase puede cambiar.
 *
 * <h2>El separador no es el de los menus</h2>
 *
 * <p>{@link Separator} hereda de {@link JSeparator} pero se comporta al reves en un punto: su tamano
 * es fijo y no se estira. Un separador que se estirara dejaria a los botones amontonados de un lado.
 */
public class JToolBar extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "ToolBarUI";

    private boolean paintBorder = true;
    private Insets margin = null;
    private boolean floatable = true;
    private int orientation = HORIZONTAL;

    /** Horizontal y sin nombre. */
    public JToolBar() {
        this(HORIZONTAL);
    }

    /**
     * Con esa orientacion.
     *
     * @throws IllegalArgumentException si no es horizontal ni vertical.
     */
    public JToolBar(int orientation) {
        this(null, orientation);
    }

    /** Con ese nombre, horizontal; el nombre es el titulo de la ventanita al flotar. */
    public JToolBar(String name) {
        this(name, HORIZONTAL);
    }

    /**
     * Con nombre y orientacion.
     *
     * @throws IllegalArgumentException si la orientacion no es horizontal ni vertical.
     */
    public JToolBar(String name, int orientation) {
        setName(name);
        checkOrientation(orientation);
        this.orientation = orientation;
        DefaultToolBarLayout layout = new DefaultToolBarLayout(this, orientation);
        setLayout(layout);
        addPropertyChangeListener(layout);
        updateUI();
    }

    public ToolBarUI getUI() {
        return (ToolBarUI) ui;
    }

    public void setUI(ToolBarUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** En que posicion esta ese componente, o -1 si no esta. */
    public int getComponentIndex(Component c) {
        int ncomponents = this.getComponentCount();
        Component[] component = this.getComponents();
        for (int i = 0; i < ncomponents; i++) {
            Component comp = component[i];
            if (comp == c) {
                return i;
            }
        }
        return -1;
    }

    /** El componente de esa posicion, o nulo si esta fuera de rango. */
    public Component getComponentAtIndex(int i) {
        int ncomponents = this.getComponentCount();
        if (i >= 0 && i < ncomponents) {
            Component[] component = this.getComponents();
            return component[i];
        }
        return null;
    }

    /** Los margenes de la barra; nulo deja los que ponga el aspecto. */
    public void setMargin(Insets m) {
        Insets old = margin;
        margin = m;
        firePropertyChange("margin", old, m);
        revalidate();
        repaint();
    }

    public Insets getMargin() {
        if (margin == null) {
            return new Insets(0, 0, 0, 0);
        }
        return margin;
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        if (paintBorder != b) {
            boolean old = paintBorder;
            paintBorder = b;
            firePropertyChange("borderPainted", old, b);
            revalidate();
            repaint();
        }
    }

    /** Dibuja el borde solo si esta prendido. */
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    /** Si se puede arrancar de su lugar; ver la nota de la clase. */
    public boolean isFloatable() {
        return floatable;
    }

    public void setFloatable(boolean b) {
        if (floatable != b) {
            boolean old = floatable;
            floatable = b;
            firePropertyChange("floatable", old, b);
            revalidate();
            repaint();
        }
    }

    public int getOrientation() {
        return orientation;
    }

    /**
     * En fila o en columna.
     *
     * @throws IllegalArgumentException si no es horizontal ni vertical.
     */
    public void setOrientation(int o) {
        checkOrientation(o);
        if (orientation != o) {
            int old = orientation;
            orientation = o;
            firePropertyChange("orientation", old, o);
            revalidate();
            repaint();
        }
    }

    private void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    /** Si los botones se dibujan planos hasta que el puntero pasa por encima. */
    public void setRollover(boolean rollover) {
        putClientProperty("JToolBar.isRollover", rollover ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isRollover() {
        Boolean rollover = (Boolean) getClientProperty("JToolBar.isRollover");
        if (rollover != null) {
            return rollover.booleanValue();
        }
        return false;
    }

    /** Un separador del tamano que decida el aspecto. */
    public void addSeparator() {
        JToolBar.Separator s = new JToolBar.Separator();
        add(s);
    }

    /** Un separador de ese tamano. */
    public void addSeparator(Dimension size) {
        JToolBar.Separator s = new JToolBar.Separator(size);
        add(s);
    }

    /**
     * Arma un boton desde esa accion y lo agrega.
     *
     * @return el boton, para poder retocarlo.
     */
    public JButton add(Action a) {
        JButton b = createActionComponent(a);
        b.setAction(a);
        add(b);
        return b;
    }

    /**
     * El boton que representa a esa accion.
     *
     * <p>Sin texto si la accion trae icono: ver la nota de la clase.
     */
    protected JButton createActionComponent(Action a) {
        String text = a != null ? (String) a.getValue(Action.NAME) : null;
        Icon icon = a != null ? (Icon) a.getValue(Action.SMALL_ICON) : null;
        boolean enabled = a != null ? a.isEnabled() : true;
        String tooltip = a != null ? (String) a.getValue(Action.SHORT_DESCRIPTION) : null;
        JButton b = new JButton(text, icon) {
            protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
                PropertyChangeListener pcl = createActionChangeListener(this);
                if (pcl == null) {
                    pcl = super.createActionPropertyChangeListener(a);
                }
                return pcl;
            }
        };
        if (icon != null) {
            b.putClientProperty("hideActionText", Boolean.TRUE);
        }
        b.setHorizontalTextPosition(JButton.CENTER);
        b.setVerticalTextPosition(JButton.BOTTOM);
        b.setEnabled(enabled);
        b.setToolTipText(tooltip);
        return b;
    }

    /** Nulo: el boton usa el oyente que arma {@code AbstractButton} por su cuenta. */
    protected PropertyChangeListener createActionChangeListener(JButton b) {
        return null;
    }

    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof Separator) {
            if (getOrientation() == VERTICAL) {
                ((Separator) comp).setOrientation(JSeparator.HORIZONTAL);
            } else {
                ((Separator) comp).setOrientation(JSeparator.VERTICAL);
            }
        }
        super.addImpl(comp, constraints, index);
    }

    protected String paramString() {
        return super.paramString();
    }

    public void setLayout(LayoutManager mgr) {
        super.setLayout(mgr);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * El separador de una barra de herramientas.
     *
     * <p>Su tamano es el mismo para el minimo, el preferido y el maximo: no se estira. Ver la nota
     * de {@link JToolBar}.
     */
    public static class Separator extends JSeparator {

        private Dimension separatorSize;

        /** Del tamano que decida el aspecto. */
        public Separator() {
            this(null);
        }

        /** De ese tamano. */
        public Separator(Dimension size) {
            super(JSeparator.HORIZONTAL);
            setSeparatorSize(size);
        }

        public String getUIClassID() {
            return "ToolBarSeparatorUI";
        }

        /** Nulo devuelve la decision al aspecto. */
        public void setSeparatorSize(Dimension size) {
            if (size != null) {
                separatorSize = size;
            } else {
                super.updateUI();
            }
            this.invalidate();
        }

        public Dimension getSeparatorSize() {
            return separatorSize;
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            if (separatorSize != null) {
                return separatorSize.getSize();
            }
            return super.getPreferredSize();
        }
    }

    /**
     * Acomoda en fila o en columna, siguiendo la orientacion de la barra.
     *
     * <p>No hereda de {@link BoxLayout}: **lo envuelve**. Un {@code BoxLayout} se ata al contenedor
     * que le pasan en el constructor y se niega a acomodar otro, asi que girar la barra obliga a
     * armar uno nuevo. Envolviendolo se puede reemplazar el de adentro sin cambiar el acomodador que
     * la barra tiene puesto.
     */
    private static class DefaultToolBarLayout implements LayoutManager2, PropertyChangeListener,
            java.io.Serializable {

        private final JToolBar barra;
        private BoxLayout lm;

        DefaultToolBarLayout(JToolBar barra, int orientation) {
            this.barra = barra;
            this.lm = armar(barra, orientation);
        }

        private static BoxLayout armar(JToolBar barra, int orientation) {
            if (orientation == JToolBar.VERTICAL) {
                return new BoxLayout(barra, BoxLayout.PAGE_AXIS);
            }
            return new BoxLayout(barra, BoxLayout.LINE_AXIS);
        }

        public void addLayoutComponent(String name, Component comp) {
            lm.addLayoutComponent(name, comp);
        }

        public void addLayoutComponent(Component comp, Object constraints) {
            lm.addLayoutComponent(comp, constraints);
        }

        public void removeLayoutComponent(Component comp) {
            lm.removeLayoutComponent(comp);
        }

        public Dimension preferredLayoutSize(Container target) {
            return lm.preferredLayoutSize(target);
        }

        public Dimension minimumLayoutSize(Container target) {
            return lm.minimumLayoutSize(target);
        }

        public Dimension maximumLayoutSize(Container target) {
            return lm.maximumLayoutSize(target);
        }

        public void layoutContainer(Container target) {
            lm.layoutContainer(target);
        }

        public float getLayoutAlignmentX(Container target) {
            return lm.getLayoutAlignmentX(target);
        }

        public float getLayoutAlignmentY(Container target) {
            return lm.getLayoutAlignmentY(target);
        }

        public void invalidateLayout(Container target) {
            lm.invalidateLayout(target);
        }

        /** Girar la barra cambia el eje; ver la nota de la clase. */
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if (name.equals("orientation")) {
                int o = ((Integer) e.getNewValue()).intValue();
                lm = armar(barra, o);
            }
        }
    }
}
