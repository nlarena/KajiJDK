package com.sun.jdi.connect;

/**
 * The medium a {@link Connector} talks to the debugged VM through.
 *
 * <p>A single method, and it lacks nothing: the transport is a **label**. JDWP runs over
 * whatever moves bytes in order -- sockets ({@code dt_socket}), shared memory
 * ({@code dt_shmem}) -- and the only thing the debugger needs to know about it is its name, so
 * that the two ends agree.
 *
 * <p>The real mechanics -- opening, accepting, reading, writing -- live in
 * {@link com.sun.jdi.connect.spi.TransportService}, which is the provider's face. This is the
 * client's face.
 */
public interface Transport {

    /** The transport's name, for instance {@code "dt_socket"}. */
    String name();
}
