package com.sun.net.httpserver;

import java.net.URI;
import java.util.List;

/**
 * La parte de un {@link HttpExchange} que solo se lee: metodo, URI y encabezados.
 *
 * <h2>Para que sirve separar esto del intercambio</h2>
 *
 * <p>Para poder <strong>reescribir un pedido sin tocar la conexion</strong>. {@link #with} devuelve
 * una vista con un encabezado cambiado, y eso alcanza para que un filtro normalice, agregue o
 * corrija algo antes de que lo vea el manejador — sin que exista forma de escribir la respuesta por
 * accidente desde ahi.
 *
 * <p>Es tambien lo que reciben los predicados de {@link HttpHandlers#handleOrElse}: elegir manejador
 * es una decision que solo mira el pedido, y darle el intercambio entero seria darle de mas.
 */
public interface Request {

    /** La URI pedida. */
    URI getRequestURI();

    /** El metodo HTTP. */
    String getRequestMethod();

    /** Los encabezados del pedido. */
    Headers getRequestHeaders();

    /**
     * Una vista de este pedido con {@code headerName} puesto en {@code headerValues}.
     *
     * <p>El pedido original no cambia: lo que se devuelve es otra vista. Es lo que hace que un
     * filtro pueda ajustar lo que ve el manejador sin efectos sobre nada mas.
     */
    default Request with(String headerName, List<String> headerValues) {
        Request original = this;
        Headers combinados = new Headers(original.getRequestHeaders());
        combinados.put(headerName, headerValues);
        return new RequestReescrito(original, combinados);
    }
}
