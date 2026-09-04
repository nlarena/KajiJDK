package com.sun.jdi.connect.spi;

import java.io.IOException;

/**
 * Un canal de paquetes JDWP ya establecido entre el depurador y la VM depurada.
 *
 * <h2>Qué es y qué no</h2>
 *
 * <p>Es deliberadamente estrecha: cuatro métodos, y ninguno sabe qué dice un paquete. Un
 * {@code Connection} transporta arreglos de bytes y nada más — quién habla primero, qué significa
 * cada campo y cómo se corresponden pedido y respuesta es asunto del protocolo JDWP, que vive una
 * capa más arriba. Esa separación es lo que permite que el mismo depurador funcione sobre un socket
 * TCP, sobre memoria compartida o sobre cualquier transporte que alguien escriba.
 *
 * <h2>El contrato de {@link #readPacket}</h2>
 *
 * <p>Devuelve <strong>un</strong> paquete completo, no lo que haya llegado. Reensamblar lo que el
 * transporte fragmentó es responsabilidad de quien implementa, y es la parte que hace que esta
 * interfaz valga la pena: sin ella cada usuario tendría que saber que TCP no respeta los límites de
 * mensaje.
 *
 * <p>Un arreglo de largo cero significa <em>fin de flujo</em>: el otro lado cerró ordenadamente. Es
 * distinto de {@link ClosedConnectionException}, que significa que esta conexión se cerró de este
 * lado o se rompió.
 */
public abstract class Connection {

    /** Para las implementaciones de transporte. */
    public Connection() {
    }

    /**
     * Lee un paquete completo.
     *
     * @return los bytes del paquete, o un arreglo vacío si el otro lado cerró
     * @throws ClosedConnectionException si esta conexión ya está cerrada
     * @throws IOException si falla el transporte
     */
    public abstract byte[] readPacket() throws IOException;

    /**
     * Escribe un paquete completo.
     *
     * @throws ClosedConnectionException si esta conexión ya está cerrada
     * @throws IllegalArgumentException si {@code pkt} no llega a tener un encabezado JDWP, o si el
     *     largo que declara su encabezado no coincide con el del arreglo
     * @throws IOException si falla el transporte
     */
    public abstract void writePacket(byte[] pkt) throws IOException;

    /**
     * Cierra la conexión.
     *
     * <p>Cerrar dos veces no es un error: la segunda no hace nada. Un {@link #readPacket} o
     * {@link #writePacket} bloqueado en otro hilo se desbloquea con
     * {@link ClosedConnectionException}, que es la razón de que este método exista en vez de
     * dejarle el trabajo al recolector.
     */
    public abstract void close() throws IOException;

    /** Si la conexión sigue abierta. */
    public abstract boolean isOpen();
}
