package com.sun.nio.sctp;

/**
 * Lo que un {@link NotificationHandler} le contesta al canal despues de atender una notificacion.
 *
 * <p>El canal esta en medio de un {@code receive} cuando llega una notificacion, asi que despues de
 * manejarla tiene que decidir si sigue esperando el mensaje que le pidieron o vuelve con las manos
 * vacias. Quien decide es el manejador, porque es el unico que sabe si lo que acaba de pasar
 * invalida la espera — un {@code COMM_LOST} la invalida, un cambio de direccion no.
 */
public enum HandlerResult {

    /** Seguir esperando: el {@code receive} continua. */
    CONTINUE,
    /** Volver ya: el {@code receive} termina sin mensaje. */
    RETURN
}
