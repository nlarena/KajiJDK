package com.sun.tools.jconsole;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * A jconsole plugin: tabs of its own over the watched application.
 *
 * <p>It is installed by {@link java.util.ServiceLoader} --a JAR in `-pluginpath` that declares
 * this class as a provider-- and jconsole creates one instance **per connection**, not a global
 * one.
 *
 * <h2>Why `newSwingWorker` and not a refresh method</h2>
 *
 * <p>A plugin has to read MBeans of a VM that may be on the other side of the network, and the
 * reading takes as long as it takes. If jconsole called it on the interface's thread, a slow
 * plugin would freeze the whole window --including the others' tabs. That is why jconsole does
 * not ask "update yourself": it asks for a {@link SwingWorker}, runs it on its own and lets
 * the plugin separate the two halves as it should.
 *
 * <p>To return `null` is valid, and means "I have nothing to update now".
 *
 * <h2>The context arrives after the constructor</h2>
 *
 * <p>{@link #setContext} is called by jconsole, once, before {@link #getTabs}. That is why
 * {@link #addContextPropertyChangeListener} exists: a plugin that wants to listen to the
 * connection's state cannot do it in its constructor --there is no context yet-- and this
 * method keeps the listener until there is one.
 */
public abstract class JConsolePlugin {

    private volatile JConsoleContext context = null;

    /** The listeners registered before the context arrived. */
    private List<PropertyChangeListener> listeners = null;

    /** For the subclasses. */
    protected JConsolePlugin() {
    }

    /**
     * It gives the plugin its context. It is called by jconsole, a single time.
     *
     * <p>The listeners that were registered before are passed here to the context, and the list is
     * released.
     *
     * @param context the connection, or `null` in order to unhook it
     */
    public final synchronized void setContext(JConsoleContext context) {
        this.context = context;
        if (context != null && this.listeners != null) {
            for (int i = 0; i < this.listeners.size(); i++) {
                context.addPropertyChangeListener(this.listeners.get(i));
            }
            this.listeners = null;
        }
    }

    /** The context, or `null` if jconsole has not given it one yet. */
    public final JConsoleContext getContext() {
        return this.context;
    }

    /**
     * The tabs this plugin adds, by title.
     *
     * <p>jconsole calls it a single time, with the context already set. An empty map is valid and
     * means that the plugin adds no tabs --it may go on doing its work over
     * {@link #newSwingWorker}.
     */
    public abstract Map<String, JPanel> getTabs();

    /**
     * A job for the next refresh, or `null` if there is nothing to do.
     *
     * <p>jconsole calls it at each update interval; see the class note.
     */
    public abstract SwingWorker<?, ?> newSwingWorker();

    /**
     * It releases whatever the plugin has taken.
     *
     * <p>It is called by jconsole on closing the connection's window. By default it does nothing:
     * most plugins have nothing to release.
     */
    public void dispose() {
    }

    /**
     * It listens to the context's properties, now or when there is one.
     *
     * <p>It is the only safe way of hooking on from a plugin's constructor. See the class note.
     *
     * @throws NullPointerException if the listener is null
     */
    public final void addContextPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        JConsoleContext ctx = this.context;
        if (ctx != null) {
            ctx.addPropertyChangeListener(listener);
            return;
        }
        synchronized (this) {
            ctx = this.context;
            if (ctx != null) {
                ctx.addPropertyChangeListener(listener);
                return;
            }
            if (this.listeners == null) {
                this.listeners = new ArrayList<PropertyChangeListener>();
            }
            this.listeners.add(listener);
        }
    }

    /**
     * It takes a listener out, be it already in the context or still waiting.
     *
     * @throws NullPointerException if the listener is null
     */
    public final void removeContextPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        JConsoleContext ctx = this.context;
        if (ctx != null) {
            ctx.removePropertyChangeListener(listener);
            return;
        }
        synchronized (this) {
            if (this.listeners != null) {
                this.listeners.remove(listener);
            }
        }
    }
}
