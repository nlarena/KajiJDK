package java.awt;

import java.io.Serializable;

/**
 * Pone los hijos en fila y baja de renglón cuando no entran.
 *
 * <p>Es la distribución de un párrafo, aplicada a componentes. Cada hijo queda de su tamaño
 * preferido y se van acomodando de izquierda a derecha; cuando el siguiente no entra, se corta el
 * renglón.
 *
 * <p>{@link #LEADING} y {@link #TRAILING} no son sinónimos de izquierda y derecha: siguen la
 * **orientación del contenedor**, así que en un texto que se lee de derecha a izquierda se dan
 * vuelta solos. Es la diferencia entre una interfaz que se traduce bien y una que hay que rehacer.
 *
 * <p>La alineación por línea de base junta los hijos por el renglón del texto y no por el borde de
 * arriba, que es lo que hace que una etiqueta al lado de un campo se vea alineada de verdad.
 */
public class FlowLayout implements LayoutManager, Serializable {

    private static final long serialVersionUID = -7262534875583282631L;

    /** Pegados a la izquierda. */
    public static final int LEFT = 0;

    /** Centrados. */
    public static final int CENTER = 1;

    /** Pegados a la derecha. */
    public static final int RIGHT = 2;

    /** Pegados al lado por donde empieza el texto. */
    public static final int LEADING = 3;

    /** Pegados al lado por donde termina. */
    public static final int TRAILING = 4;

    private int align;
    private int hgap;
    private int vgap;
    private boolean alignOnBaseline;

    /** Centrados, con cinco píxeles de separación. */
    public FlowLayout() {
        this(CENTER, 5, 5);
    }

    /** Con esa alineación y cinco píxeles de separación. */
    public FlowLayout(int align) {
        this(align, 5, 5);
    }

    /** Con alineación y separaciones dadas. */
    public FlowLayout(int align, int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
        this.setAlignment(align);
    }

    /** Cómo se alinean dentro del renglón. */
    public int getAlignment() {
        return this.align;
    }

    /** Cambia la alineación; un valor que no sea una de las cinco se toma como centrado. */
    public void setAlignment(int align) {
        if (align < LEFT || align > TRAILING) {
            this.align = CENTER;
        } else {
            this.align = align;
        }
    }

    /** Cuánto se separan horizontalmente. */
    public int getHgap() {
        return this.hgap;
    }

    /** Cambia la separación horizontal. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** Cuánto se separan verticalmente. */
    public int getVgap() {
        return this.vgap;
    }

    /** Cambia la separación vertical. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /** Declara si se alinean por la línea de base del texto. */
    public void setAlignOnBaseline(boolean alignOnBaseline) {
        this.alignOnBaseline = alignOnBaseline;
    }

    /** Si se alinean por la línea de base. */
    public boolean getAlignOnBaseline() {
        return this.alignOnBaseline;
    }

    /** No hace nada: esta distribución no guarda nada por hijo. */
    public void addLayoutComponent(String name, Component comp) {
    }

    /** No hace nada, por el mismo motivo. */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Lo que el contenedor necesita para poner todo **en un solo renglón**.
     *
     * <p>Es a propósito: la medida preferida de una fila es la de la fila entera. Si no entra, ahí
     * recién se corta, pero preferir un tamaño ya cortado dejaría al contenedor sin margen para
     * crecer.
     */
    public Dimension preferredLayoutSize(Container target) {
        return this.medir(target, true);
    }

    /** Lo mínimo, con cada hijo en su medida mínima. */
    public Dimension minimumLayoutSize(Container target) {
        return this.medir(target, false);
    }

    /** La suma de los anchos y el alto del más alto, más las separaciones y los márgenes. */
    private Dimension medir(Container target, boolean preferida) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int n = target.getComponentCount();
            boolean primero = true;
            for (int i = 0; i < n; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = preferida ? m.getPreferredSize() : m.getMinimumSize();
                dim.height = Math.max(dim.height, d.height);
                if (!primero) {
                    dim.width = dim.width + this.hgap;
                }
                dim.width = dim.width + d.width;
                primero = false;
            }
            Insets insets = target.getInsets();
            dim.width = dim.width + insets.left + insets.right + this.hgap * 2;
            dim.height = dim.height + insets.top + insets.bottom + this.vgap * 2;
            return dim;
        }
    }

    /**
     * Acomoda los hijos en renglones.
     *
     * <p>Cada renglón se acomoda **cuando se cierra**, no mientras se llena: hasta no saber cuántos
     * entran no se sabe cuánto espacio sobra, y sin eso no se puede centrar ni alinear a la derecha.
     */
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxwidth = target.getWidth() - (insets.left + insets.right + this.hgap * 2);
            int n = target.getComponentCount();
            int x = 0;
            int y = insets.top + this.vgap;
            int rowh = 0;
            int start = 0;
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            for (int i = 0; i < n; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = m.getPreferredSize();
                m.setSize(d.width, d.height);
                if (x == 0 || x + d.width <= maxwidth) {
                    if (x > 0) {
                        x = x + this.hgap;
                    }
                    x = x + d.width;
                    rowh = Math.max(rowh, d.height);
                } else {
                    this.acomodarFila(target, start, i, maxwidth - x, y, rowh, ltr);
                    x = d.width;
                    y = y + this.vgap + rowh;
                    rowh = d.height;
                    start = i;
                }
            }
            this.acomodarFila(target, start, n, maxwidth - x, y, rowh, ltr);
        }
    }

    /** Ubica los hijos de un renglón ya cerrado, repartiendo el espacio que sobró. */
    private void acomodarFila(Container target, int rowStart, int rowEnd, int sobra, int y,
            int height, boolean ltr) {
        int a = this.align;
        // LEADING y TRAILING se resuelven a izquierda o derecha segun la orientacion: es acá donde
        // una interfaz en árabe se da vuelta sola.
        if (a == LEADING) {
            a = ltr ? LEFT : RIGHT;
        } else if (a == TRAILING) {
            a = ltr ? RIGHT : LEFT;
        }
        Insets insets = target.getInsets();
        int x = insets.left + this.hgap;
        if (a == CENTER) {
            x = x + sobra / 2;
        } else if (a == RIGHT) {
            x = x + sobra;
        }
        for (int i = rowStart; i < rowEnd; i++) {
            Component m = target.getComponent(i);
            if (!m.isVisible()) {
                continue;
            }
            int cy = y + (height - m.getHeight()) / 2;
            m.setLocation(x, cy);
            x = x + m.getWidth() + this.hgap;
        }
    }

    public String toString() {
        String s;
        if (this.align == LEFT) {
            s = ",align=left";
        } else if (this.align == CENTER) {
            s = ",align=center";
        } else if (this.align == RIGHT) {
            s = ",align=right";
        } else if (this.align == LEADING) {
            s = ",align=leading";
        } else {
            s = ",align=trailing";
        }
        return this.getClass().getName() + "[hgap=" + this.hgap + ",vgap=" + this.vgap + s + "]";
    }
}
