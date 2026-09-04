package java.rmi.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.rmi.Remote;

/**
 * El handle de un objeto remoto: donde vive y como llamarlo.
 *
 * <p>Es lo que un stub lleva adentro. Que sea {@link Externalizable} es la razon de que un stub
 * pueda viajar: al serializarlo se escribe primero el nombre de la clase de la referencia
 * ({@link #getRefClass}) y despues sus datos, de modo que el otro lado sepa que implementacion
 * reconstruir.
 *
 * <p>{@link #remoteHashCode} y {@link #remoteEquals} existen porque la identidad de un objeto
 * remoto es <strong>la de su referencia</strong>, no la del stub: dos stubs distintos que apuntan al
 * mismo objeto tienen que ser iguales, y eso no sale de {@code Object}.
 */
public interface RemoteRef extends Externalizable {

    /** @deprecated la parte de {@code newCall}/{@code invoke(RemoteCall)} quedo obsoleta. */
    @Deprecated(since = "1.2")
    static final String packagePrefix = "sun.rmi.server";

    static final long serialVersionUID = 3632638527362204081L;

    /** Invoca el metodo en el objeto remoto y devuelve el resultado. */
    Object invoke(Remote obj, Method method, Object[] params, long opnum) throws Exception;

    /**
     * @deprecated de la epoca de los stubs generados; usar {@link #invoke(Remote, Method, Object[], long)}
     */
    @Deprecated(since = "1.2")
    RemoteCall newCall(RemoteObject obj, Operation[] op, int opnum, long hash) throws RemoteException;

    /** @deprecated idem. */
    @Deprecated(since = "1.2")
    void invoke(RemoteCall call) throws Exception;

    /** @deprecated idem. */
    @Deprecated(since = "1.2")
    void done(RemoteCall call) throws RemoteException;

    /** El nombre de la clase de esta referencia, para poder reconstruirla del otro lado. */
    String getRefClass(ObjectOutput out);

    /** El hash del objeto remoto, no el del stub. */
    int remoteHashCode();

    /** Si apuntan al mismo objeto remoto. */
    boolean remoteEquals(RemoteRef obj);

    /** Una descripcion de la referencia. */
    String remoteToString();
}
