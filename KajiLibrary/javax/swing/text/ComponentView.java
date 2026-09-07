package javax.swing.text;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * Un componente de verdad incrustado en el texto: un boton, un campo, lo que sea.
 *
 * <h2>La vista no lo dibuja</h2>
 *
 * <p>El componente se agrega al contenedor del editor y se dibuja solo, como cualquier hijo. Esta
 * vista solo lo <em>ubica</em>: le pone el tamano y la posicion que le tocan en el texto. De ahi
 * que {@link #paint} no pinte nada mas que acomodarlo.
 *
 * <p>Eso trae una consecuencia que conviene saber: el componente sigue existiendo aunque su tramo
 * de texto quede fuera de la vista, y su tamano preferido manda sobre el maquetado del parrafo.
 */
public class ComponentView extends View {

    private Component createdC;
    private Invalidator c;

    public ComponentView(Element elem) {
        super(elem);
    }

    /**
     * El componente a mostrar; el que el elemento tiene como atributo.
     *
     * <p>Una subclase puede fabricarlo en vez de tomarlo del atributo: es como se incrusta algo
     * que no estaba en el documento.
     */
    protected Component createComponent() {
        AttributeSet attr = getElement().getAttributes();
        Component comp = StyleConstants.getComponent(attr);
        return comp;
    }

    public final Component getComponent() {
        return createdC;
    }

    /** No dibuja: acomoda. Ver la nota de la clase. */
    public void paint(Graphics g, Shape a) {
        if (c != null) {
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            c.setBounds(alloc.x, alloc.y, alloc.width, alloc.height);
        }
    }

    public float getPreferredSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getPreferredSize();
            if (axis == View.X_AXIS) {
                return size.width;
            }
            return size.height;
        }
        return 0;
    }

    public float getMinimumSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getMinimumSize();
            if (axis == View.X_AXIS) {
                return size.width;
            }
            return size.height;
        }
        return 0;
    }

    public float getMaximumSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getMaximumSize();
            if (axis == View.X_AXIS) {
                return size.width;
            }
            return size.height;
        }
        return 0;
    }

    /** Se alinea al medio, como cualquier vista sin linea de base propia. */
    public float getAlignment(int axis) {
        return super.getAlignment(axis);
    }

    /**
     * Al entrar en el arbol, agrega el componente al editor; al salir, lo saca.
     *
     * <p>Es el unico lugar donde una vista toca la jerarquia de componentes, y por eso esta
     * cuidadosamente atado al ciclo de vida de la vista.
     */
    public void setParent(View p) {
        super.setParent(p);
        if (p != null) {
            Container host = getContainer();
            if (host != null) {
                if (createdC == null) {
                    createdC = createComponent();
                    if (createdC != null) {
                        c = new Invalidator(createdC, this);
                        host.add(createdC);
                    }
                }
            }
        } else {
            if (c != null) {
                Container host = c.getParent();
                if (host != null) {
                    host.remove(c.getComponente());
                }
                c = null;
                createdC = null;
            }
        }
    }

    /** Acomoda el componente dentro del editor. */
    void setComponentParent() {
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            Rectangle r = a.getBounds();
            if (pos == p1) {
                r.x = r.x + r.width;
            }
            r.width = 0;
            return r;
        }
        throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) a;
        if (x < alloc.x + (alloc.width / 2)) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }

    /**
     * Envuelve al componente para saber cuando cambia de tamano.
     *
     * <p>Un componente incrustado que cambia de tamano tiene que hacer que el parrafo se rehaga, y
     * la unica forma de enterarse es escucharlo.
     */
    static class Invalidator {

        private final Component comp;
        private final ComponentView vista;

        Invalidator(Component comp, ComponentView vista) {
            this.comp = comp;
            this.vista = vista;
        }

        Component getComponente() {
            return comp;
        }

        Container getParent() {
            return comp.getParent();
        }

        Dimension getPreferredSize() {
            return comp.getPreferredSize();
        }

        Dimension getMinimumSize() {
            return comp.getMinimumSize();
        }

        Dimension getMaximumSize() {
            return comp.getMaximumSize();
        }

        void setBounds(int x, int y, int w, int h) {
            comp.setBounds(x, y, w, h);
        }
    }
}
