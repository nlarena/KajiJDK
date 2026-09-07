package javax.management.remote.rmi;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import javax.security.auth.Subject;

/**
 * El servidor JMX que se alcanza por JRMP, el protocolo propio de RMI.
 *
 * <h2>Que agrega sobre {@link RMIServerImpl}</h2>
 *
 * <p>Solamente el transporte. La clase de arriba ya sabe llevar la lista de clientes, autenticar y
 * cerrar en cascada; aca esta el puerto, las dos fabricas de sockets y las tres operaciones que
 * hablan con RMI: exportar, dar el stub y desexportar.
 *
 * <h2>Las fabricas de sockets</h2>
 *
 * <p>Son el punto donde se le pone TLS a una conexion JMX. La del cliente viaja
 * <strong>dentro del stub</strong> --por eso tiene que ser serializable--: el cliente recibe el
 * objeto remoto y con el, la instruccion de con que clase de socket hablarle. La del servidor se
 * queda de este lado y decide como se escucha.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>{@link #export} necesita {@link UnicastRemoteObject#exportObject}, que esta VM no tiene:
 * publicar un objeto remoto es abrir un puerto y atender el protocolo, y eso es transporte. Tira
 * {@link UnsupportedOperationException} con el motivo, igual que todo {@code java.rmi.server}.
 *
 * <p>{@link #toStub} tira {@link NoSuchObjectException}, que es exactamente lo que hace el JDK
 * cuando se lo llama sin haber exportado. Aca nunca se exporto, asi que siempre es ese el caso, y no
 * hace falta ninguna excepcion inventada para decirlo.
 *
 * <p>Lo demas --el protocolo, fabricar la conexion del cliente, el cierre-- funciona. La conexion
 * que devuelve {@link #makeClient} es un {@link RMIConnectionImpl} de verdad, que reenvia al
 * {@link javax.management.MBeanServer}; lo que no hay es como hacerla llegar a otra maquina.
 *
 * @since 1.5
 */
public class RMIJRMPServerImpl extends RMIServerImpl {

    private final int port;
    private final RMIClientSocketFactory csf;
    private final RMIServerSocketFactory ssf;

    /**
     * Un servidor JRMP en ese puerto.
     *
     * @param port el puerto; {@code 0} deja que lo elija el sistema
     * @param csf la fabrica de sockets del cliente, o {@code null} para la de siempre
     * @param ssf la fabrica de sockets del servidor, o {@code null} para la de siempre
     * @param env las propiedades de configuracion, o {@code null}
     * @throws IOException si no se pudo crear
     * @throws IllegalArgumentException si el puerto es negativo
     */
    public RMIJRMPServerImpl(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf,
            Map<String, ?> env) throws IOException {
        super(env);
        if (port < 0) {
            throw new IllegalArgumentException("Negative port: " + port);
        }
        this.port = port;
        this.csf = csf;
        this.ssf = ssf;
    }

    /**
     * Publica este objeto por RMI.
     *
     * @throws IOException si no se pudo publicar
     * @throws UnsupportedOperationException en esta VM, que no tiene el transporte de RMI
     */
    @Override
    protected void export() throws IOException {
        // Las dos formas se distinguen porque el JDK las distingue: con las fabricas por omision no
        // se le pasa `null`, se llama a la sobrecarga que no las toma. La diferencia se ve en el
        // stub que le llega al cliente.
        if (csf == null && ssf == null) {
            UnicastRemoteObject.exportObject(this, port);
        } else {
            UnicastRemoteObject.exportObject(this, port, csf, ssf);
        }
    }

    /**
     * El nombre del protocolo.
     *
     * @return {@code "rmi"}
     */
    @Override
    protected String getProtocol() {
        return "rmi";
    }

    /**
     * El objeto remoto que hay que mandarle al cliente.
     *
     * @return el stub
     * @throws NoSuchObjectException si este servidor no esta exportado, que aca es siempre
     */
    @Override
    public Remote toStub() throws IOException {
        throw new NoSuchObjectException("object not exported");
    }

    /**
     * Fabrica la conexion de un cliente y la publica.
     *
     * <p>En el JDK la conexion tambien se exporta, porque el cliente la va a llamar directamente y
     * no a traves de este servidor. Aca se la crea igual --es un {@link RMIConnectionImpl} que
     * funciona-- pero no se la exporta: no hay transporte, y exportar seria lo unico que fallaria de
     * un objeto que por lo demas anda.
     *
     * @param connectionId el identificador que le toca
     * @param subject quien se autentico, o {@code null}
     * @return la conexion
     * @throws IOException si no se pudo crear
     */
    @Override
    protected RMIConnection makeClient(String connectionId, Subject subject) throws IOException {
        if (connectionId == null) {
            throw new NullPointerException("Null connectionId");
        }
        return new RMIConnectionImpl(this, connectionId, getDefaultClassLoader(), subject,
                entorno());
    }

    /**
     * Deja de publicar la conexion de un cliente.
     *
     * <p>En el JDK esto la desexporta, que es lo que la vuelve inalcanzable desde afuera. Aca no hay
     * nada que deshacer, porque {@link #makeClient} nunca la exporto.
     *
     * @param client la conexion
     * @throws IOException si no se pudo cerrar
     * @throws NullPointerException si {@code client} es {@code null}
     */
    @Override
    protected void closeClient(RMIConnection client) throws IOException {
        if (client == null) {
            throw new NullPointerException("Null client");
        }
    }

    /**
     * Deja de publicar este servidor.
     *
     * @throws IOException si no se pudo cerrar
     */
    @Override
    protected void closeServer() throws IOException {
    }
}
