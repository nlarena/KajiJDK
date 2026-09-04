package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * Todo lo que acompana a un mensaje SCTP y no son sus bytes.
 *
 * <h2>Por que un mensaje necesita esto y un byte de TCP no</h2>
 *
 * <p>TCP entrega un flujo: el unico dato asociado es cuantos bytes hay. SCTP entrega
 * <strong>mensajes</strong>, y cada uno lleva por que flujo va, si va ordenado, cuanto vale la pena
 * seguir intentandolo, y con que protocolo de aplicacion se lo va a interpretar del otro lado. Nada
 * de eso cabe en el {@code ByteBuffer}.
 *
 * <h2>Los setters devuelven {@code this}</h2>
 *
 * <p>{@link #streamNumber(int)} y sus hermanos devuelven el mismo objeto, no uno nuevo, y eso
 * permite escribir la configuracion en una linea:
 *
 * <pre>{@code
 * MessageInfo.createOutgoing(destino, 3).unordered(true).timeToLive(500)
 * }</pre>
 *
 * <p>Vale saber que <strong>muta</strong>: no es un value object. Reusar un {@code MessageInfo}
 * entre dos envios comparte los cambios.
 */
public abstract class MessageInfo {

    /** Para las implementaciones de SCTP. */
    protected MessageInfo() {
    }

    /**
     * Un mensaje para mandar a {@code address} por el flujo {@code streamNumber}.
     *
     * <p>La direccion puede ser {@code null} cuando el canal ya sabe adonde va — un
     * {@link SctpChannel} conectado— y hace falta en un {@link SctpMultiChannel}, que habla con
     * varias puntas.
     *
     * @throws IllegalArgumentException si {@code streamNumber} es negativo o pasa de {@code 65536}
     */
    public static MessageInfo createOutgoing(SocketAddress address, int streamNumber) {
        return new Saliente(null, address, streamNumber);
    }

    /**
     * Igual, pero sobre una asociacion concreta de un {@link SctpMultiChannel}.
     *
     * @throws IllegalArgumentException si {@code streamNumber} esta fuera de rango
     */
    public static MessageInfo createOutgoing(Association association, SocketAddress address,
            int streamNumber) {
        if (association == null) {
            throw new IllegalArgumentException("hace falta la asociacion");
        }
        return new Saliente(association, address, streamNumber);
    }

    /** De donde vino o adonde va. */
    public abstract SocketAddress address();

    /** La asociacion, o {@code null} si todavia no hay una. */
    public abstract Association association();

    /** Cuantos bytes tiene el mensaje. */
    public abstract int bytes();

    /**
     * Si el mensaje esta entero.
     *
     * <p>Un {@code receive} puede devolver un mensaje incompleto cuando el buffer que le dieron no
     * alcanzaba. Ignorar esto es la forma mas facil de procesar medio mensaje como si fuera uno.
     */
    public abstract boolean isComplete();

    /** Marca si esta entero. */
    public abstract MessageInfo complete(boolean complete);

    /** Si va sin orden respecto de los demas de su flujo. */
    public abstract boolean isUnordered();

    /**
     * Marca si va sin orden.
     *
     * <p>Es la perilla que cambia el trato: un mensaje sin orden se entrega apenas llega, sin
     * esperar a los que iban antes en su flujo.
     */
    public abstract MessageInfo unordered(boolean unordered);

    /**
     * El identificador del protocolo de aplicacion.
     *
     * <p>SCTP no lo mira: lo lleva y lo entrega. Sirve para que las dos puntas se pongan de acuerdo
     * sobre como interpretar los bytes sin gastar un encabezado propio.
     */
    public abstract int payloadProtocolID();

    /** Fija el identificador de protocolo de aplicacion. */
    public abstract MessageInfo payloadProtocolID(int ppid);

    /** Por que flujo va o vino. */
    public abstract int streamNumber();

    /**
     * Fija el flujo.
     *
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract MessageInfo streamNumber(int streamNumber);

    /** Cuantos milisegundos vale la pena seguir intentando; {@code 0} es sin limite. */
    public abstract long timeToLive();

    /**
     * Fija el tiempo de vida.
     *
     * <p>Vencido, el mensaje se descarta y llega un {@link SendFailedNotification}. Es lo que hace
     * util a SCTP para datos que envejecen —telemetria, audio— donde reintentar para siempre es
     * peor que perder.
     */
    public abstract MessageInfo timeToLive(long millis);

    /**
     * La implementacion de un mensaje saliente.
     *
     * <p>Privada porque nadie deberia poder crear una por fuera de las dos fabricas: el JDK hace lo
     * mismo. Los mensajes <em>entrantes</em> los construye la pila, no esta clase.
     */
    private static final class Saliente extends MessageInfo {

        private final Association association;
        private final SocketAddress address;
        private int streamNumber;
        private boolean complete = true;
        private boolean unordered;
        private int ppid;
        private long timeToLive;

        Saliente(Association association, SocketAddress address, int streamNumber) {
            revisarFlujo(streamNumber);
            this.association = association;
            this.address = address;
            this.streamNumber = streamNumber;
        }

        private static void revisarFlujo(int streamNumber) {
            if (streamNumber < 0 || streamNumber > 65536) {
                throw new IllegalArgumentException("flujo fuera de rango: "
                        + String.valueOf(streamNumber));
            }
        }

        public SocketAddress address() {
            return this.address;
        }

        public Association association() {
            return this.association;
        }

        // Un mensaje saliente todavia no tiene bytes: los tiene el `ByteBuffer` que se le pasa al
        // `send`. El JDK contesta cero por lo mismo.
        public int bytes() {
            return 0;
        }

        public boolean isComplete() {
            return this.complete;
        }

        public MessageInfo complete(boolean complete) {
            this.complete = complete;
            return this;
        }

        public boolean isUnordered() {
            return this.unordered;
        }

        public MessageInfo unordered(boolean unordered) {
            this.unordered = unordered;
            return this;
        }

        public int payloadProtocolID() {
            return this.ppid;
        }

        public MessageInfo payloadProtocolID(int ppid) {
            this.ppid = ppid;
            return this;
        }

        public int streamNumber() {
            return this.streamNumber;
        }

        public MessageInfo streamNumber(int streamNumber) {
            revisarFlujo(streamNumber);
            this.streamNumber = streamNumber;
            return this;
        }

        public long timeToLive() {
            return this.timeToLive;
        }

        public MessageInfo timeToLive(long millis) {
            this.timeToLive = millis;
            return this;
        }
    }
}
