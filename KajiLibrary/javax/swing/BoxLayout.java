package javax.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.PrintStream;
import java.io.Serializable;

/**
 * Acomoda a los hijos en una sola fila o en una sola columna.
 *
 * <h2>Un eje se reparte, el otro se alinea</h2>
 *
 * <p>Toda la clase es eso. Sobre el eje elegido los hijos van uno detras de otro y se reparten el
 * largo con {@link SizeRequirements#calculateTiledPositions}; sobre el eje perpendicular se
 * encabalgan y se alinean con {@link SizeRequirements#calculateAlignedPositions}, cada uno por su
 * {@code alignmentX} o {@code alignmentY}. De ahi salen las dos sorpresas clasicas: un hijo puede
 * quedar mas ancho de lo que pidio, porque su maximo se lo permitia, y una columna de botones
 * queda centrada porque la alineacion por omision es 0.5.
 *
 * <h2>Ejes absolutos y ejes del idioma</h2>
 *
 * <p>{@link #X_AXIS} y {@link #Y_AXIS} son direcciones fijas. {@link #LINE_AXIS} y
 * {@link #PAGE_AXIS} son "el sentido en el que avanza una linea" y "el sentido en el que avanzan
 * las lineas", que dependen de la orientacion del contenedor: en un idioma que se lee de derecha a
 * izquierda, una caja de eje de linea llena desde la derecha. Ese es el unico lugar donde esta
 * clase mira la orientacion.
 *
 * <h2>Un layout por contenedor</h2>
 *
 * <p>El contenedor se pasa al construir y no se puede cambiar: la clase guarda los pedidos de los
 * hijos entre llamadas, y compartirla seria mezclar los de dos contenedores. Pedirle que acomode
 * otro es un {@link AWTError}, no una excepcion: es un error de programa, no una condicion que un
 * programa pueda manejar.
 */
public class BoxLayout implements LayoutManager2, Serializable {

    /** De izquierda a derecha. */
    public static final int X_AXIS = 0;

    /** De arriba hacia abajo. */
    public static final int Y_AXIS = 1;

    /** El sentido en el que avanza una linea de texto. */
    public static final int LINE_AXIS = 2;

    /** El sentido en el que se apilan las lineas. */
    public static final int PAGE_AXIS = 3;

    private int axis;
    private Container target;

    private transient SizeRequirements[] xChildren;
    private transient SizeRequirements[] yChildren;
    private transient SizeRequirements xTotal;
    private transient SizeRequirements yTotal;

    private transient PrintStream dbg;

    /** Acomoda a los hijos de ese contenedor sobre ese eje. */
    public BoxLayout(Container target, int axis) {
        if (axis != X_AXIS && axis != Y_AXIS && axis != LINE_AXIS && axis != PAGE_AXIS) {
            throw new AWTError("Invalid axis");
        }
        this.axis = axis;
        this.target = target;
    }

    /**
     * Como el otro constructor, ademas escribiendo lo que decide en ese flujo.
     *
     * @deprecated es de depuracion; el JDK lo dejo por compatibilidad.
     */
    @Deprecated
    BoxLayout(Container target, int axis, PrintStream dbg) {
        this(target, axis);
        this.dbg = dbg;
    }

    /** El contenedor que acomoda. */
    public final Container getTarget() {
        return this.target;
    }

    /** El eje tal como se pidio, sin resolver contra la orientacion. */
    public final int getAxis() {
        return this.axis;
    }

    /** Olvida los pedidos guardados: algo cambio y hay que volver a preguntar. */
    public synchronized void invalidateLayout(Container target) {
        checkContainer(target);
        xChildren = null;
        yChildren = null;
        xTotal = null;
        yTotal = null;
    }

    /** Nada: esta distribucion no usa nombres. */
    public void addLayoutComponent(String name, Component comp) {
        invalidateLayout(comp.getParent());
    }

    public void removeLayoutComponent(Component comp) {
        invalidateLayout(comp.getParent());
    }

    /** Nada: esta distribucion no usa restricciones. */
    public void addLayoutComponent(Component comp, Object constraints) {
        invalidateLayout(comp.getParent());
    }

    public Dimension preferredLayoutSize(Container target) {
        Dimension size;
        synchronized (this) {
            checkContainer(target);
            checkRequests();
            size = new Dimension(xTotal.preferred, yTotal.preferred);
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right,
                Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom,
                Integer.MAX_VALUE);
        return size;
    }

    public Dimension minimumLayoutSize(Container target) {
        Dimension size;
        synchronized (this) {
            checkContainer(target);
            checkRequests();
            size = new Dimension(xTotal.minimum, yTotal.minimum);
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right,
                Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom,
                Integer.MAX_VALUE);
        return size;
    }

    public Dimension maximumLayoutSize(Container target) {
        Dimension size;
        synchronized (this) {
            checkContainer(target);
            checkRequests();
            size = new Dimension(xTotal.maximum, yTotal.maximum);
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right,
                Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom,
                Integer.MAX_VALUE);
        return size;
    }

    public synchronized float getLayoutAlignmentX(Container target) {
        checkContainer(target);
        checkRequests();
        return xTotal.alignment;
    }

    public synchronized float getLayoutAlignmentY(Container target) {
        checkContainer(target);
        checkRequests();
        return yTotal.alignment;
    }

    /** Ubica a los hijos; ver la nota de la clase. */
    public void layoutContainer(Container target) {
        checkContainer(target);
        int nChildren = target.getComponentCount();
        int[] xOffsets = new int[nChildren];
        int[] xSpans = new int[nChildren];
        int[] yOffsets = new int[nChildren];
        int[] ySpans = new int[nChildren];

        Dimension alloc = target.getSize();
        Insets in = target.getInsets();
        alloc.width = alloc.width - (in.left + in.right);
        alloc.height = alloc.height - (in.top + in.bottom);

        ComponentOrientation o = target.getComponentOrientation();
        int absoluteAxis = resolveAxis(axis, o);
        boolean ltr = (absoluteAxis != axis) ? o.isLeftToRight() : true;

        synchronized (this) {
            checkRequests();

            if (absoluteAxis == X_AXIS) {
                SizeRequirements.calculateTiledPositions(alloc.width, xTotal, xChildren, xOffsets,
                        xSpans, ltr);
                SizeRequirements.calculateAlignedPositions(alloc.height, yTotal, yChildren,
                        yOffsets, ySpans);
            } else {
                SizeRequirements.calculateAlignedPositions(alloc.width, xTotal, xChildren,
                        xOffsets, xSpans, ltr);
                SizeRequirements.calculateTiledPositions(alloc.height, yTotal, yChildren, yOffsets,
                        ySpans);
            }
        }

        for (int i = 0; i < nChildren; i++) {
            Component c = target.getComponent(i);
            c.setBounds((int) Math.min((long) in.left + (long) xOffsets[i], Integer.MAX_VALUE),
                    (int) Math.min((long) in.top + (long) yOffsets[i], Integer.MAX_VALUE),
                    xSpans[i], ySpans[i]);
        }
        if (dbg != null) {
            for (int i = 0; i < nChildren; i++) {
                Component c = target.getComponent(i);
                dbg.println(c.toString());
            }
        }
    }

    /** Un {@link AWTError} si no es su contenedor; ver la nota de la clase. */
    void checkContainer(Container target) {
        if (this.target != target) {
            throw new AWTError("BoxLayout can't be shared");
        }
    }

    /**
     * Vuelve a preguntarle a cada hijo cuanto quiere medir, si hace falta.
     *
     * <p>Un hijo invisible pide cero en todo pero conserva su alineacion: ocupa un lugar en los
     * arreglos, para que los indices sigan siendo los del contenedor, pero no ocupa espacio.
     */
    void checkRequests() {
        if (xChildren == null || yChildren == null) {
            int n = target.getComponentCount();
            xChildren = new SizeRequirements[n];
            yChildren = new SizeRequirements[n];
            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                if (!c.isVisible()) {
                    xChildren[i] = new SizeRequirements(0, 0, 0, c.getAlignmentX());
                    yChildren[i] = new SizeRequirements(0, 0, 0, c.getAlignmentY());
                    continue;
                }
                Dimension min = c.getMinimumSize();
                Dimension typ = c.getPreferredSize();
                Dimension max = c.getMaximumSize();
                xChildren[i] = new SizeRequirements(min.width, typ.width, max.width,
                        c.getAlignmentX());
                yChildren[i] = new SizeRequirements(min.height, typ.height, max.height,
                        c.getAlignmentY());
            }

            int absoluteAxis = resolveAxis(axis, target.getComponentOrientation());
            if (absoluteAxis == X_AXIS) {
                xTotal = SizeRequirements.getTiledSizeRequirements(xChildren);
                yTotal = SizeRequirements.getAlignedSizeRequirements(yChildren);
            } else {
                xTotal = SizeRequirements.getAlignedSizeRequirements(xChildren);
                yTotal = SizeRequirements.getTiledSizeRequirements(yChildren);
            }
        }
    }

    /** El eje del idioma llevado a un eje fijo; ver la nota de la clase. */
    private int resolveAxis(int axis, ComponentOrientation o) {
        int absoluteAxis;
        if (axis == LINE_AXIS) {
            absoluteAxis = o.isHorizontal() ? X_AXIS : Y_AXIS;
        } else if (axis == PAGE_AXIS) {
            absoluteAxis = o.isHorizontal() ? Y_AXIS : X_AXIS;
        } else {
            absoluteAxis = axis;
        }
        return absoluteAxis;
    }
}
