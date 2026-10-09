package com.sun.nio.sctp;

import java.net.SocketOption;

/**
 * A socket option of SCTP's own.
 *
 * <p>It adds no method to {@link SocketOption}, and even so it is not superfluous: it exists so
 * that the type should tell the options an SCTP channel understands from those it does not.
 * {@link SctpChannel}'s {@code setOption}s ask for this type, so passing them a TCP option does
 * not compile -- an error that otherwise would appear only when running.
 *
 * <p>See {@link SctpStandardSocketOptions} for those the JDK defines.
 *
 * @param <T> the type of the option's value
 */
public interface SctpSocketOption<T> extends SocketOption<T> {
}
