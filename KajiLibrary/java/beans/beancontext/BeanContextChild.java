package java.beans.beancontext;

import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

/**
 * Un bean que sabe en qué contexto vive.
 *
 * <p>La otra mitad de {@link BeanContext}: el contenedor guarda a sus hijos y el hijo guarda a su
 * contenedor. Que la relación sea de dos vías no es redundancia — es lo que le permite a un hijo
 * pedirle servicios y recursos a su entorno sin que nadie se los pase por parámetro.
 *
 * <p><strong>Un hijo puede rechazar el cambio de contexto</strong>, y ése es el motivo de que
 * {@code setBeanContext} declare {@link PropertyVetoException}: el hijo registra oyentes de veto
 * sobre la propiedad `"beanContext"` y, si alguno se opone, la mudanza no ocurre. Es la única
 * propiedad de esta API que se define vetable, y por eso las dos familias de oyentes —cambio y
 * veto— están las dos acá.
 */
public interface BeanContextChild {

    /**
     * Muda este hijo a ese contexto.
     *
     * @throws PropertyVetoException si un oyente de veto se opone; el contexto no cambia
     */
    void setBeanContext(BeanContext bc) throws PropertyVetoException;

    /** El contexto en el que vive, o `null` si todavía no está en ninguno. */
    BeanContext getBeanContext();

    /** Registra un oyente para los cambios de esa propiedad. */
    void addPropertyChangeListener(String name, PropertyChangeListener pcl);

    /** Lo quita. */
    void removePropertyChangeListener(String name, PropertyChangeListener pcl);

    /** Registra un oyente que puede **vetar** los cambios de esa propiedad. */
    void addVetoableChangeListener(String name, VetoableChangeListener vcl);

    /** Lo quita. */
    void removeVetoableChangeListener(String name, VetoableChangeListener vcl);
}
