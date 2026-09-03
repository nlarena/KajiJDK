package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.EventTarget -- algo que puede recibir eventos.
 *
 * <p>La implementa {@code Node}, asi que cualquier nodo del documento lo es.
 *
 * <h2>useCapture es parte de la identidad del registro</h2>
 *
 * <p>Es lo que no es obvio: registrar el mismo escucha para el mismo tipo con {@code useCapture}
 * distinto son <b>dos registros</b>, y quitar uno no quita el otro. {@link #removeEventListener}
 * tiene que recibir el mismo valor con el que se registro, o no encuentra nada y no avisa.
 *
 * <p>Registrar dos veces con los tres argumentos iguales, en cambio, <b>no</b> duplica: el segundo se
 * descarta y el escucha recibe el evento una sola vez.
 */
public interface EventTarget {

    /**
     * Registra un escucha.
     *
     * @param useCapture si escucha en la fase de captura --bajando-- en vez de en objetivo y
     *     burbujeo. Ver la nota de la clase: es parte de la identidad del registro
     */
    void addEventListener(String type, EventListener listener, boolean useCapture);

    /**
     * Quita un escucha. Si no hay ninguno con esos tres valores, no hace nada y no avisa.
     */
    void removeEventListener(String type, EventListener listener, boolean useCapture);

    /**
     * Despacha un evento por este objetivo, con las tres fases completas.
     *
     * @return si <b>no</b> se cancelo la accion por omision. Ojo con el sentido: devuelve true
     *     cuando nadie llamo a {@code preventDefault()}
     * @throws EventException {@code UNSPECIFIED_EVENT_TYPE_ERR} si el evento no tiene tipo
     */
    boolean dispatchEvent(Event evt) throws EventException;
}
