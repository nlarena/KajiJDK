package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

/**
 * El administrador de foco de Swing anterior a Java 1.4.
 *
 * <h2>Que sigue haciendo</h2>
 *
 * <p>Poco: sus cinco metodos delegan en una {@link LayoutFocusTraversalPolicy}, que es la politica
 * que Swing usa igual sin pasar por aca. La clase se conserva porque es publica y porque codigo
 * viejo la subclasea para cambiar el orden del tabulador -- que hoy se hace poniendole una politica
 * al contenedor, sin administrador de por medio.
 *
 * <p>Ver la nota de {@link FocusManager} para el contexto completo.
 */
public class DefaultFocusManager extends FocusManager {

    /** La politica en la que delegan los cinco metodos. */
    final FocusTraversalPolicy gluePolicy = new LayoutFocusTraversalPolicy();

    /** El administrador de siempre. */
    public DefaultFocusManager() {
        setDefaultFocusTraversalPolicy(gluePolicy);
    }

    /** El componente que sigue en el recorrido. */
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        return gluePolicy.getComponentAfter(aContainer, aComponent);
    }

    /** El anterior. */
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        return gluePolicy.getComponentBefore(aContainer, aComponent);
    }

    /** El primero. */
    public Component getFirstComponent(Container aContainer) {
        return gluePolicy.getFirstComponent(aContainer);
    }

    /** El ultimo. */
    public Component getLastComponent(Container aContainer) {
        return gluePolicy.getLastComponent(aContainer);
    }

    /**
     * Si el primero va antes que el segundo en el recorrido.
     *
     * <p>Ordena por posicion, como {@link LayoutFocusTraversalPolicy}: arriba antes que abajo y, en
     * la misma fila, izquierda antes que derecha.
     */
    public boolean compareTabOrder(Component a, Component b) {
        int ay = a.getY();
        int by = b.getY();
        int ah = a.getHeight();
        int bh = b.getHeight();
        boolean mismaFila = (ay < by + bh) && (by < ay + ah);
        if (!mismaFila) {
            return ay < by;
        }
        return a.getX() < b.getX();
    }
}
