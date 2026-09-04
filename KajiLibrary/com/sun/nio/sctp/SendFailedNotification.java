package com.sun.nio.sctp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * Un mensaje no se pudo entregar y volvio.
 *
 * <p>Lo notable es {@link #buffer}: <strong>el mensaje vuelve entero</strong>, no solo el aviso de
 * que fallo. Es lo que permite reintentarlo por otra direccion o por otro flujo sin haberlo tenido
 * que guardar de antemano — y la razon de que esta notificacion no sea simplemente un codigo de
 * error.
 */
public abstract class SendFailedNotification implements Notification {

    /** Para las implementaciones de SCTP. */
    protected SendFailedNotification() {
    }

    /** La asociacion por la que se intento enviar. */
    public abstract Association association();

    /** La direccion a la que se intento enviar. */
    public abstract SocketAddress address();

    /** El mensaje que no se pudo entregar, entero. */
    public abstract ByteBuffer buffer();

    /** El codigo de error que dio la pila. */
    public abstract int errorCode();

    /** El flujo por el que se intento enviar. */
    public abstract int streamNumber();
}
