package jdk.jfr;

/**
 * En que punto de su vida esta una grabacion.
 *
 * <h2>El orden es de ida y no se vuelve</h2>
 *
 * <p>{@link #NEW} a {@link #RUNNING} a {@link #STOPPED} a {@link #CLOSED}, con {@link #DELAYED}
 * como desvio cuando el arranque quedo programado para mas tarde. No hay camino de vuelta: una
 * grabacion detenida no se puede reanudar.
 *
 * <p>La razon es el formato del archivo. Una grabacion es una secuencia continua de bloques con sus
 * marcas de tiempo; reanudarla dejaria un agujero en el medio, y una herramienta que la lea no
 * tendria como distinguir ese agujero de un periodo sin actividad. Para seguir grabando se empieza
 * una grabacion nueva.
 *
 * <h2>Detenida y cerrada no es lo mismo</h2>
 *
 * <p>{@link #STOPPED} ya no graba y <strong>todavia tiene los datos</strong>: se pueden volcar a un
 * archivo o leer como flujo. {@link #CLOSED} los solto. Cerrar sin haber volcado pierde la
 * grabacion, y es el error mas comun con esta API.
 *
 * @since 9
 */
public enum RecordingState {

    /** Creada y sin arrancar. */
    NEW,

    /** Con arranque programado para un momento futuro. */
    DELAYED,

    /** Grabando. */
    RUNNING,

    /** Detenida, con los datos todavia disponibles. */
    STOPPED,

    /** Cerrada; los datos ya se soltaron. */
    CLOSED
}
