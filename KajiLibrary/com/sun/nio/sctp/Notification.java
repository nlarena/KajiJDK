package com.sun.nio.sctp;

/**
 * Algo que le paso a una asociacion y que no es un mensaje.
 *
 * <h2>Por que el protocolo necesita esto y TCP no</h2>
 *
 * <p>En TCP los eventos de la conexion se ven como efectos: el socket se cierra, una lectura
 * devuelve {@code -1}. SCTP tiene mas cosas que contar —una direccion del par que dejo de responder,
 * un mensaje que no se pudo entregar, una asociacion que se reinicio— y ninguna de ellas cabe en el
 * flujo de datos, porque no son datos.
 *
 * <p>Van entonces por un canal aparte, y como el {@code receive} es el unico momento en que el
 * programa mira el canal, las notificaciones se entregan ahi, a un {@link NotificationHandler}.
 */
public interface Notification {

    /** La asociacion a la que le paso; puede ser {@code null} si todavia no habia ninguna. */
    Association association();
}
