package com.sun.tools.jconsole;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * Un complemento de jconsole: solapas propias sobre la aplicacion vigilada.
 *
 * <p>Se instala por {@link java.util.ServiceLoader} --un JAR en `-pluginpath` que declare esta
 * clase como proveedor-- y jconsole crea una instancia **por conexion**, no una global.
 *
 * <h2>Por que `newSwingWorker` y no un metodo de refresco</h2>
 *
 * <p>Un complemento tiene que leer MBean de una VM que puede estar del otro lado de la red, y la
 * lectura tarda lo que tarde. Si jconsole lo llamara en el hilo de la interfaz, un complemento
 * lento congelaria la ventana entera --incluidas las solapas de los demas. Por eso jconsole no
 * pide "actualizate": pide un {@link SwingWorker}, lo corre por su cuenta y deja que el complemento
 * separe las dos mitades como corresponde.
 *
 * <p>Devolver `null` es valido, y significa "no tengo nada que actualizar ahora".
 *
 * <h2>El contexto llega despues del constructor</h2>
 *
 * <p>{@link #setContext} lo llama jconsole, una vez, antes de {@link #getTabs}. Por eso
 * {@link #addContextPropertyChangeListener} existe: un complemento que quiera escuchar el estado de
 * la conexion no puede hacerlo en su constructor --todavia no hay contexto-- y este metodo guarda
 * el escucha hasta que lo haya.
 */
public abstract class JConsolePlugin {

    private volatile JConsoleContext context = null;

    /** Los escuchas registrados antes de que llegara el contexto. */
    private List<PropertyChangeListener> listeners = null;

    /** Para las subclases. */
    protected JConsolePlugin() {
    }

    /**
     * Le da al complemento su contexto. Lo llama jconsole, una sola vez.
     *
     * <p>Los escuchas que se hayan registrado antes se pasan aca al contexto, y la lista se suelta.
     *
     * @param context la conexion, o `null` para desengancharlo
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

    /** El contexto, o `null` si jconsole todavia no se lo dio. */
    public final JConsoleContext getContext() {
        return this.context;
    }

    /**
     * Las solapas que este complemento agrega, por titulo.
     *
     * <p>jconsole lo llama una sola vez, con el contexto ya puesto. Un mapa vacio es valido y
     * significa que el complemento no agrega solapas --puede seguir haciendo su trabajo por
     * {@link #newSwingWorker}.
     */
    public abstract Map<String, JPanel> getTabs();

    /**
     * Un trabajo para el proximo refresco, o `null` si no hay nada que hacer.
     *
     * <p>jconsole lo llama en cada intervalo de actualizacion; ver la nota de la clase.
     */
    public abstract SwingWorker<?, ?> newSwingWorker();

    /**
     * Suelta lo que el complemento haya tomado.
     *
     * <p>Lo llama jconsole al cerrar la ventana de la conexion. Por omision no hace nada: la
     * mayoria de los complementos no tienen nada que soltar.
     */
    public void dispose() {
    }

    /**
     * Escucha las propiedades del contexto, ahora o cuando lo haya.
     *
     * <p>Es la unica forma segura de engancharse desde el constructor de un complemento. Ver la
     * nota de la clase.
     *
     * @throws NullPointerException si el escucha es nulo
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
     * Saca un escucha, este ya en el contexto o todavia en espera.
     *
     * @throws NullPointerException si el escucha es nulo
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
