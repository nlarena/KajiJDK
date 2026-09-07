package jdk.jfr;

import java.time.Duration;

/**
 * Los ajustes de un evento dentro de una grabacion, con una interfaz encadenable.
 *
 * <h2>Por que es una clase abstracta con metodos {@code final}</h2>
 *
 * <p>Porque todos los metodos comodos —{@link #withThreshold}, {@link #withStackTrace} y
 * companeros— son lo mismo: una llamada a {@link #with} con el nombre del ajuste y su valor
 * formateado. Lo unico que cambia entre implementaciones es {@code with}, y es el unico abstracto.
 *
 * <p>Que los demas sean {@code final} no es rigidez: es lo que garantiza que
 * {@code withThreshold(Duration.ofMillis(20))} signifique exactamente
 * {@code with("threshold", "20 ms")} en cualquier implementacion. Si una subclase pudiera
 * redefinirlos, dos grabaciones configuradas igual podrian comportarse distinto.
 *
 * <h2>Se encadena y se aplica al final</h2>
 *
 * <p>Cada metodo devuelve el mismo objeto, asi que se escribe
 * {@code r.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1)).withStackTrace()}. Los ajustes
 * se van acumulando y la grabacion los toma cuando arranca.
 *
 * @since 9
 */
public abstract class EventSettings {

    /** Para las subclases. */
    protected EventSettings() {
    }

    /**
     * Graba la pila de llamadas.
     *
     * @return este mismo objeto
     */
    public final EventSettings withStackTrace() {
        return with(StackTrace.NAME, "true");
    }

    /**
     * No graba la pila de llamadas.
     *
     * @return este mismo objeto
     */
    public final EventSettings withoutStackTrace() {
        return with(StackTrace.NAME, "false");
    }

    /**
     * Saca el umbral: se graban todos los eventos, duren lo que duren.
     *
     * <p>Es {@code "0 ns"} y no una cadena vacia: el ajuste sigue existiendo con un valor que no
     * descarta nada, que es distinto de no tener ajuste.
     *
     * @return este mismo objeto
     */
    public final EventSettings withoutThreshold() {
        return with(Threshold.NAME, "0 ns");
    }

    /**
     * Cada cuanto se emite un evento periodico.
     *
     * @param duration el periodo
     * @return este mismo objeto
     * @throws NullPointerException si es {@code null}
     */
    public final EventSettings withPeriod(final Duration duration) {
        return with(Period.NAME, nanos(duration));
    }

    /**
     * La duracion minima para grabar el evento.
     *
     * @param duration el umbral
     * @return este mismo objeto
     * @throws NullPointerException si es {@code null}
     */
    public final EventSettings withThreshold(final Duration duration) {
        return with(Threshold.NAME, nanos(duration));
    }

    /**
     * En nanosegundos, que es la unidad que JFR entiende sin ambiguedad.
     *
     * <p>Se formatea aca y no en cada llamador para que el formato sea uno solo: un archivo de
     * configuracion escrito por esta API y uno escrito a mano tienen que poder leerse igual.
     */
    private static String nanos(final Duration d) {
        if (d == null) {
            throw new NullPointerException("duration");
        }
        return d.toNanos() + " ns";
    }

    /**
     * Un ajuste cualquiera, por nombre.
     *
     * <p>Es el unico metodo que una implementacion tiene que escribir, y el unico camino para los
     * ajustes propios de un evento, que por definicion no tienen un metodo comodo.
     *
     * @param name el nombre del ajuste
     * @param value el valor
     * @return este mismo objeto
     */
    public abstract EventSettings with(String name, String value);
}
