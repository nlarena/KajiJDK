package java.rmi.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * El manejador que convierte una llamada sobre un proxy en una llamada remota.
 *
 * <h2>Lo que reemplazo</h2>
 *
 * <p>Antes de esto, cada interfaz remota necesitaba un stub generado por {@code rmic} y compilado
 * junto con la aplicacion — un paso extra en el build y un archivo que se desincronizaba. Un proxy
 * dinamico con este manejador hace lo mismo <strong>en tiempo de ejecucion</strong>: la llamada
 * llega como un {@link Method} y se reenvia por la {@link RemoteRef}.
 *
 * <p>Hereda de {@link RemoteObject} y no solo implementa {@link InvocationHandler} porque necesita
 * la identidad remota: {@code equals} y {@code hashCode} sobre un proxy tienen que comparar el
 * objeto del otro lado, y eso ya esta resuelto en la clase base.
 */
public class RemoteObjectInvocationHandler extends RemoteObject implements InvocationHandler {

    private static final long serialVersionUID = 2L;

    /**
     * @throws NullPointerException si la referencia es {@code null}
     */
    public RemoteObjectInvocationHandler(RemoteRef ref) {
        super(ref);
        if (ref == null) {
            throw new NullPointerException("ref");
        }
    }

    /**
     * Reenvia la llamada al objeto remoto.
     *
     * <p>Los tres metodos de {@link Object} se atienden localmente y no viajan: preguntarle a otra
     * maquina cuanto vale el {@code hashCode} de su objeto seria un viaje de red por cada uso en un
     * mapa, y para {@code equals} ademas daria mal — hay que comparar referencias, no objetos.
     *
     * @throws IllegalArgumentException si el metodo no es de una interfaz remota
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            String nombre = method.getName();
            if (nombre.equals("hashCode")) {
                return Integer.valueOf(hashCode());
            }
            if (nombre.equals("equals")) {
                Object otro = args[0];
                return Boolean.valueOf(proxy == otro
                        || (otro != null && java.lang.reflect.Proxy.isProxyClass(otro.getClass())
                                && equals(java.lang.reflect.Proxy.getInvocationHandler(otro))));
            }
            if (nombre.equals("toString")) {
                return proxy.getClass().getName() + "[" + getRef().remoteToString() + "]";
            }
            throw new IllegalArgumentException("metodo inesperado: " + nombre);
        }
        return getRef().invoke((java.rmi.Remote) proxy, method, args, -1L);
    }
}
