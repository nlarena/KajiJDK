package com.sun.jdi.connect;

/**
 * El medio por el que un {@link Connector} habla con la VM depurada.
 *
 * <p>Un solo metodo, y no le falta nada: el transporte es una **etiqueta**. JDWP corre sobre lo que
 * sea que mueva bytes en orden --sockets ({@code dt_socket}), memoria compartida
 * ({@code dt_shmem})-- y lo unico que el depurador necesita saber de el es su nombre, para que los
 * dos extremos se pongan de acuerdo.
 *
 * <p>La mecanica de verdad --abrir, aceptar, leer, escribir-- vive en
 * {@link com.sun.jdi.connect.spi.TransportService}, que es la cara del proveedor. Esta es la cara
 * del cliente.
 */
public interface Transport {

    /** El nombre del transporte, por ejemplo {@code "dt_socket"}. */
    String name();
}
