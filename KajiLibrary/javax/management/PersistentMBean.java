package javax.management;

/**
 * Un MBean que sabe guardarse y recuperarse.
 *
 * <p>Deliberadamente no dice donde ni como: el contrato es solo "hay un lugar y estas dos
 * operaciones lo usan". Quien lo implementa elige archivo, base o lo que sea.
 */
public interface PersistentMBean {

    /**
     * Recupera el estado guardado y lo aplica.
     *
     * @throws InstanceNotFoundException si no hay nada guardado para este MBean
     */
    void load() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException;

    /** Guarda el estado actual. */
    void store() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException;
}
