package com.sun.net.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Un pedido y su respuesta, vistos como un solo objeto.
 *
 * <h2>El orden es un contrato, no una sugerencia</h2>
 *
 * <p>Leer el cuerpo del pedido, despues {@link #sendResponseHeaders}, despues escribir el cuerpo de
 * la respuesta, despues {@link #close}. Salirse de ese orden no da un error claro: da un encabezado
 * que llega despues del cuerpo, o una conexion que queda colgada.
 *
 * <h2>Los dos numeros de {@code sendResponseHeaders}</h2>
 *
 * <p>El segundo parametro tiene tres significados distintos segun el valor, y es lo mas facil de
 * equivocar de toda esta API:
 *
 * <ul>
 * <li><strong>positivo</strong> — el largo exacto del cuerpo. Escribir mas o menos que eso rompe la
 *     respuesta;</li>
 * <li><strong>cero</strong> — hay cuerpo pero no se sabe cuanto; se manda por trozos;</li>
 * <li><strong>{@code -1}</strong> — no hay cuerpo. Un {@code 204} o un {@code 304} lo necesitan, y
 *     usar cero ahi dejaria al cliente esperando un cuerpo que nunca llega.</li>
 * </ul>
 *
 * <p>Es {@link AutoCloseable} desde Java 21, asi que la forma correcta es un {@code try} con
 * recursos: cerrar libera las dos corrientes y la conexion, y no cerrar la deja tomada hasta que
 * venza.
 */
public abstract class HttpExchange implements AutoCloseable, Request {

    /** Para las implementaciones. */
    protected HttpExchange() {
    }

    /** Los encabezados del pedido, de solo lectura. */
    public abstract Headers getRequestHeaders();

    /**
     * Los encabezados de la respuesta, mutables.
     *
     * <p>Hay que llenarlos <strong>antes</strong> de {@link #sendResponseHeaders}: despues ya
     * viajaron y modificarlos no hace nada.
     */
    public abstract Headers getResponseHeaders();

    /** La URI pedida. */
    public abstract URI getRequestURI();

    /** El metodo HTTP, en mayusculas. */
    public abstract String getRequestMethod();

    /** El contexto que atrapo este pedido. */
    public abstract HttpContext getHttpContext();

    /**
     * Cierra el intercambio.
     *
     * <p>No declara {@code IOException}, y eso es deliberado: cerrar tiene que poder ir en un
     * {@code finally} sin obligar a anidar otro {@code try}.
     */
    public abstract void close();

    /**
     * El cuerpo del pedido.
     *
     * <p>Hay que leerlo hasta el final —o cerrarlo— aunque no interese: lo que quede sin leer sigue
     * en la conexion y descoloca al pedido siguiente si el cliente la reusa.
     */
    public abstract InputStream getRequestBody();

    /**
     * El cuerpo de la respuesta.
     *
     * <p>Recien sirve despues de {@link #sendResponseHeaders}.
     */
    public abstract OutputStream getResponseBody();

    /**
     * Manda el codigo y los encabezados; ver la nota de la clase sobre {@code responseLength}.
     *
     * @param rCode el codigo HTTP
     * @param responseLength positivo el largo exacto, {@code 0} desconocido, {@code -1} sin cuerpo
     */
    public abstract void sendResponseHeaders(int rCode, long responseLength) throws IOException;

    /** De donde vino el pedido. */
    public abstract InetSocketAddress getRemoteAddress();

    /** El codigo ya mandado, o {@code -1} si todavia no se mando ninguno. */
    public abstract int getResponseCode();

    /** La direccion local por la que entro. */
    public abstract InetSocketAddress getLocalAddress();

    /** La version del protocolo, como {@code "HTTP/1.1"}. */
    public abstract String getProtocol();

    /**
     * Un atributo de <strong>este</strong> pedido.
     *
     * <p>Distinto de {@link HttpContext#getAttributes}, que es compartido entre todos: esto vive lo
     * que vive el intercambio, y es donde un filtro le deja algo al manejador.
     */
    public abstract Object getAttribute(String name);

    /** Pone un atributo de este pedido. */
    public abstract void setAttribute(String name, Object value);

    /**
     * Reemplaza las dos corrientes.
     *
     * <p>Es como un filtro envuelve el cuerpo — comprimirlo, contarlo, cifrarlo — sin que el
     * manejador se entere. Cualquiera de las dos puede ser {@code null} para dejarla como estaba.
     */
    public abstract void setStreams(InputStream i, OutputStream o);

    /**
     * Quien mando el pedido, o {@code null} si el contexto no tenia autenticador.
     *
     * <p>Nunca es {@code null} cuando si lo tenia: un pedido que llego al manejador con
     * autenticador puesto es, por construccion, un pedido autenticado.
     */
    public abstract HttpPrincipal getPrincipal();
}
