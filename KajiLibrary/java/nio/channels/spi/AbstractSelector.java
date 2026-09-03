package java.nio.channels.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractSelector — la parte de un selector que no depende del
 * sistema.
 *
 * <p>Resuelve tres cosas y deja el resto a quien la herede:
 *
 * <ul>
 *   <li><strong>El cierre ocurre una vez.</strong> {@link #close()} es `final` y lleva la
 *       contabilidad; lo propio va en {@link #implCloseSelector()};
 *   <li><strong>Las canceladas se juntan en un lugar.</strong> {@link #cancelledKeys()} es el buzon
 *       donde {@link AbstractSelectionKey#cancel()} deja las llaves; el que implementa la seleccion
 *       lo vacia al principio de cada `select`, que es el unico momento en que puede tocar sus
 *       estructuras sin carrera;
 *   <li><strong>La baja de una llave es un solo paso.</strong> {@link #deregister} invalida la llave
 *       y la saca del canal a la vez. Separarlo deja llaves validas sobre canales dados de baja, que
 *       es la clase de estado que despues nadie entiende.
 * </ul>
 *
 * <p>{@link #begin()} y {@link #end()} envuelven la espera para que se la pueda interrumpir. Aca
 * valen las mismas salvedades que en {@link AbstractInterruptibleChannel}: esta VM no puede sacar a
 * un hilo de una espera empezada, asi que lo que hay es el par y su contrato, no el desbloqueo.
 *
 * <p>Sin `Selector.open()` no hay manera de llegar a un selector del sistema; quien implemente uno
 * propio hereda de aca y tiene lo de arriba hecho.
 */
public abstract class AbstractSelector extends Selector {

    private final Set<SelectionKey> canceladas = new HashSet<SelectionKey>();
    private final SelectorProvider proveedor;
    private boolean abierto = true;

    protected AbstractSelector(SelectorProvider provider) {
        this.proveedor = provider;
    }

    // La llama `AbstractSelectionKey.cancel()`. Package-private: el buzon se llena por ese camino y
    // no por cualquiera, o dejaria de valer que todo lo que hay adentro fue cancelado de verdad.
    void cancel(SelectionKey k) {
        synchronized (this.canceladas) {
            this.canceladas.add(k);
        }
    }

    /**
     * Cierra el selector.
     *
     * <p>Sin `throws IOException` --el JDK la declara-- por la cadena que arranca en
     * `java.io.Closeable`; lo que {@link #implCloseSelector()} tire sale envuelto en
     * {@link UncheckedIOException} para no perder el motivo.
     */
    public final void close() {
        synchronized (this) {
            if (!this.abierto) {
                return;
            }
            this.abierto = false;
        }
        try {
            this.implCloseSelector();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** El cierre concreto, llamado una sola vez. */
    protected abstract void implCloseSelector() throws IOException;

    public final boolean isOpen() {
        return this.abierto;
    }

    public final SelectorProvider provider() {
        return this.proveedor;
    }

    /**
     * El buzon de llaves canceladas.
     *
     * <p>Se devuelve el conjunto vivo, no una copia: el que implementa `select` tiene que **vaciarlo**
     * despues de procesarlo, y sobre una copia no podria.
     */
    protected final Set<SelectionKey> cancelledKeys() {
        return this.canceladas;
    }

    /**
     * Registra `ch` en este selector.
     *
     * <p>Lo llama {@link AbstractSelectableChannel#register} y no el usuario: la validacion de que el
     * canal este abierto, no bloqueante y admita `ops` ya paso alli.
     */
    protected abstract SelectionKey register(AbstractSelectableChannel ch, int ops, Object att);

    /**
     * Da de baja `key`: la invalida y la saca de su canal.
     *
     * <p>Las dos cosas juntas a proposito; ver la nota de la clase.
     */
    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel) key.channel()).removeKey(key);
    }

    /** Marca el arranque de una espera interrumpible. Va en pareja con {@link #end()}. */
    protected final void begin() {
    }

    /** Cierra la pareja de {@link #begin()}. */
    protected final void end() {
    }
}
