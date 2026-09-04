package com.sun.jdi.connect.spi;

import java.io.IOException;

/**
 * Un transporte por el que un depurador y una VM pueden hablar JDWP.
 *
 * <h2>Los dos roles, y por qué son asimétricos</h2>
 *
 * <p>Una conexión de depuración se establece de dos maneras y esta clase tiene métodos para las dos:
 * {@link #attach} va a buscar a alguien que ya está esperando, y el par
 * {@link #startListening}/{@link #accept} se pone a esperar. La asimetría no es de estilo: quien
 * escucha necesita una dirección <em>antes</em> de que alguien se conecte, y por eso
 * {@code startListening} devuelve un {@link ListenKey} en vez de bloquearse. La dirección sale de
 * ahí, se le pasa al otro lado por cualquier medio, y recién entonces se llama a {@code accept}.
 *
 * <h2>Por qué {@link #capabilities} en vez de tirar excepciones</h2>
 *
 * <p>No todo transporte soporta todo. La memoria compartida, por ejemplo, no siempre puede imponer
 * un timeout de handshake. Un transporte podría aceptar el parámetro y tirar una excepción al
 * usarlo; en vez de eso, {@link Capabilities} deja preguntarlo <em>antes</em>. La diferencia es
 * práctica: quien llama puede elegir otro transporte en vez de descubrir a mitad de camino que el
 * timeout que pidió no se respeta.
 *
 * <p>Cuando una capacidad falta, el parámetro correspondiente no se ignora en silencio —eso sería lo
 * peor de los dos mundos—: se rechaza con {@link IllegalArgumentException} si no es cero.
 */
public abstract class TransportService {

    /** Para las implementaciones de transporte. */
    public TransportService() {
    }

    /** El nombre del transporte, como lo nombra quien lo elige. */
    public abstract String name();

    /** Una descripción para mostrarle a una persona. */
    public abstract String description();

    /** Qué sabe hacer este transporte; ver la nota en la descripción de la clase. */
    public abstract Capabilities capabilities();

    /**
     * Se conecta a una VM que ya está esperando en {@code address}.
     *
     * @param attachTimeout milisegundos para establecer la conexión, o {@code 0} para esperar sin
     *     límite
     * @param handshakeTimeout milisegundos para completar el handshake JDWP una vez conectados. Es
     *     un timeout aparte porque son dos fallas distintas: no llegar y llegar a algo que no habla
     *     el protocolo
     * @throws IllegalArgumentException si un timeout es negativo, o si es positivo y el transporte
     *     no soporta esa capacidad
     */
    public abstract Connection attach(String address, long attachTimeout, long handshakeTimeout)
            throws IOException;

    /**
     * Empieza a escuchar en {@code address}.
     *
     * <p>No bloquea: vuelve enseguida con la llave que {@link #accept} necesita, y de la que sale la
     * dirección real que hay que darle al otro lado.
     */
    public abstract ListenKey startListening(String address) throws IOException;

    /** Empieza a escuchar en una dirección que elige el transporte. */
    public abstract ListenKey startListening() throws IOException;

    /**
     * Deja de escuchar.
     *
     * <p>Las conexiones ya aceptadas siguen vivas: esto cierra la puerta, no lo que ya entró.
     */
    public abstract void stopListening(ListenKey listenKey) throws IOException;

    /**
     * Espera a que alguien se conecte a lo que {@code listenKey} está escuchando.
     *
     * @throws IllegalStateException si a esa llave ya se le hizo {@link #stopListening}
     */
    public abstract Connection accept(ListenKey listenKey, long acceptTimeout,
            long handshakeTimeout) throws IOException;

    /**
     * Qué sabe hacer un transporte.
     *
     * <p>Abstracta y no una interfaz, ni un record de cuatro {@code boolean}, porque así el JDK
     * puede agregarle capacidades sin romper a quien ya la extendió — el precio de agregar un método
     * a una clase abstracta lo paga sólo quien quiera contestarlo.
     */
    public abstract static class Capabilities {

        public Capabilities() {
        }

        /** Si un mismo {@link ListenKey} puede aceptar más de una conexión. */
        public abstract boolean supportsMultipleConnections();

        /** Si {@link TransportService#attach} respeta su {@code attachTimeout}. */
        public abstract boolean supportsAttachTimeout();

        /** Si {@link TransportService#accept} respeta su {@code acceptTimeout}. */
        public abstract boolean supportsAcceptTimeout();

        /** Si los dos respetan su {@code handshakeTimeout}. */
        public abstract boolean supportsHandshakeTimeout();
    }

    /**
     * Lo que devuelve {@link TransportService#startListening} y consume
     * {@link TransportService#accept}.
     *
     * <p>Es un objeto y no la dirección en texto por una razón: la dirección que quedó no tiene por
     * qué ser la que se pidió. Escuchar en el puerto {@code 0} hace que el sistema elija uno, y
     * {@link #address} es cómo se averigua cuál.
     */
    public abstract static class ListenKey {

        public ListenKey() {
        }

        /** La dirección en la que realmente se está escuchando. */
        public abstract String address();
    }
}
