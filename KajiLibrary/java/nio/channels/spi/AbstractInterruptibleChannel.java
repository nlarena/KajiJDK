package java.nio.channels.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.InterruptibleChannel;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractInterruptibleChannel — la base de todo canal.
 *
 * <p>Resuelve una sola cosa, y por eso existe: que **cerrar sea idempotente y ocurra una vez**. El
 * `close()` publico es `final` y lleva la contabilidad del bit de abierto; lo que cada canal tiene
 * de propio va en {@link #implCloseChannel()}, que se llama exactamente una vez por canal aunque se
 * cierre diez veces desde cinco hilos. Sin esta separacion cada canal reimplementaria el mismo
 * `if (yaCerrado) return;` y alguno lo haria mal.
 *
 * <h2>Lo que `begin()`/`end()` hacen aca, y lo que no</h2>
 *
 * <p>En el JDK este par envuelve cada operacion bloqueante y sirve para **abortarla desde afuera**:
 * `begin()` inscribe un interruptor en el hilo, y si alguien lo interrumpe mientras esta adentro, el
 * canal se cierra debajo suyo y la llamada revienta en el acto con {@link ClosedByInterruptException}.
 *
 * <p>Aca la deteccion es **al salir, no en el medio**, y conviene decirlo sin adornos: `end()` mira
 * si el canal se cerro o si el hilo quedo interrumpido durante la operacion y recien entonces tira.
 * La razon es que esta VM no expone el gancho que el JDK usa para desbloquear a un hilo parado en
 * una syscall. La diferencia no se nota en los canales que esta biblioteca sabe fabricar --los de
 * archivo, donde ninguna operacion se bloquea de verdad-- pero se notaria en uno de red, y por eso
 * queda escrito.
 *
 * <p>El contrato que **si** se cumple entero: si el canal se cerro asincronicamente mientras la
 * operacion corria, la operacion no devuelve un resultado a medias sino
 * {@link AsynchronousCloseException}; y si el hilo fue interrumpido, el canal queda cerrado y sale
 * {@link ClosedByInterruptException}. Un resultado parcial de una operacion abandonada es
 * exactamente lo que estas excepciones existen para no entregar.
 *
 * <h2>`close()` sin `throws IOException`</h2>
 *
 * <p>El JDK la declara; aca no se puede. {@link Channel} de esta biblioteca hereda de
 * `java.io.Closeable`, cuyo `close()` no la declara, y §8.4.8.3 prohibe que una redefinicion
 * ensanche las excepciones chequeadas. La divergencia nace en `Closeable` y se arrastra hasta aca.
 * Para no perder el motivo, lo que {@link #implCloseChannel()} tire como {@link IOException} sale
 * envuelto en {@link UncheckedIOException}: el error no se traga, cambia de forma.
 */
public abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {

    // Sin `volatile` a proposito: esta VM no garantiza que la palabra clave signifique lo que el
    // JMM dice, y un `volatile` que no ordena nada es peor que su ausencia porque invita a confiar.
    // Lo que si se garantiza es la idempotencia bajo el cerrojo de abajo.
    private boolean abierto = true;

    // Cerrojo propio y no `this`: si el candado fuera el canal, cualquiera que sincronice sobre un
    // canal ajeno podria trabar su cierre.
    private final Object cerrojo = new Object();

    // Marca de cierre asincronico ocurrido mientras habia una operacion adentro. Es lo que separa
    // "me cerraron" de "termine normal" cuando `end()` tiene que decidir que tirar.
    private boolean cerradoDuranteOperacion = false;

    protected AbstractInterruptibleChannel() {
    }

    /**
     * Cierra el canal.
     *
     * <p>Es `final` porque el punto de la clase es que nadie se saltee la contabilidad; lo propio de
     * cada canal va en {@link #implCloseChannel()}.
     *
     * @throws UncheckedIOException si el cierre concreto falla; ver la nota de la clase
     */
    public final void close() {
        synchronized (this.cerrojo) {
            if (!this.abierto) {
                return;
            }
            this.abierto = false;
            this.cerradoDuranteOperacion = true;
        }
        try {
            this.implCloseChannel();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * El cierre concreto de este canal, llamado una sola vez.
     *
     * <p>Cuando corre, el canal ya figura cerrado: quien pregunte {@link #isOpen()} desde adentro va
     * a ver `false`, que es lo correcto --el cierre ya se decidio, esto es solo ejecutarlo--.
     */
    protected abstract void implCloseChannel() throws IOException;

    public final boolean isOpen() {
        synchronized (this.cerrojo) {
            return this.abierto;
        }
    }

    /**
     * Marca el arranque de una operacion que podria bloquear.
     *
     * <p>Va siempre en pareja con {@link #end}, y el `end` va en un `finally`; si no, una excepcion
     * en el medio deja la marca puesta y la proxima operacion hereda un estado que no es suyo.
     */
    protected final void begin() {
        synchronized (this.cerrojo) {
            this.cerradoDuranteOperacion = !this.abierto;
        }
    }

    /**
     * Cierra la pareja de {@link #begin}.
     *
     * @param completed `true` si la operacion llego a completarse
     * @throws AsynchronousCloseException si el canal se cerro mientras la operacion corria
     * @throws ClosedByInterruptException si el hilo quedo interrumpido; el canal queda cerrado
     */
    protected final void end(boolean completed) throws AsynchronousCloseException {
        boolean cerrado;
        synchronized (this.cerrojo) {
            cerrado = this.cerradoDuranteOperacion || !this.abierto;
        }
        // El interrumpido se mira primero porque es la causa y el cierre es su consecuencia: al
        // reves, una interrupcion se reportaria como un cierre anonimo y se perderia el porque.
        if (Thread.currentThread().isInterrupted()) {
            this.close();
            throw new ClosedByInterruptException();
        }
        if (cerrado && !completed) {
            throw new AsynchronousCloseException();
        }
    }
}
