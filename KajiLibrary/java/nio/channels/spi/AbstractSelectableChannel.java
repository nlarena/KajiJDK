package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractSelectableChannel — el registro y el modo bloqueante,
 * resueltos de una vez.
 *
 * <p>Esta es la clase que hace que traer un transporte propio a `java.nio` sea razonable: todo lo
 * enredado del registro esta aca y no se reescribe. Lo que queda para el que hereda son dos metodos
 * --{@link #implCloseSelectableChannel()} y {@link #implConfigureBlocking(boolean)}-- y ninguno
 * tiene contabilidad.
 *
 * <h2>Las llaves se guardan en un arreglo, no en un mapa</h2>
 *
 * <p>Un canal esta registrado en uno o dos selectores; casi nunca en mas. Con esos tama&ntilde;os un
 * `HashMap` cuesta mas de lo que ahorra --hash, cubetas, objeto de entrada por registro-- y una
 * busqueda lineal sobre un arreglo de tres es mas rapida. El JDK hace lo mismo y por lo mismo. Los
 * huecos que deja borrar se reusan antes de agrandar, asi que un canal que se registra y se da de
 * baja mil veces no hace crecer nada.
 *
 * <h2>Por que el modo bloqueante tiene su propio candado</h2>
 *
 * <p>Registrar exige que el canal **no** sea bloqueante, y ponerlo bloqueante exige que **no** este
 * registrado. Las dos condiciones se miran y se actuan, asi que sin un candado comun dos hilos
 * pueden pasar los dos chequeos y dejar un canal bloqueante dentro de un selector, que es
 * precisamente el estado que las dos reglas existen para impedir. Ese candado es
 * {@link #blockingLock()}, y esta expuesto porque el codigo de afuera necesita la misma garantia.
 */
public abstract class AbstractSelectableChannel extends SelectableChannel {

    private final SelectorProvider proveedor;

    // Las llaves vigentes; puede haber huecos en `null`. Ver la nota de la clase.
    private SelectionKey[] llaves = null;
    private int cuantas = 0;

    private final Object candadoLlaves = new Object();
    private final Object candadoBloqueo = new Object();

    private boolean bloqueante = true;

    protected AbstractSelectableChannel(SelectorProvider provider) {
        this.proveedor = provider;
    }

    public final SelectorProvider provider() {
        return this.proveedor;
    }

    // ---- llaves ----------------------------------------------------------------------------------

    private void agregar(SelectionKey k) {
        synchronized (this.candadoLlaves) {
            if (this.llaves == null) {
                this.llaves = new SelectionKey[3];
            }
            int i = 0;
            while (i < this.llaves.length) {
                if (this.llaves[i] == null) {
                    this.llaves[i] = k;
                    this.cuantas = this.cuantas + 1;
                    return;
                }
                i = i + 1;
            }
            // Se duplica en vez de crecer de a uno: registrarse en n selectores costaria O(n^2) en
            // copias si el arreglo creciera justo lo necesario cada vez.
            SelectionKey[] mas = new SelectionKey[this.llaves.length * 2];
            System.arraycopy(this.llaves, 0, mas, 0, this.llaves.length);
            mas[this.llaves.length] = k;
            this.llaves = mas;
            this.cuantas = this.cuantas + 1;
        }
    }

    // La llama `AbstractSelector.deregister`. Invalida la llave **y** la saca: separarlo dejaria
    // llaves validas apuntando a un registro que ya no existe.
    void removeKey(SelectionKey k) {
        synchronized (this.candadoLlaves) {
            if (this.llaves == null) {
                return;
            }
            int i = 0;
            while (i < this.llaves.length) {
                if (this.llaves[i] == k) {
                    this.llaves[i] = null;
                    this.cuantas = this.cuantas - 1;
                    break;
                }
                i = i + 1;
            }
        }
        ((AbstractSelectionKey) k).invalidate();
    }

    public final boolean isRegistered() {
        synchronized (this.candadoLlaves) {
            return this.cuantas != 0;
        }
    }

    public final SelectionKey keyFor(Selector sel) {
        synchronized (this.candadoLlaves) {
            if (this.llaves == null) {
                return null;
            }
            int i = 0;
            while (i < this.llaves.length) {
                SelectionKey k = this.llaves[i];
                if (k != null && k.selector() == sel) {
                    return k;
                }
                i = i + 1;
            }
            return null;
        }
    }

    /**
     * Anota el canal en `sel`.
     *
     * <p>Si ya estaba anotado ahi **no crea una llave nueva**: le cambia las operaciones y el adjunto
     * a la que habia. Es lo que hace que registrar de nuevo sea seguro, y sin eso un lazo de
     * reactor que reafirma su interes en cada vuelta acumularia registros hasta quedarse sin memoria.
     */
    public final SelectionKey register(Selector sel, int ops, Object att)
            throws ClosedChannelException {
        if (sel == null) {
            throw new NullPointerException();
        }
        if ((ops & ~this.validOps()) != 0) {
            throw new IllegalArgumentException("operacion no admitida por este canal");
        }
        synchronized (this.candadoBloqueo) {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.bloqueante) {
                throw new IllegalBlockingModeException();
            }
            SelectionKey k = this.keyFor(sel);
            if (k != null) {
                // Un `interestOps` sobre una llave cancelada tira, y la cancelacion pudo pasar entre
                // el `keyFor` y esto: se deja subir tal cual, porque reciclar una llave cancelada
                // seria resucitar un registro que alguien pidio terminar.
                k.interestOps(ops);
                k.attach(att);
                return k;
            }
            SelectionKey nueva = ((AbstractSelector) sel).register(this, ops, att);
            this.agregar(nueva);
            return nueva;
        }
    }

    // ---- cierre ----------------------------------------------------------------------------------

    /**
     * Cierra el canal y cancela todas sus llaves.
     *
     * <p>El orden importa: primero lo propio del canal, despues las llaves. Al reves, un selector
     * podria despertar por un canal que ya esta cerrado y entregar una llave lista sobre nada.
     */
    protected final void implCloseChannel() throws IOException {
        this.implCloseSelectableChannel();
        synchronized (this.candadoLlaves) {
            if (this.llaves == null) {
                return;
            }
            int i = 0;
            while (i < this.llaves.length) {
                SelectionKey k = this.llaves[i];
                if (k != null) {
                    k.cancel();
                }
                i = i + 1;
            }
        }
    }

    /** El cierre concreto de este canal. */
    protected abstract void implCloseSelectableChannel() throws IOException;

    // ---- modo bloqueante -------------------------------------------------------------------------

    public final boolean isBlocking() {
        synchronized (this.candadoBloqueo) {
            return this.bloqueante;
        }
    }

    public final Object blockingLock() {
        return this.candadoBloqueo;
    }

    /**
     * Cambia el modo.
     *
     * <p>Volver a bloqueante estando registrado es {@link IllegalBlockingModeException}: un selector
     * que vigila un canal bloqueante no puede cumplir lo que promete.
     */
    public final SelectableChannel configureBlocking(boolean block) throws IOException {
        synchronized (this.candadoBloqueo) {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.bloqueante == block) {
                return this;
            }
            if (block && this.isRegistered()) {
                throw new IllegalBlockingModeException();
            }
            this.implConfigureBlocking(block);
            this.bloqueante = block;
        }
        return this;
    }

    /** El cambio de modo concreto. Solo se llama cuando el modo de verdad cambia. */
    protected abstract void implConfigureBlocking(boolean block) throws IOException;
}
