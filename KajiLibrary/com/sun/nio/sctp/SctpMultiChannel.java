package com.sun.nio.sctp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * Un canal que sostiene <strong>varias</strong> asociaciones a la vez.
 *
 * <h2>Que hace distinto</h2>
 *
 * <p>No tiene analogo en TCP. Un {@link SctpChannel} habla con una punta; este habla con muchas por
 * el mismo socket, y las asociaciones se van creando solas: mandarle un mensaje a una direccion con
 * la que todavia no hay asociacion la establece. De ahi que casi todos sus metodos lleven un
 * {@link Association} extra — hay que decir de cual se esta hablando.
 *
 * <p>Es la forma util para un servidor que atiende muchos pares sin un socket por cada uno.
 *
 * <h2>{@link #branch}, que es lo mas interesante de la clase</h2>
 *
 * <p>Saca una asociacion de este canal y la convierte en un {@link SctpChannel} propio. Sirve
 * justamente cuando una de muchas conexiones resulta ser especial y conviene tratarla aparte —sin
 * cortarla y volverla a establecer, que es lo que habria que hacer sin este metodo.
 *
 * <h2>Lo que esta VM no puede</h2>
 *
 * <p>{@link #open} tira {@link UnsupportedOperationException}: no hay pila SCTP. Ver la nota de
 * {@link SctpChannel}.
 */
public abstract class SctpMultiChannel extends AbstractSelectableChannel {

    /** Para las implementaciones de SCTP. */
    protected SctpMultiChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Un canal nuevo, sin asociaciones.
     *
     * @throws UnsupportedOperationException siempre, en esta VM
     */
    public static SctpMultiChannel open() throws IOException {
        throw new UnsupportedOperationException("esta VM no tiene pila SCTP");
    }

    /** Las asociaciones abiertas ahora. */
    public abstract Set<Association> associations() throws IOException;

    /** Liga el canal a una direccion local, con esa cantidad de conexiones en espera. */
    public abstract SctpMultiChannel bind(SocketAddress local, int backlog) throws IOException;

    /** Liga el canal dejando el {@code backlog} por omision. */
    public final SctpMultiChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }

    /** Agrega una direccion local a todas las asociaciones. */
    public abstract SctpMultiChannel bindAddress(InetAddress address) throws IOException;

    /** Saca una direccion local de todas las asociaciones. */
    public abstract SctpMultiChannel unbindAddress(InetAddress address) throws IOException;

    /** Todas las direcciones locales. */
    public abstract Set<SocketAddress> getAllLocalAddresses() throws IOException;

    /** Las direcciones del par de una asociacion. */
    public abstract Set<SocketAddress> getRemoteAddresses(Association association)
            throws IOException;

    /** Empieza a cerrar una asociacion; las demas siguen. */
    public abstract SctpMultiChannel shutdown(Association association) throws IOException;

    /** El valor de una opcion, en el ambito de una asociacion. */
    public abstract <T> T getOption(SctpSocketOption<T> name, Association association)
            throws IOException;

    /** Fija una opcion, en el ambito de una asociacion. */
    public abstract <T> SctpMultiChannel setOption(SctpSocketOption<T> name, T value,
            Association association) throws IOException;

    /** Las opciones que este canal entiende. */
    public abstract Set<SctpSocketOption<?>> supportedOptions();

    /**
     * Las operaciones que admite en un selector.
     *
     * <p>Leer y escribir, no conectar: aca no se conecta explicitamente — mandar a una direccion
     * nueva establece la asociacion sola.
     */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    }

    /** Recibe un mensaje de cualquiera de las asociaciones. */
    public abstract <T> MessageInfo receive(ByteBuffer dst, T attachment,
            NotificationHandler<T> handler) throws IOException;

    /** Manda un mensaje; si no hay asociacion con ese destino, se establece una. */
    public abstract int send(ByteBuffer src, MessageInfo messageInfo) throws IOException;

    /** Saca una asociacion de este canal y la devuelve como un canal propio. */
    public abstract SctpChannel branch(Association association) throws IOException;
}
