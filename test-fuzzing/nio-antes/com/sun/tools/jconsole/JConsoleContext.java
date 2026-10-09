package com.sun.tools.jconsole;

import java.beans.PropertyChangeListener;
import javax.management.MBeanServerConnection;

/**
 * jconsole's connection with **one** watched application.
 *
 * <p>It is what a plugin receives in order to do its work: over
 * {@link #getMBeanServerConnection()} it reaches the application's MBeans, which is where
 * everything jconsole shows comes from.
 *
 * <p>The state is a bound property --{@link #CONNECTION_STATE_PROPERTY}-- and not a datum that
 * is consulted every so often, because a JMX connection falls by itself: the watched
 * application may finish, or the network be cut, at any moment and with no warning. A plugin
 * that does not listen to that change is left drawing old data.
 *
 * <p>It is implemented by jconsole, not by the plugin.
 */
public interface JConsoleContext {

    /** The name of the bound property of the connection's state. */
    String CONNECTION_STATE_PROPERTY = "connectionState";

    /**
     * The connection with the watched application's MBean server.
     *
     * <p>It may be down: check {@link #getConnectionState()} before using it.
     */
    MBeanServerConnection getMBeanServerConnection();

    /** What state the connection is in. */
    ConnectionState getConnectionState();

    /**
     * It adds a listener of this connection's properties.
     *
     * <p>A plugin does not usually call it directly: for that there is
     * {@link JConsolePlugin#addContextPropertyChangeListener}, which besides survives a change of
     * context.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /** It takes a listener out. */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /** What state a jconsole connection is in. */
    enum ConnectionState {

        /** Connected: the MBeans may be consulted. */
        CONNECTED,

        /** Down. It may come back: jconsole retries. */
        DISCONNECTED,

        /** Connecting. It is a state of its own and not a `DISCONNECTED` because it lasts: the
                 * initial connection to a remote VM may take a while, and the interface has to be
                 * able to say "waiting" instead of "there is none". */
        CONNECTING
    }
}
