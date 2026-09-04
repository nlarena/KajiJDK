package com.sun.nio.sctp;

import java.net.SocketOption;

/**
 * Una opcion de socket propia de SCTP.
 *
 * <p>No agrega ningun metodo a {@link SocketOption}, y aun asi no sobra: existe para que el tipo
 * distinga las opciones que un canal SCTP entiende de las que no. Los {@code setOption} de
 * {@link SctpChannel} piden este tipo, asi que pasarles una opcion de TCP no compila — un error que
 * de otro modo apareceria recien al correr.
 *
 * <p>Ver {@link SctpStandardSocketOptions} para las que define el JDK.
 *
 * @param <T> el tipo del valor de la opcion
 */
public interface SctpSocketOption<T> extends SocketOption<T> {
}
