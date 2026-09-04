package java.nio.channels;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.function.Consumer;

/**
 * KajiLibrary's java.nio.channels.Selector — el que vigila muchos canales a la vez.
 *
 * <p>Un hilo bloqueado en {@link #select()} se despierta cuando **alguno** de los canales
 * registrados tiene algo listo. Eso es lo que permite atender diez mil conexiones con un pu&ntilde;ado
 * de hilos en vez de con diez mil, que es la razon de existir de todo `java.nio`.
 *
 * <p>Hay tres juegos de llaves y confundirlos es el error clasico:
 *
 * <ul>
 *   <li>{@link #keys()} — todo lo registrado. No se toca desde afuera;
 *   <li>{@link #selectedKeys()} — lo que tuvo actividad. <strong>Hay que vaciarlo a mano</strong>:
 *       el selector agrega ahi pero nunca saca, asi que una llave que no se remueve vuelve a
 *       aparecer en la vuelta siguiente aunque ya no tenga nada, y el lazo gira al vacio para
 *       siempre. Es el bug numero uno de quien empieza con selectores;
 *   <li>las canceladas — internas, se limpian solas en la seleccion siguiente.
 * </ul>
 *
 * <p>{@link #wakeup()} existe porque un `select()` puede quedarse quieto indefinidamente y a veces
 * hay que sacarlo de ahi sin que ningun canal tenga nada: apagar el servidor, por ejemplo. Es la
 * unica operacion del selector que se puede llamar desde otro hilo con seguridad.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p><strong>No hay `Selector.open()`.</strong> Ese estatico pide el selector al proveedor del
 * sistema, y esta VM no tiene proveedor del sistema porque no tiene nativos de red --ver
 * {@link SelectorProvider}--. Un `open()` que tirara seria peor que su ausencia: quien lo escribiera
 * compilaria bien y descubriria el problema en produccion, mientras que asi lo descubre al compilar,
 * que es donde corresponde.
 *
 * <p>Todo lo demas esta, incluidas las tres formas con {@link Consumer} --que en el JDK tampoco son
 * abstractas-- expresadas en terminos de las abstractas. Quien implemente un selector propio hereda
 * de {@link java.nio.channels.spi.AbstractSelector}, pone lo suyo, y estas le funcionan gratis.
 */
public abstract class Selector implements Closeable {

    protected Selector() {
    }

    /** Si el selector sigue abierto. */
    public abstract boolean isOpen();

    /** El proveedor que lo fabrico. */
    public abstract SelectorProvider provider();

    /** Todas las llaves registradas. El conjunto no se puede modificar desde afuera. */
    public abstract Set<SelectionKey> keys();

    /** Las llaves con actividad. **Se vacia a mano**; ver la nota de la clase. */
    public abstract Set<SelectionKey> selectedKeys();

    /** Mira y vuelve en el acto, haya o no algo listo. */
    public abstract int selectNow() throws IOException;

    /**
     * Espera hasta que haya algo listo o hasta que pasen `timeout` milisegundos.
     *
     * @param timeout `0` significa esperar sin limite, no "no esperar"; eso es {@link #selectNow()}
     */
    public abstract int select(long timeout) throws IOException;

    /** Espera sin limite. */
    public abstract int select() throws IOException;

    /**
     * Como {@link #select(long)}, pero corre `action` por cada llave lista en vez de dejarlas en
     * {@link #selectedKeys()}.
     *
     * <p>Es la forma que no se puede usar mal: el conjunto de seleccionadas no participa, asi que no
     * hay nada que olvidarse de vaciar.
     *
     * @return cuantas veces se corrio `action`, que puede ser mas que la cantidad de llaves si una
     *         se puso lista de nuevo durante la misma seleccion
     */
    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        if (action == null) {
            throw new NullPointerException();
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout negativo");
        }
        return this.recorrer(action, this.select(timeout));
    }

    /** Como el otro, sin limite de espera. */
    public int select(Consumer<SelectionKey> action) throws IOException {
        if (action == null) {
            throw new NullPointerException();
        }
        return this.recorrer(action, this.select());
    }

    /** Como el otro, sin esperar nada. */
    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        if (action == null) {
            throw new NullPointerException();
        }
        return this.recorrer(action, this.selectNow());
    }

    // Las tres formas con `Consumer` se apoyan en las abstractas y despues vacian el conjunto: es lo
    // que hace que no haya nada que el que llama pueda olvidarse de limpiar.
    private int recorrer(Consumer<SelectionKey> action, int n) {
        if (n == 0) {
            return 0;
        }
        Set<SelectionKey> listas = this.selectedKeys();
        int corridas = 0;
        // Se copia antes de recorrer: `action` tiene derecho a cancelar llaves, y cancelar mientras
        // se itera el conjunto vivo es una `ConcurrentModificationException` esperando su turno.
        Object[] copia = listas.toArray();
        listas.clear();
        int i = 0;
        while (i < copia.length) {
            action.accept((SelectionKey) copia[i]);
            corridas = corridas + 1;
            i = i + 1;
        }
        return corridas;
    }

    /**
     * Despierta a un `select` bloqueado, o hace que el proximo no llegue a bloquearse.
     *
     * <p>Lo segundo importa tanto como lo primero: si el aviso solo valiera para un `select` ya
     * empezado, quien llame justo antes de que empiece perderia el despertar y el hilo se quedaria
     * dormido igual.
     */
    public abstract Selector wakeup();

    /**
     * Cierra el selector; las llaves quedan invalidas y los canales se desregistran.
     *
     * <p>Sin `throws IOException`, y el JDK la declara: `java.io.Closeable` de esta biblioteca no la
     * declara y §8.4.8.3 prohibe ensanchar. La divergencia nace en `Closeable`; esta anotada igual
     * en {@link Channel}.
     */
    public abstract void close();
}
