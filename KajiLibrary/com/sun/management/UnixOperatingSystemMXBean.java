package com.sun.management;

/**
 * What may be asked of the operating system <strong>only</strong> on Unix.
 *
 * <p>They are the file descriptors, which on Unix are a counted and exhaustible resource: each
 * socket, each open file and each pipe spends one, and on reaching the top the process stops
 * being able to open anything. It is one of the commonest causes of a server's stopping
 * accepting connections without the memory or the CPU showing anything strange.
 *
 * <p>It is separated into an interface of its own, and not added to
 * {@link OperatingSystemMXBean}, because in Windows the question has no answer. Whoever wants
 * the datum has to ask first whether the bean is of this type, and that is exactly what is
 * intended.
 *
 * @since 1.5
 */
public interface UnixOperatingSystemMXBean extends OperatingSystemMXBean {

    /**
     * How many descriptors the process has open now.
     *
     * @return the number
     */
    long getOpenFileDescriptorCount();

    /**
     * How many it may have open at most.
     *
     * @return the top
     */
    long getMaxFileDescriptorCount();
}
