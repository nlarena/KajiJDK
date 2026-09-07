package jdk.jfr.consumer;

import java.util.Collections;
import java.util.List;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;

/**
 * El aviso de que los metadatos de un flujo de eventos cambiaron.
 *
 * <h2>Por que los metadatos cambian a mitad de un flujo</h2>
 *
 * <p>Porque las clases se cargan cuando hacen falta. Un evento definido en una biblioteca que se
 * carga a los diez minutos de arrancar no existe en los metadatos iniciales, y sin este aviso quien
 * consume el flujo no tendria como enterarse de que apareció un tipo nuevo.
 *
 * <p>Tambien pueden desaparecer, si el codigo que los definia se descarga.
 *
 * <p>{@link #getAddedEventTypes} y {@link #getRemovedEventTypes} son el delta;
 * {@link #getEventTypes} es el estado completo despues del cambio. El delta es lo que sirve para
 * reaccionar, el estado completo para el que se conecta a mitad de camino.
 *
 * @since 16
 */
public final class MetadataEvent {

    private final List<EventType> todos;
    private final List<EventType> agregados;
    private final List<EventType> quitados;
    private final List<Configuration> configuraciones;

    MetadataEvent(List<EventType> todos, List<EventType> agregados, List<EventType> quitados,
            List<Configuration> configuraciones) {
        this.todos = Collections.unmodifiableList(todos);
        this.agregados = Collections.unmodifiableList(agregados);
        this.quitados = Collections.unmodifiableList(quitados);
        this.configuraciones = Collections.unmodifiableList(configuraciones);
    }

    /**
     * Todos los tipos de evento que hay despues del cambio.
     *
     * @return los tipos
     */
    public final List<EventType> getEventTypes() {
        return todos;
    }

    /**
     * Los tipos que aparecieron.
     *
     * @return los tipos agregados
     */
    public final List<EventType> getAddedEventTypes() {
        return agregados;
    }

    /**
     * Los tipos que desaparecieron.
     *
     * @return los tipos quitados
     */
    public final List<EventType> getRemovedEventTypes() {
        return quitados;
    }

    /**
     * Las configuraciones disponibles.
     *
     * @return las configuraciones
     */
    public List<Configuration> getConfigurations() {
        return configuraciones;
    }
}
