package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.UserSessionEvent -- la sesion del usuario se activo o se desactivo.
 *
 * <p>Lo entrega {@link UserSessionListener}. Una sesion se desactiva cuando el usuario cambia de
 * cuenta, bloquea la pantalla, o se conecta desde otro lado; el programa <b>sigue corriendo</b>, solo
 * que nadie lo esta viendo.
 *
 * <p>{@link #getReason} dice cual de esas cosas paso, y es lo que permite reaccionar distinto: un
 * bloqueo de pantalla es buen momento para pedir la clave de vuelta, un cambio de consola no
 * necesariamente.
 */
public final class UserSessionEvent extends AppEvent {

    private static final long serialVersionUID = 6747138462796569055L;

    /** Por que cambio. */
    private final Reason reason;

    /**
     * Los cuatro motivos por los que una sesion cambia de estado.
     *
     * <p>{@link #UNSPECIFIED} no es un error: hay sistemas que avisan del cambio sin decir por que, y
     * un manejador tiene que estar preparado para eso.
     */
    public enum Reason {

        /** El sistema no dijo por que. */
        UNSPECIFIED,

        /** El usuario cambio de consola local. */
        CONSOLE,

        /** Alguien se conecto o desconecto de forma remota. */
        REMOTE,

        /** Se bloqueo o se desbloqueo la pantalla. */
        LOCK
    }

    /** @param reason por que cambio */
    public UserSessionEvent(final Reason reason) {
        this.reason = reason;
    }

    /** Por que cambio. Ver la nota de la clase. */
    public Reason getReason() {
        return this.reason;
    }
}
