package javax.management.remote;

import javax.management.MBeanServer;

/**
 * KajiLibrary's javax.management.remote.MBeanServerForwarder -- an {@link MBeanServer} that wraps
 * another.
 *
 * <p>It is a complete {@code MBeanServer} plus two methods to chain it. It is put between the
 * connector and the real server, and there it can log every operation, filter by permissions, or
 * cache.
 *
 * <p>Several are chained: each one points at the next and the last at the real server. It is built
 * with {@code JMXConnectorServer.setMBeanServerForwarder}, which puts each new one in front of
 * what was already there.
 *
 * <p>The order matters and is the reverse of what it looks like: the <b>last</b> one added is the
 * <b>first</b> to see the calls.
 */
public interface MBeanServerForwarder extends MBeanServer {

    /** Who it delegates to. */
    MBeanServer getMBeanServer();

    /** Changes who it delegates to. */
    void setMBeanServer(MBeanServer mbs);
}
