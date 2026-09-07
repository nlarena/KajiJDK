package jdk.jfr;

/**
 * La clase de la que hereda todo evento propio de JFR.
 *
 * <h2>Como se usa</h2>
 *
 * <p>Se hereda, se le agregan campos publicos —cada campo es un dato del evento— y se emite:
 *
 * <pre>{@code
 * class Pedido extends Event {
 *     @Label("Ruta") String ruta;
 *     @Label("Bytes") @DataAmount long bytes;
 * }
 *
 * Pedido e = new Pedido();
 * e.begin();
 * ... hacer el trabajo ...
 * e.ruta = ruta;
 * e.end();
 * if (e.shouldCommit()) { e.commit(); }
 * }</pre>
 *
 * <h2>Por que los metodos estan vacios</h2>
 *
 * <p>Porque asi estan en el JDK, y no es una omision: la VM <strong>reescribe el bytecode</strong>
 * de cada subclase al cargarla, reemplazando estas llamadas por el codigo que escribe el evento en
 * el buffer. El metodo que se ve aca nunca corre.
 *
 * <p>Eso explica dos cosas que de otro modo son raras. Que todos sean {@code final}: si una
 * subclase los redefiniera, la reescritura no tendria donde engancharse. Y que un evento en un
 * programa sin JFR activo cueste literalmente nada — los cuerpos vacios se eliminan en linea y no
 * queda ni la llamada.
 *
 * <p>En esta biblioteca las clases son las mismas y la reescritura no ocurre, asi que los eventos
 * no se graban. {@link #isEnabled} y {@link #shouldCommit} contestan {@code false}, que es la
 * respuesta correcta: no hay nada escuchando.
 *
 * <h2>El par {@code shouldCommit} / {@code commit}</h2>
 *
 * <p>Preguntar antes de emitir no es una optimizacion opcional. Llenar los campos de un evento
 * puede costar —formatear una cadena, recorrer una estructura— y {@code shouldCommit} contesta si
 * ese trabajo va a servir de algo, mirando el umbral y los filtros configurados.
 *
 * @since 9
 */
public abstract class Event extends jdk.internal.event.Event {

    /** Para las subclases. */
    protected Event() {
    }

    /**
     * Marca el comienzo del evento y arranca su cronometro.
     *
     * <p>No hace falta para un evento sin duracion: si no se llama, el evento queda con duracion
     * cero y la marca de tiempo la pone {@link #commit}.
     */
    public final void begin() {
    }

    /**
     * Marca el final del evento y detiene su cronometro.
     *
     * <p>Separado de {@link #commit} para que la duracion medida sea la del trabajo y no incluya lo
     * que cueste llenar los campos del evento despues.
     */
    public final void end() {
    }

    /**
     * Emite el evento.
     *
     * <p>Si no se llamo a {@link #end}, lo llama por su cuenta.
     */
    public final void commit() {
    }

    /**
     * Si alguien esta grabando este tipo de evento.
     *
     * @return {@code false} en esta biblioteca, porque no hay grabador
     */
    public final boolean isEnabled() {
        return false;
    }

    /**
     * Si este evento pasaria los filtros configurados —umbral incluido— y por lo tanto vale la pena
     * terminar de armarlo.
     *
     * @return {@code false} en esta biblioteca, porque no hay grabador
     */
    public final boolean shouldCommit() {
        return false;
    }

    /**
     * Fija un campo por su indice, para los eventos armados en tiempo de ejecucion con
     * {@link EventFactory}.
     *
     * <p>Un evento hecho a mano tiene campos con nombre y se les asigna directamente; uno fabricado
     * dinamicamente no tiene campos Java, y esta es la unica forma de llenarlo.
     *
     * @param index el indice del campo, en el orden en que se declararon
     * @param value el valor
     */
    public final void set(int index, Object value) {
    }
}
