package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * Una direccion del par cambio de estado.
 *
 * <h2>Por que existe: el multihoming</h2>
 *
 * <p>Una punta SCTP puede tener <strong>varias direcciones</strong> a la vez, y la asociacion sigue
 * viva mientras alguna funcione. Eso es lo que le da tolerancia a fallas sin reconectar — y es
 * tambien lo que hace falta notificar, porque el conjunto de direcciones cambia mientras la
 * asociacion esta abierta.
 *
 * <p>Una de ellas es la <em>primaria</em>: la que se usa por omision. {@link AddressChangeEvent}
 * incluye el cambio de primaria por eso.
 */
public abstract class PeerAddressChangeNotification implements Notification {

    /** Que le paso a la direccion. */
    public enum AddressChangeEvent {

        /** Volvio a estar disponible. */
        ADDR_AVAILABLE,
        /** Dejo de responder. */
        ADDR_UNREACHABLE,
        /** El par la saco de la asociacion. */
        ADDR_REMOVED,
        /** El par la agrego a la asociacion. */
        ADDR_ADDED,
        /** Paso a ser la primaria. */
        ADDR_MADE_PRIMARY,
        /** Se confirmo que es alcanzable. */
        ADDR_CONFIRMED
    }

    /** Para las implementaciones de SCTP. */
    protected PeerAddressChangeNotification() {
    }

    /** La direccion que cambio. */
    public abstract SocketAddress address();

    /** La asociacion a la que pertenece. */
    public abstract Association association();

    /** Cual de los seis eventos fue. */
    public abstract AddressChangeEvent event();
}
