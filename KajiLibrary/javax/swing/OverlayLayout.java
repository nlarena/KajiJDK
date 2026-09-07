package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.Serializable;

/**
 * Acomoda a todos los hijos <em>uno encima de otro</em>, alineados por sus puntos de alineacion.
 *
 * <h2>No es "todos en la misma posicion"</h2>
 *
 * <p>La diferencia esta en la alineacion. Cada componente tiene un {@code alignmentX} y un
 * {@code alignmentY} entre cero y uno, que dicen que punto de si mismo quiere alinear. Con
 * {@code 0.5} se alinea por el centro, con {@code 0} por el borde izquierdo o de arriba, con
 * {@code 1} por el derecho o de abajo. Este acomodador busca un punto comun y hace que el punto de
 * alineacion de cada hijo caiga ahi.
 *
 * <p>Por eso dos hijos de distinto tamano con la misma alineacion quedan centrados uno sobre otro,
 * y con alineaciones distintas quedan corridos. Con todos en {@code 0} y del mismo tamano queda el
 * caso trivial, que es lo que la mayoria espera y no es lo que la clase hace en general.
 *
 * <h2>El orden de dibujo lo decide el contenedor</h2>
 *
 * <p>Este acomodador solo pone posiciones y tamanos. Cual se ve arriba lo decide el orden de los
 * hijos, que es del contenedor: el primero se dibuja ultimo y por lo tanto queda encima.
 *
 * <h2>Las medidas se guardan y hay que invalidarlas</h2>
 *
 * <p>Medir a todos los hijos es caro, asi que el resultado se guarda. {@link #invalidateLayout} lo
 * tira; el contenedor la llama sola cuando algo cambia.
 */
public class OverlayLayout implements LayoutManager2, Serializable {

    private final Container target;

    private transient SizeRequirements[] xChildren;
    private transient SizeRequirements[] yChildren;
    private transient SizeRequirements xTotal;
    private transient SizeRequirements yTotal;

    /**
     * Para ese contenedor.
     *
     * <p>Se le pasa el contenedor en el constructor y despues no se lo puede cambiar: es lo mismo
     * que hace {@link BoxLayout}, y por eso un acomodador de estos no se comparte entre dos
     * contenedores.
     */
    public OverlayLayout(Container target) {
        this.target = target;
    }

    /** El contenedor al que esta atado. */
    public final Container getTarget() {
        return this.target;
    }

    /** Tira las medidas guardadas; ver la nota de la clase. */
    public void invalidateLayout(Container target) {
        checkContainer(target);
        xChildren = null;
        yChildren = null;
        xTotal = null;
        yTotal = null;
    }

    /** No hace nada: este acomodador no usa nombres. */
    public void addLayoutComponent(String name, Component comp) {
        invalidateLayout(comp.getParent());
    }

    public void removeLayoutComponent(Component comp) {
        invalidateLayout(comp.getParent());
    }

    /** No hace nada: este acomodador no usa restricciones. */
    public void addLayoutComponent(Component comp, Object constraints) {
        invalidateLayout(comp.getParent());
    }

    /**
     * Lo que el contenedor querria medir.
     *
     * @throws AWTError si no es el contenedor al que esta atado
     */
    public Dimension preferredLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();
        Dimension size = new Dimension(xTotal.preferred, yTotal.preferred);
        Insets insets = target.getInsets();
        size.width = size.width + insets.left + insets.right;
        size.height = size.height + insets.top + insets.bottom;
        return size;
    }

    /**
     * Lo minimo con lo que se arregla.
     *
     * @throws AWTError si no es el contenedor al que esta atado
     */
    public Dimension minimumLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();
        Dimension size = new Dimension(xTotal.minimum, yTotal.minimum);
        Insets insets = target.getInsets();
        size.width = size.width + insets.left + insets.right;
        size.height = size.height + insets.top + insets.bottom;
        return size;
    }

    /**
     * Lo maximo que puede ocupar.
     *
     * @throws AWTError si no es el contenedor al que esta atado
     */
    public Dimension maximumLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();
        Dimension size = new Dimension(xTotal.maximum, yTotal.maximum);
        Insets insets = target.getInsets();
        size.width = size.width + insets.left + insets.right;
        size.height = size.height + insets.top + insets.bottom;
        return size;
    }

    /**
     * La alineacion horizontal del conjunto.
     *
     * @throws AWTError si no es el contenedor al que esta atado
     */
    public float getLayoutAlignmentX(Container target) {
        checkContainer(target);
        checkRequests();
        return xTotal.alignment;
    }

    /**
     * La alineacion vertical del conjunto.
     *
     * @throws AWTError si no es el contenedor al que esta atado
     */
    public float getLayoutAlignmentY(Container target) {
        checkContainer(target);
        checkRequests();
        return yTotal.alignment;
    }

    /**
     * Pone a cada hijo con su punto de alineacion sobre el punto comun.
     *
     * @throws AWTError si no es el contenedor al que esta atado
     */
    public void layoutContainer(Container target) {
        checkContainer(target);
        checkRequests();
        int nChildren = target.getComponentCount();
        int[] xOffsets = new int[nChildren];
        int[] xSpans = new int[nChildren];
        int[] yOffsets = new int[nChildren];
        int[] ySpans = new int[nChildren];
        Dimension alloc = target.getSize();
        Insets in = target.getInsets();
        alloc.width = alloc.width - (in.left + in.right);
        alloc.height = alloc.height - (in.top + in.bottom);
        SizeRequirements.calculateAlignedPositions(alloc.width, xTotal, xChildren, xOffsets,
                xSpans);
        SizeRequirements.calculateAlignedPositions(alloc.height, yTotal, yChildren, yOffsets,
                ySpans);
        for (int i = 0; i < nChildren; i++) {
            Component c = target.getComponent(i);
            c.setBounds(in.left + xOffsets[i], in.top + yOffsets[i], xSpans[i], ySpans[i]);
        }
    }

    /**
     * @throws AWTError si no es el contenedor al que esta atado
     */
    void checkContainer(Container target) {
        if (this.target != target) {
            throw new java.awt.AWTError("OverlayLayout can't be shared");
        }
    }

    /** Vuelve a medir a los hijos si hace falta; ver la nota de la clase. */
    void checkRequests() {
        if (xChildren == null || yChildren == null) {
            int n = target.getComponentCount();
            xChildren = new SizeRequirements[n];
            yChildren = new SizeRequirements[n];
            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                Dimension min = c.getMinimumSize();
                Dimension typ = c.getPreferredSize();
                Dimension max = c.getMaximumSize();
                xChildren[i] = new SizeRequirements(min.width, typ.width, max.width,
                        c.getAlignmentX());
                yChildren[i] = new SizeRequirements(min.height, typ.height, max.height,
                        c.getAlignmentY());
            }
            xTotal = SizeRequirements.getAlignedSizeRequirements(xChildren);
            yTotal = SizeRequirements.getAlignedSizeRequirements(yChildren);
        }
    }
}
