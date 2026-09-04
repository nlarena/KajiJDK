package java.awt;

import java.io.Serializable;

/**
 * Cinco lugares: los cuatro bordes y el centro.
 *
 * <p>Es la distribución más usada de AWT y la que mejor reparte el espacio sobrante: **el centro se
 * queda con todo lo que quede**. Los bordes reciben su medida preferida en la dirección que los
 * limita —el norte su alto, el oeste su ancho— y se estiran en la otra.
 *
 * <p>El orden importa: primero se reservan norte y sur a lo ancho de todo, después este y oeste con
 * lo que queda de alto, y el centro se lleva el resto. Por eso una barra de herramientas al norte
 * llega de punta a punta y una barra lateral al oeste no.
 *
 * <p>Las constantes vienen en **dos juegos** y es la parte que más confunde. `NORTH` y compañía son
 * absolutas; {@link #PAGE_START} y {@link #LINE_START} son relativas a cómo se lee el texto, así que
 * en árabe `LINE_START` es la derecha. Poner un componente en las dos formas a la vez es un error, y
 * la posición relativa gana.
 */
public class BorderLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = -8658291919501921765L;

    /** Arriba, de punta a punta. */
    public static final String NORTH = "North";

    /** Abajo, de punta a punta. */
    public static final String SOUTH = "South";

    /** A la derecha, entre el norte y el sur. */
    public static final String EAST = "East";

    /** A la izquierda, entre el norte y el sur. */
    public static final String WEST = "West";

    /** El resto del espacio. */
    public static final String CENTER = "Center";

    /** Donde empieza la página: arriba en las escrituras horizontales. */
    public static final String PAGE_START = "First";

    /** Donde termina la página. */
    public static final String PAGE_END = "Last";

    /** Donde empieza el renglón: la izquierda, o la derecha en árabe y hebreo. */
    public static final String LINE_START = "Before";

    /** Donde termina el renglón. */
    public static final String LINE_END = "After";

    /**
     * El nombre viejo de {@link #PAGE_START}.
     *
     * @deprecated se renombró en 1.4 para que el juego relativo fuera coherente.
     */
    @Deprecated
    public static final String BEFORE_FIRST_LINE = PAGE_START;

    /**
     * El nombre viejo de {@link #PAGE_END}.
     *
     * @deprecated se renombró en 1.4.
     */
    @Deprecated
    public static final String AFTER_LAST_LINE = PAGE_END;

    /**
     * El nombre viejo de {@link #LINE_START}.
     *
     * @deprecated se renombró en 1.4.
     */
    @Deprecated
    public static final String BEFORE_LINE_BEGINS = LINE_START;

    /**
     * El nombre viejo de {@link #LINE_END}.
     *
     * @deprecated se renombró en 1.4.
     */
    @Deprecated
    public static final String AFTER_LINE_ENDS = LINE_END;

    private int hgap;
    private int vgap;

    private Component north;
    private Component west;
    private Component east;
    private Component south;
    private Component center;

    private Component firstLine;
    private Component lastLine;
    private Component firstItem;
    private Component lastItem;

    /** Sin separación entre los cinco lugares. */
    public BorderLayout() {
        this(0, 0);
    }

    /** Con las separaciones dadas. */
    public BorderLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /** Cuánto separa horizontalmente. */
    public int getHgap() {
        return this.hgap;
    }

    /** Cambia la separación horizontal. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** Cuánto separa verticalmente. */
    public int getVgap() {
        return this.vgap;
    }

    /** Cambia la separación vertical. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Anota en qué lugar va un hijo.
     *
     * @param constraints una de las nueve constantes, o `null` para el centro
     * @throws IllegalArgumentException si no es ninguna de ellas
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        synchronized (comp.getTreeLock()) {
            Object c = constraints == null ? CENTER : constraints;
            if (!(c instanceof String)) {
                throw new IllegalArgumentException(
                        "cannot add to layout: constraint must be a string (or null)");
            }
            this.ubicar(comp, (String) c);
        }
    }

    /** Guarda el hijo en la ranura que le toca. */
    private void ubicar(Component comp, String name) {
        if (CENTER.equals(name)) {
            this.center = comp;
        } else if (NORTH.equals(name)) {
            this.north = comp;
        } else if (SOUTH.equals(name)) {
            this.south = comp;
        } else if (EAST.equals(name)) {
            this.east = comp;
        } else if (WEST.equals(name)) {
            this.west = comp;
        } else if (PAGE_START.equals(name)) {
            this.firstLine = comp;
        } else if (PAGE_END.equals(name)) {
            this.lastLine = comp;
        } else if (LINE_START.equals(name)) {
            this.firstItem = comp;
        } else if (LINE_END.equals(name)) {
            this.lastItem = comp;
        } else {
            throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
        }
    }

    /**
     * Anota en qué lugar va un hijo, por nombre.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #addLayoutComponent(Component, Object)}.
     * @throws IllegalArgumentException si el nombre no es uno de los nueve
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            this.ubicar(comp, name == null ? CENTER : name);
        }
    }

    /** Saca ese hijo de la ranura en la que esté. */
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            if (comp == this.center) {
                this.center = null;
            } else if (comp == this.north) {
                this.north = null;
            } else if (comp == this.south) {
                this.south = null;
            } else if (comp == this.east) {
                this.east = null;
            } else if (comp == this.west) {
                this.west = null;
            } else if (comp == this.firstLine) {
                this.firstLine = null;
            } else if (comp == this.lastLine) {
                this.lastLine = null;
            } else if (comp == this.firstItem) {
                this.firstItem = null;
            } else if (comp == this.lastItem) {
                this.lastItem = null;
            }
        }
    }

    /**
     * Qué hijo está en ese lugar.
     *
     * @return el hijo, o `null` si el lugar está vacío
     * @throws IllegalArgumentException si la posición no es una de las nueve
     */
    public Component getLayoutComponent(Object constraints) {
        if (CENTER.equals(constraints)) {
            return this.center;
        }
        if (NORTH.equals(constraints)) {
            return this.north;
        }
        if (SOUTH.equals(constraints)) {
            return this.south;
        }
        if (WEST.equals(constraints)) {
            return this.west;
        }
        if (EAST.equals(constraints)) {
            return this.east;
        }
        if (PAGE_START.equals(constraints)) {
            return this.firstLine;
        }
        if (PAGE_END.equals(constraints)) {
            return this.lastLine;
        }
        if (LINE_START.equals(constraints)) {
            return this.firstItem;
        }
        if (LINE_END.equals(constraints)) {
            return this.lastItem;
        }
        throw new IllegalArgumentException("cannot get component: invalid constraint: "
                + constraints);
    }

    /**
     * Qué hijo está en ese lugar, resolviendo las posiciones relativas para ese contenedor.
     *
     * <p>Preguntar por `NORTH` acá devuelve el que esté en `PAGE_START` si no hay ninguno en
     * `NORTH`: es la consulta que hace falta cuando lo que importa es dónde va a quedar dibujado y
     * no con qué constante se lo agregó.
     *
     * @throws IllegalArgumentException si la posición no es una de las nueve
     */
    public Component getLayoutComponent(Container target, Object constraints) {
        boolean ltr = target.getComponentOrientation().isLeftToRight();
        if (CENTER.equals(constraints)) {
            return this.center;
        }
        if (NORTH.equals(constraints)) {
            return this.firstLine != null ? this.firstLine : this.north;
        }
        if (SOUTH.equals(constraints)) {
            return this.lastLine != null ? this.lastLine : this.south;
        }
        if (WEST.equals(constraints)) {
            Component c = ltr ? this.firstItem : this.lastItem;
            return c != null ? c : this.west;
        }
        if (EAST.equals(constraints)) {
            Component c = ltr ? this.lastItem : this.firstItem;
            return c != null ? c : this.east;
        }
        if (PAGE_START.equals(constraints)) {
            return this.firstLine != null ? this.firstLine : this.north;
        }
        if (PAGE_END.equals(constraints)) {
            return this.lastLine != null ? this.lastLine : this.south;
        }
        if (LINE_START.equals(constraints)) {
            Component c = this.firstItem;
            return c != null ? c : (ltr ? this.west : this.east);
        }
        if (LINE_END.equals(constraints)) {
            Component c = this.lastItem;
            return c != null ? c : (ltr ? this.east : this.west);
        }
        throw new IllegalArgumentException("cannot get component: unknown constraint: "
                + constraints);
    }

    /**
     * En qué lugar está ese hijo.
     *
     * @return la constante con la que se lo agregó, o `null` si no está en esta distribución
     */
    public Object getConstraints(Component comp) {
        if (comp == null) {
            return null;
        }
        if (comp == this.center) {
            return CENTER;
        }
        if (comp == this.north) {
            return NORTH;
        }
        if (comp == this.south) {
            return SOUTH;
        }
        if (comp == this.west) {
            return WEST;
        }
        if (comp == this.east) {
            return EAST;
        }
        if (comp == this.firstLine) {
            return PAGE_START;
        }
        if (comp == this.lastLine) {
            return PAGE_END;
        }
        if (comp == this.firstItem) {
            return LINE_START;
        }
        if (comp == this.lastItem) {
            return LINE_END;
        }
        return null;
    }

    /** El ancho de la fila más ancha y el alto de todo lo apilado. */
    public Dimension minimumLayoutSize(Container target) {
        return this.medir(target, false);
    }

    /** Lo mismo, con las medidas preferidas. */
    public Dimension preferredLayoutSize(Container target) {
        return this.medir(target, true);
    }

    /** Norte y sur suman alto; este, oeste y centro suman ancho. */
    private Dimension medir(Container target, boolean preferida) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            Component c = this.getChild(EAST, ltr);
            if (c != null) {
                Dimension d = preferida ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = dim.width + d.width + this.hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            c = this.getChild(WEST, ltr);
            if (c != null) {
                Dimension d = preferida ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = dim.width + d.width + this.hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            c = this.getChild(CENTER, ltr);
            if (c != null) {
                Dimension d = preferida ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = dim.width + d.width;
                dim.height = Math.max(d.height, dim.height);
            }
            c = this.getChild(NORTH, ltr);
            if (c != null) {
                Dimension d = preferida ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height = dim.height + d.height + this.vgap;
            }
            c = this.getChild(SOUTH, ltr);
            if (c != null) {
                Dimension d = preferida ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height = dim.height + d.height + this.vgap;
            }
            Insets insets = target.getInsets();
            dim.width = dim.width + insets.left + insets.right;
            dim.height = dim.height + insets.top + insets.bottom;
            return dim;
        }
    }

    /** El hijo de ese lugar, resolviendo la posición relativa contra la absoluta. */
    private Component getChild(String key, boolean ltr) {
        Component result = null;
        if (NORTH.equals(key)) {
            result = this.firstLine != null ? this.firstLine : this.north;
        } else if (SOUTH.equals(key)) {
            result = this.lastLine != null ? this.lastLine : this.south;
        } else if (WEST.equals(key)) {
            result = ltr ? this.firstItem : this.lastItem;
            if (result == null) {
                result = this.west;
            }
        } else if (EAST.equals(key)) {
            result = ltr ? this.lastItem : this.firstItem;
            if (result == null) {
                result = this.east;
            }
        } else if (CENTER.equals(key)) {
            result = this.center;
        }
        if (result != null && !result.isVisible()) {
            result = null;
        }
        return result;
    }

    /** Sin tope: el centro aprovecha todo lo que le den. */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** Centrado. */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /** Centrado. */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /** No guarda cuentas entre llamadas, así que no hay nada que tirar. */
    public void invalidateLayout(Container target) {
    }

    /**
     * Ubica los cinco lugares.
     *
     * <p>El orden es el que define la distribución: norte y sur se llevan el ancho completo, este y
     * oeste el alto que sobra, y el centro lo que queda. Cambiar ese orden cambiaría qué componente
     * llega a las esquinas.
     */
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int top = insets.top;
            int bottom = target.getHeight() - insets.bottom;
            int left = insets.left;
            int right = target.getWidth() - insets.right;
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            Component c = this.getChild(NORTH, ltr);
            if (c != null) {
                c.setSize(right - left, c.getHeight());
                Dimension d = c.getPreferredSize();
                c.setBounds(left, top, right - left, d.height);
                top = top + d.height + this.vgap;
            }
            c = this.getChild(SOUTH, ltr);
            if (c != null) {
                c.setSize(right - left, c.getHeight());
                Dimension d = c.getPreferredSize();
                c.setBounds(left, bottom - d.height, right - left, d.height);
                bottom = bottom - d.height - this.vgap;
            }
            c = this.getChild(EAST, ltr);
            if (c != null) {
                c.setSize(c.getWidth(), bottom - top);
                Dimension d = c.getPreferredSize();
                c.setBounds(right - d.width, top, d.width, bottom - top);
                right = right - d.width - this.hgap;
            }
            c = this.getChild(WEST, ltr);
            if (c != null) {
                c.setSize(c.getWidth(), bottom - top);
                Dimension d = c.getPreferredSize();
                c.setBounds(left, top, d.width, bottom - top);
                left = left + d.width + this.hgap;
            }
            c = this.getChild(CENTER, ltr);
            if (c != null) {
                c.setBounds(left, top, right - left, bottom - top);
            }
        }
    }

    public String toString() {
        return this.getClass().getName() + "[hgap=" + this.hgap + ",vgap=" + this.vgap + "]";
    }
}
