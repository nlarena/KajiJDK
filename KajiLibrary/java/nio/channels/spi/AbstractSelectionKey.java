package java.nio.channels.spi;

import java.nio.channels.SelectionKey;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractSelectionKey — la validez de una llave, resuelta.
 *
 * <p>Es la clase mas chica del paquete y hace una sola cosa, que es la que todos harian mal: separar
 * **cancelar** de **invalidar**.
 *
 * <p>{@link #cancel()} lo llama el usuario y tiene que ser barato e idempotente; lo unico que hace
 * es marcar la llave y anotarla en la lista de canceladas del selector. La baja de verdad
 * --sacar el canal, liberar lo que haya-- ocurre despues, dentro de la seleccion siguiente, donde el
 * selector es due&ntilde;o de sus estructuras. Hacerlo al reves --dar de baja en el acto-- significa
 * modificar el juego de llaves mientras otro hilo puede estar recorriendolo.
 *
 * <p>{@link #invalidate()} es lo contrario: no la llama el usuario --no es publica-- sino el
 * selector, cuando ya hizo la baja.
 */
public abstract class AbstractSelectionKey extends SelectionKey {

    private boolean valida = true;

    protected AbstractSelectionKey() {
    }

    public final boolean isValid() {
        return this.valida;
    }

    // La usa `AbstractSelector.deregister`. Package-private como en el JDK: invalidar sin dar de
    // baja dejaria al canal registrado en un selector que ya no lo mira.
    void invalidate() {
        this.valida = false;
    }

    /**
     * Cancela la llave.
     *
     * <p>Idempotente: llamarla dos veces no anota dos veces en la lista de canceladas, que si no
     * creceria sin limite en cualquier lazo que cancele por las dudas.
     */
    public final void cancel() {
        boolean primera = false;
        synchronized (this) {
            if (this.valida) {
                this.valida = false;
                primera = true;
            }
        }
        if (primera) {
            ((AbstractSelector) this.selector()).cancel(this);
        }
    }
}
