package jdk.jfr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fabrica tipos de evento en tiempo de ejecucion, sin que exista una clase Java para ellos.
 *
 * <h2>Para que sirve</h2>
 *
 * <p>Para un puente. Un motor de reglas, un lenguaje de scripting o un servidor de aplicaciones
 * sabe en tiempo de ejecucion que eventos quiere emitir, y no puede tener una clase escrita para
 * cada uno: los nombres y los campos salen de una configuracion que se lee al arrancar.
 *
 * <p>Esta fabrica arma el tipo desde {@link ValueDescriptor} y {@link AnnotationElement} —los
 * mismos metadatos que se sacarian de una clase— y genera la clase por debajo.
 *
 * <h2>Como se llenan los campos</h2>
 *
 * <p>Con {@link Event#set(int, Object)}, por indice. Un evento fabricado asi no tiene campos Java a
 * los que asignarles nada, y el indice es el de la lista de descriptores con la que se lo creo. Es
 * la razon de que {@code Event.set} exista.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Fabricar el tipo significa <strong>generar una clase</strong> y definirla en tiempo de
 * ejecucion, que necesita soporte de la VM que esta biblioteca no tiene. {@link #create} falla
 * diciendolo, en vez de devolver una fabrica que despues no fabrique eventos.
 *
 * @since 9
 */
public final class EventFactory {

    private static final String NO_HAY =
            "fabricar un tipo de evento genera y define una clase en tiempo de ejecucion, que esta "
            + "VM no soporta todavia";

    private EventFactory() {
    }

    /**
     * Una fabrica para un tipo de evento con esos campos y esas anotaciones.
     *
     * @param annotationElements las anotaciones del tipo
     * @param fields los campos
     * @return la fabrica
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public static EventFactory create(final List<AnnotationElement> annotationElements,
            final List<ValueDescriptor> fields) {
        Objects.requireNonNull(annotationElements, "annotationElements");
        Objects.requireNonNull(fields, "fields");
        // Se copian y se validan antes de fallar: si algun dia hay soporte, el error de un campo
        // repetido tiene que salir aca y no al primer evento emitido.
        final List<String> vistos = new ArrayList<String>();
        for (final ValueDescriptor v : fields) {
            if (vistos.contains(v.getName())) {
                throw new IllegalArgumentException("hay dos campos llamados " + v.getName());
            }
            vistos.add(v.getName());
        }
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Un evento nuevo de este tipo.
     *
     * @return el evento
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public Event newEvent() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * El tipo que fabrica esta fabrica.
     *
     * @return el tipo
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public EventType getEventType() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Registra el tipo, para que JFR pueda grabarlo.
     *
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public void register() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Saca el tipo del registro.
     *
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public void unregister() {
        throw new UnsupportedOperationException(NO_HAY);
    }
}
