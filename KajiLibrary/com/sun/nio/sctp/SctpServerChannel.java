package com.sun.nio.sctp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * El canal que escucha y acepta asociaciones, una por una.
 *
 * <p>Es el analogo de un {@code ServerSocketChannel}, y la alternativa a
 * {@link SctpMultiChannel}: alli las asociaciones conviven en un solo canal, aca cada
 * {@link #accept} devuelve un {@link SctpChannel} propio. Cual conviene depende de si el servidor
 * quiere tratar a cada par por separado o atenderlos a todos desde el mismo lugar.
 *
 * <p>No tiene {@code send} ni {@code receive}: por un canal que solo escucha no pasan mensajes.
 *
 * <p>{@link #open} tira {@link UnsupportedOperationException} en esta VM; ver la nota de
 * {@link SctpChannel}.
 */
public abstract class SctpServerChannel extends AbstractSelectableChannel {

    /** Para las implementaciones de SCTP. */
    protected SctpServerChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Un canal de escucha nuevo, sin ligar.
     *
     * @throws UnsupportedOperationException siempre, en esta VM
     */
    public static SctpServerChannel open() throws IOException {
        throw new UnsupportedOperationException("esta VM no tiene pila SCTP");
    }

    /** Espera una asociacion y la devuelve como canal propio. */
    public abstract SctpChannel accept() throws IOException;

    /** Liga el canal dejando el {@code backlog} por omision. */
    public final SctpServerChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }

    /** Liga el canal, con esa cantidad de asociaciones en espera. */
    public abstract SctpServerChannel bind(SocketAddress local, int backlog) throws IOException;

    /** Agrega una direccion local. */
    public abstract SctpServerChannel bindAddress(InetAddress address) throws IOException;

    /** Saca una direccion local. */
    public abstract SctpServerChannel unbindAddress(InetAddress address) throws IOException;

    /** Todas las direcciones locales. */
    public abstract Set<SocketAddress> getAllLocalAddresses() throws IOException;

    /** El valor de una opcion. */
    public abstract <T> T getOption(SctpSocketOption<T> name) throws IOException;

    /** Fija una opcion. */
    public abstract <T> SctpServerChannel setOption(SctpSocketOption<T> name, T value)
            throws IOException;

    /** Las opciones que este canal entiende. */
    public abstract Set<SctpSocketOption<?>> supportedOptions();

    /** Lo unico que un canal de escucha admite en un selector: aceptar. */
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
    }
}
