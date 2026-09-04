package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.SelectableChannel — un canal que un {@link Selector} puede vigilar.
 *
 * <p>La idea entera de `java.nio` esta en esta clase: en vez de un hilo por conexion bloqueado en su
 * `read`, un hilo vigila mil canales y solo atiende a los que tienen algo. Para entrar en esa rueda
 * un canal tiene que poder hacer dos cosas: **no bloquear** ({@link #configureBlocking}) y
 * **anotarse** ({@link #register}).
 *
 * <p>Las dos van juntas y el orden importa: registrar un canal en modo bloqueante tira
 * {@link IllegalBlockingModeException}, porque seria pedirle a un selector que avise cuando algo
 * este listo para una lectura que igual se iba a quedar esperando sola.
 *
 * <p>Una llave ({@link SelectionKey}) por canal **y por selector**: registrar dos veces en el mismo
 * selector no crea una llave nueva, actualiza la que habia. Es lo que hace que
 * `register` sea idempotente y que no se acumulen registros fantasma.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La clase esta entera --sus diez miembros publicos-- pero <strong>no hay ningun canal selectable
 * que se pueda fabricar</strong>, porque los unicos que lo son en el JDK son los de red y esta VM no
 * tiene nativos de red. Lo que si esta es toda la maquinaria de abajo:
 * {@link java.nio.channels.spi.AbstractSelectableChannel} implementa de verdad el registro, el modo
 * bloqueante y el manejo de llaves, asi que quien traiga su propio transporte hereda de ahi y le
 * funciona sin escribir nada de esto.
 */
public abstract class SelectableChannel extends AbstractInterruptibleChannel implements Channel {

    protected SelectableChannel() {
    }

    /** El proveedor que lo fabrico. */
    public abstract SelectorProvider provider();

    /**
     * Las operaciones que este tipo de canal admite, en el juego de bits de {@link SelectionKey}.
     *
     * <p>Un canal de escucha admite `OP_ACCEPT` y nada mas; uno conectado, lectura y escritura.
     * Registrar pidiendo una operacion que no esta aca es {@link IllegalArgumentException}, y es
     * mejor que el silencio: un `OP_ACCEPT` sobre un socket conectado no se cumple nunca, y sin este
     * chequeo el sintoma seria un selector que no despierta jamas.
     */
    public abstract int validOps();

    /** Si esta registrado en algun selector. */
    public abstract boolean isRegistered();

    /** La llave de este canal en `sel`, o `null` si no esta registrado ahi. */
    public abstract SelectionKey keyFor(Selector sel);

    /**
     * Anota el canal en `sel` para las operaciones `ops`, con `att` colgado de la llave.
     *
     * @throws ClosedChannelException si el canal esta cerrado
     * @throws IllegalBlockingModeException si el canal esta en modo bloqueante
     * @throws IllegalArgumentException si `ops` pide algo fuera de {@link #validOps()}
     */
    public abstract SelectionKey register(Selector sel, int ops, Object att)
            throws ClosedChannelException;

    /** Como el otro, sin nada colgado. */
    public final SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
        return this.register(sel, ops, null);
    }

    /**
     * Pone el canal en modo bloqueante o no bloqueante.
     *
     * @throws IllegalBlockingModeException si se pide bloqueante estando registrado en un selector
     */
    public abstract SelectableChannel configureBlocking(boolean block) throws IOException;

    /** Si esta en modo bloqueante. */
    public abstract boolean isBlocking();

    /**
     * El objeto sobre el que sincronizar para que el modo bloqueante no cambie.
     *
     * <p>Esta expuesto y no escondido porque el que necesita la garantia es el codigo de afuera: sin
     * un candado publico, "poner en no bloqueante, hacer la operacion, restaurar" es una carrera con
     * cualquier otro hilo que toque el mismo canal.
     */
    public abstract Object blockingLock();
}
