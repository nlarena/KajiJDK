package javax.management;

import java.io.Serializable;

/**
 * La base de las expresiones de consulta: lleva el servidor contra el que resolver los atributos.
 *
 * <p>El servidor vive en un `ThreadLocal` **estatico**, y eso es lo que hay que entender de esta
 * clase. La alternativa obvia --un campo por expresion-- obligaria a recorrer el arbol entero
 * fijandolo en cada nodo antes de cada evaluacion, y ademas haria que una misma consulta no se
 * pudiera evaluar en dos hilos a la vez. Con el `ThreadLocal`, el que evalua lo pone una vez y todos
 * los nodos lo encuentran, cada hilo el suyo.
 *
 * <p>Ese es tambien el motivo de que {@code setMBeanServer} este obsoleto en {@link QueryExp} y en
 * {@link ValueExp}: no hace falta llamarlo.
 */
public abstract class QueryEval implements Serializable {

    private static final long serialVersionUID = 2675899265640874796L;

    private static ThreadLocal<MBeanServer> server = new ThreadLocal<MBeanServer>();

    /** Fija el servidor **de este hilo**. */
    public void setMBeanServer(MBeanServer s) {
        server.set(s);
    }

    /** El servidor de este hilo, o `null` si nadie lo fijo. */
    public static MBeanServer getMBeanServer() {
        return server.get();
    }
}
