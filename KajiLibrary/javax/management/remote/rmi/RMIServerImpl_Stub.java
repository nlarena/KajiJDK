package javax.management.remote.rmi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteStub;

/**
 * El stub de {@link RMIServerImpl}: lo que el cliente tiene en la mano.
 *
 * <h2>Que es un stub</h2>
 *
 * <p>Un objeto que implementa la misma interfaz que el de alla y no hace nada por si mismo: cada
 * metodo empaqueta los argumentos y se los pasa a la {@link RemoteRef}, que es la que sabe llegar
 * hasta la otra maquina. El {@link Hash numero} que acompana a cada llamada es lo que le dice al
 * otro lado cual de los metodos se invoco.
 *
 * <p>Que sea codigo tan mecanico no es casualidad: en el JDK esta clase la genera {@code rmic}. Aca
 * esta escrita, con la unica diferencia de que los numeros se calculan al cargar la clase en vez de
 * estar puestos como literales.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>La clase funciona: se construye, calcula bien los numeros y llama a la referencia. Lo que no
 * hay es de donde sacar una {@link RemoteRef} viva, porque eso es el transporte. Un stub construido
 * con una referencia que no lleva a ningun lado falla en la referencia, que es donde corresponde.
 *
 * @since 1.5
 */
public final class RMIServerImpl_Stub extends RemoteStub implements RMIServer {

    private static final long serialVersionUID = 2L;

    private static Method $method_getVersion_0;
    private static Method $method_newClient_1;

    private static long $hash_getVersion_0;
    private static long $hash_newClient_1;

    static {
        try {
            $method_getVersion_0 = RMIServer.class.getMethod("getVersion");
            $method_newClient_1 = RMIServer.class.getMethod("newClient", Object.class);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError("stub class initialization failed");
        }
        $hash_getVersion_0 = Hash.de($method_getVersion_0);
        $hash_newClient_1 = Hash.de($method_newClient_1);
    }

    /**
     * Un stub sobre esa referencia.
     *
     * @param ref la referencia que sabe llegar al objeto remoto
     */
    public RMIServerImpl_Stub(RemoteRef ref) {
        super(ref);
    }

    /**
     * La version del protocolo y del proveedor, preguntada al otro lado.
     *
     * @return la version
     * @throws RemoteException si no se pudo hablar con el servidor
     */
    public String getVersion() throws RemoteException {
        try {
            final Object r = ref.invoke(this, $method_getVersion_0, null, $hash_getVersion_0);
            return (String) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Le pide una conexion al servidor.
     *
     * @param credentials la credencial, o {@code null}
     * @return la conexion
     * @throws IOException si no se pudo abrir
     */
    public RMIConnection newClient(Object credentials) throws IOException {
        try {
            final Object r = ref.invoke(this, $method_newClient_1, new Object[] {credentials},
                    $hash_newClient_1);
            return (RMIConnection) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }
}
