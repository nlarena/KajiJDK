package jdk.jfr.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jdk.jfr.EventType;
import jdk.jfr.ValueDescriptor;

/**
 * Un evento leido de una grabacion.
 *
 * <p>Es un {@link RecordedObject} con cuatro accesores de mas, uno por cada campo que todo evento
 * lleva: {@link #getStartTime}, {@link #getEndTime}, {@link #getThread} y {@link #getStackTrace}.
 * Los demas campos, los que definio quien escribio el evento, se leen por nombre con los getters
 * tipados de la clase base.
 *
 * <p>{@link #getEndTime} no es un campo grabado: sale de sumar la duracion al comienzo. Es asi
 * porque en el archivo se guarda el comienzo y la duracion, y guardar ademas el final seria un dato
 * redundante en cada uno de millones de eventos.
 *
 * @since 9
 */
public final class RecordedEvent extends RecordedObject {

    private final EventType tipo;

    RecordedEvent(EventType tipo, List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
        this.tipo = tipo;
    }

    /**
     * La pila de llamadas del punto donde se emitio.
     *
     * @return la pila, o {@code null} si no se grabo
     */
    public RecordedStackTrace getStackTrace() {
        return getValue("stackTrace");
    }

    /**
     * El hilo que lo emitio.
     *
     * @return el hilo, o {@code null} si no se grabo
     */
    public RecordedThread getThread() {
        return getValue("eventThread");
    }

    /**
     * El tipo del evento.
     *
     * @return el tipo
     */
    public EventType getEventType() {
        return tipo;
    }

    /**
     * Cuando empezo.
     *
     * @return el momento
     */
    public Instant getStartTime() {
        return getInstant("startTime");
    }

    /**
     * Cuando termino.
     *
     * <p>Calculado: el comienzo mas la duracion.
     *
     * @return el momento
     */
    public Instant getEndTime() {
        return getStartTime().plus(getDuration());
    }

    /**
     * Cuanto duro.
     *
     * @return la duracion
     */
    public Duration getDuration() {
        return getDuration("duration");
    }

    /**
     * Los campos del evento.
     *
     * @return los descriptores
     */
    public List<ValueDescriptor> getFields() {
        return tipo.getFields();
    }
}
