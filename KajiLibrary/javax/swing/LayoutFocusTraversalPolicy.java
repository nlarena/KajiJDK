package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.io.Serializable;
import java.util.Comparator;

/**
 * La politica de foco que usa Swing: recorre en el orden en que se ve.
 *
 * <h2>Arriba a abajo, izquierda a derecha</h2>
 *
 * <p>Es lo unico que agrega sobre {@link SortingFocusTraversalPolicy}: el comparador. Se ordena por
 * la coordenada vertical y, dentro de la misma fila, por la horizontal. Es como se lee, y por eso
 * coincide con lo que el usuario espera del tabulador sin que nadie configure nada.
 *
 * <p>"La misma fila" no es "la misma coordenada exacta": dos campos alineados pueden estar a un
 * pixel de distancia por sus bordes. Se los considera de la misma fila cuando se superponen
 * verticalmente, que es lo que hace que un boton mas alto al lado de un campo no se salga de orden.
 *
 * <h2>Ademas filtra un poco mas</h2>
 *
 * <p>{@link #accept} saltea tambien los componentes de texto de solo lectura: se pueden enfocar,
 * pero no hay nada que hacer ahi con el teclado, y detenerse en ellos molesta mas de lo que ayuda.
 */
public class LayoutFocusTraversalPolicy extends SortingFocusTraversalPolicy
        implements Serializable {

    /** Con el orden de lectura. */
    public LayoutFocusTraversalPolicy() {
        super(new PorPosicion());
    }

    /** Con otro criterio; solo para las subclases de la biblioteca. */
    LayoutFocusTraversalPolicy(Comparator<? super Component> c) {
        super(c);
    }

    public Component getComponentAfter(Container aContainer, Component aComponent) {
        return super.getComponentAfter(aContainer, aComponent);
    }

    public Component getComponentBefore(Container aContainer, Component aComponent) {
        return super.getComponentBefore(aContainer, aComponent);
    }

    public Component getFirstComponent(Container aContainer) {
        return super.getFirstComponent(aContainer);
    }

    public Component getLastComponent(Container aContainer) {
        return super.getLastComponent(aContainer);
    }

    /**
     * Ademas de lo que pide la clase de arriba, saltea el texto de solo lectura.
     *
     * <p>Ver la nota de la clase.
     */
    protected boolean accept(Component aComponent) {
        if (!super.accept(aComponent)) {
            return false;
        }
        if (aComponent instanceof javax.swing.text.JTextComponent) {
            javax.swing.text.JTextComponent t = (javax.swing.text.JTextComponent) aComponent;
            if (!t.isEditable()) {
                return false;
            }
        }
        return true;
    }

    /** Arriba a abajo, y dentro de la misma fila, izquierda a derecha. */
    private static class PorPosicion implements Comparator<Component>, Serializable {

        public int compare(Component a, Component b) {
            if (a == b) {
                return 0;
            }
            int ay = a.getY();
            int by = b.getY();
            int ah = a.getHeight();
            int bh = b.getHeight();
            // Se superponen verticalmente: misma fila. Ver la nota de la clase.
            boolean mismaFila = (ay < by + bh) && (by < ay + ah);
            if (!mismaFila) {
                return (ay < by) ? -1 : 1;
            }
            int ax = a.getX();
            int bx = b.getX();
            if (ax != bx) {
                return (ax < bx) ? -1 : 1;
            }
            if (ay != by) {
                return (ay < by) ? -1 : 1;
            }
            // Misma posicion exacta: se desempata por identidad para que el orden sea total y
            // estable. Sin esto, dos componentes superpuestos podrian intercambiarse entre dos
            // recorridos y el tabulador se volveria impredecible.
            return (System.identityHashCode(a) < System.identityHashCode(b)) ? -1 : 1;
        }
    }
}
