package javax.swing.event;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;

/**
 * El {@link PropertyChangeSupport} de JavaBeans, con la regla del hilo de Swing.
 *
 * <h2>Que agrega</h2>
 *
 * <p>Swing tiene una regla dura: todo lo que toca la interfaz corre en el hilo despachador de
 * eventos. Un bean que cambia una propiedad desde un hilo de trabajo y avisa directamente haria que
 * un oyente actualice un componente desde el hilo equivocado — que no falla enseguida, falla
 * despues y en otro lado.
 *
 * <p>Con {@link #isNotifyOnEDT} en {@code true} el aviso se reencola en el hilo correcto. La bandera
 * arranca apagada por compatibilidad: esta clase es anterior a la regla.
 *
 * <h2>Lo que esta VM no hace</h2>
 *
 * <p>Reencolar necesita el hilo despachador, que lo provee el sistema de ventanas. Esta VM no lo
 * tiene, asi que el aviso sale en el hilo que llamo, como si la bandera estuviera apagada. La
 * bandera se guarda y se reporta con fidelidad; lo que no ocurre es el salto de hilo, y queda dicho
 * aca en vez de aparentar una garantia que no se cumple.
 */
public final class SwingPropertyChangeSupport extends PropertyChangeSupport {

    private static final long serialVersionUID = 7162625831330845068L;

    private final boolean notifyOnEDT;

    /** Sin reencolado, que es el comportamiento historico. */
    public SwingPropertyChangeSupport(Object sourceBean) {
        this(sourceBean, false);
    }

    /** Eligiendo si los avisos se reencolan al hilo de la interfaz. */
    public SwingPropertyChangeSupport(Object sourceBean, boolean notifyOnEDT) {
        super(sourceBean);
        this.notifyOnEDT = notifyOnEDT;
    }

    /** Reparte el aviso; ver la nota de la clase sobre el hilo. */
    public void firePropertyChange(PropertyChangeEvent evt) {
        super.firePropertyChange(evt);
    }

    /** Si los avisos deberian reencolarse al hilo de la interfaz. */
    public boolean isNotifyOnEDT() {
        return this.notifyOnEDT;
    }
}
