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
 * Un canal sobre una unica asociacion SCTP.
 *
 * <h2>El equivalente de un {@code SocketChannel}, con dos diferencias</h2>
 *
 * <p>La primera es el <strong>multihoming</strong>: {@link #bindAddress} y {@link #unbindAddress}
 * agregan y sacan direcciones locales <em>mientras la asociacion esta abierta</em>. Un
 * {@code SocketChannel} se liga a una direccion y ahi se queda; este puede ir cambiando el conjunto,
 * que es lo que le da tolerancia a fallas sin reconectar.
 *
 * <p>La segunda es que se manda y se recibe por <strong>mensajes</strong>, no por bytes: de ahi que
 * {@link #send} y {@link #receive} lleven un {@link MessageInfo} y no sean los {@code read}/
 * {@code write} de un canal de bytes.
 *
 * <h2>Por que {@link #receive} recibe un manejador de notificaciones</h2>
 *
 * <p>Porque mientras se espera un mensaje pueden llegar eventos de la asociacion, y no hay otro
 * momento en que el programa mire el canal. El {@link NotificationHandler} los atiende ahi mismo y
 * decide, con su {@link HandlerResult}, si la espera sigue.
 *
 * <h2>Lo que esta VM no puede</h2>
 *
 * <p>Los tres {@link #open} tiran {@link UnsupportedOperationException}: SCTP es un protocolo del
 * sistema operativo y esta VM no tiene la pila. No es algo que se arregle escribiendo mas Java. La
 * clase queda con la forma exacta del JDK —los metodos abstractos son declaraciones y no prometen
 * nada— y lo unico que declina es fabricar un canal que despues no hablaria con nadie.
 */
public abstract class SctpChannel extends AbstractSelectableChannel {

    /** Para las implementaciones de SCTP. */
    protected SctpChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Un canal sin conectar.
     *
     * @throws UnsupportedOperationException siempre, en esta VM — ver la nota de la clase
     */
    public static SctpChannel open() throws IOException {
        throw new UnsupportedOperationException("esta VM no tiene pila SCTP");
    }

    /**
     * Un canal conectado a {@code remote}, pidiendo esa cantidad de flujos.
     *
     * @throws UnsupportedOperationException siempre, en esta VM
     */
    public static SctpChannel open(SocketAddress remote, int maxOutStreams, int maxInStreams)
            throws IOException {
        throw new UnsupportedOperationException("esta VM no tiene pila SCTP");
    }

    /** La asociacion, o {@code null} si todavia no esta conectado. */
    public abstract Association association() throws IOException;

    /** Liga el canal a una direccion local; {@code null} deja elegir al sistema. */
    public abstract SctpChannel bind(SocketAddress local) throws IOException;

    /** Agrega una direccion local a la asociacion. Ver el multihoming en la nota de la clase. */
    public abstract SctpChannel bindAddress(InetAddress address) throws IOException;

    /** Saca una direccion local de la asociacion. */
    public abstract SctpChannel unbindAddress(InetAddress address) throws IOException;

    /** Conecta a {@code remote}; {@code false} si la conexion queda pendiente. */
    public abstract boolean connect(SocketAddress remote) throws IOException;

    /** Igual, pidiendo esa cantidad de flujos. */
    public abstract boolean connect(SocketAddress remote, int maxOutStreams, int maxInStreams)
            throws IOException;

    /** Si hay una conexion empezada y sin terminar. */
    public abstract boolean isConnectionPending();

    /** Termina una conexion pendiente. */
    public abstract boolean finishConnect() throws IOException;

    /** Todas las direcciones locales de la asociacion. */
    public abstract Set<SocketAddress> getAllLocalAddresses() throws IOException;

    /** Todas las direcciones del par. */
    public abstract Set<SocketAddress> getRemoteAddresses() throws IOException;

    /** Empieza a cerrar la asociacion ordenadamente. */
    public abstract SctpChannel shutdown() throws IOException;

    /** El valor de una opcion. */
    public abstract <T> T getOption(SctpSocketOption<T> name) throws IOException;

    /** Fija una opcion. */
    public abstract <T> SctpChannel setOption(SctpSocketOption<T> name, T value) throws IOException;

    /** Las opciones que este canal entiende. */
    public abstract Set<SctpSocketOption<?>> supportedOptions();

    /**
     * Las operaciones que este canal admite en un selector.
     *
     * <p>Las mismas tres que un {@code SocketChannel}: leer, escribir y conectar. Es {@code final}
     * porque no depende de la implementacion sino del tipo de canal.
     */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
    }

    /**
     * Recibe un mensaje, atendiendo por el camino las notificaciones que lleguen.
     *
     * @param handler quien atiende las notificaciones; {@code null} para ignorarlas
     * @return el descriptor del mensaje, o {@code null} si el manejador dijo
     *     {@link HandlerResult#RETURN}
     */
    public abstract <T> MessageInfo receive(ByteBuffer dst, T attachment,
            NotificationHandler<T> handler) throws IOException;

    /** Manda el contenido de {@code src} como un mensaje. */
    public abstract int send(ByteBuffer src, MessageInfo messageInfo) throws IOException;
}
