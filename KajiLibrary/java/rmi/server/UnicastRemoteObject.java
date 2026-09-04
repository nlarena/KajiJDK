package java.rmi.server;

import java.io.ObjectInputFilter;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * La forma normal de hacer un objeto alcanzable por RMI.
 *
 * <h2>Que quiere decir "unicast"</h2>
 *
 * <p>Que la referencia apunta a <strong>un</strong> objeto, en un proceso, mientras ese proceso
 * viva. Es la unica semantica que quedo: las otras que RMI llego a tener —objetos activables, que se
 * levantaban solos al recibir una llamada— se removieron.
 *
 * <p>Exportar hace dos cosas a la vez: abre el puerto por el que se lo va a alcanzar, y
 * <strong>ancla el objeto</strong> para que el recolector local no se lo lleve mientras haya
 * clientes. De ahi que exista {@link #unexportObject}: sin llamarlo, un objeto exportado no se
 * recolecta nunca.
 *
 * <h2>El filtro de deserializacion</h2>
 *
 * <p>Las sobrecargas con {@link ObjectInputFilter} llegaron despues y son las que hay que preferir.
 * Recibir argumentos por la red es deserializar lo que mande otro, y sin filtro eso acepta cualquier
 * grafo de objetos — que es el vector de los ataques de deserializacion. El filtro acota que clases
 * se admiten <em>antes</em> de construirlas.
 *
 * <h2>En esta VM</h2>
 *
 * <p>Exportar necesita el transporte de RMI, que esta VM no tiene: los {@code exportObject} tiran
 * {@link UnsupportedOperationException} con el motivo, en vez de devolver un stub que no llevaria a
 * ningun lado.
 */
public class UnicastRemoteObject extends RemoteServer {

    private static final long serialVersionUID = 4974527148936298033L;

    /** En un puerto anonimo. */
    protected UnicastRemoteObject() throws RemoteException {
        this(0);
    }

    /** En ese puerto; {@code 0} lo elige el sistema. */
    protected UnicastRemoteObject(int port) throws RemoteException {
        super();
    }

    /** En ese puerto y con esas fabricas de sockets. */
    protected UnicastRemoteObject(int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        super();
    }

    /**
     * Clonar uno de estos exporta la copia.
     *
     * @throws java.rmi.server.ServerCloneException si la copia no se pudo exportar
     */
    public Object clone() throws CloneNotSupportedException {
        throw new ServerCloneException("esta VM no exporta objetos remotos");
    }

    /**
     * Exporta el objeto en un puerto anonimo.
     *
     * @deprecated devuelve un {@link RemoteStub}, que es de la epoca de {@code rmic}; usar
     *     {@link #exportObject(Remote, int)}
     * @throws UnsupportedOperationException en esta VM
     */
    @Deprecated(since = "1.2")
    public static RemoteStub exportObject(Remote obj) throws RemoteException {
        throw new UnsupportedOperationException("esta VM no tiene el transporte de RMI");
    }

    /**
     * Exporta el objeto en ese puerto.
     *
     * @throws UnsupportedOperationException en esta VM
     */
    public static Remote exportObject(Remote obj, int port) throws RemoteException {
        throw new UnsupportedOperationException("esta VM no tiene el transporte de RMI");
    }

    /**
     * Exporta con fabricas de sockets propias; asi es como un objeto exige TLS.
     *
     * @throws UnsupportedOperationException en esta VM
     */
    public static Remote exportObject(Remote obj, int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        throw new UnsupportedOperationException("esta VM no tiene el transporte de RMI");
    }

    /**
     * Exporta con un filtro de deserializacion; ver la nota de la clase.
     *
     * @throws UnsupportedOperationException en esta VM
     */
    public static Remote exportObject(Remote obj, int port, ObjectInputFilter filter)
            throws RemoteException {
        throw new UnsupportedOperationException("esta VM no tiene el transporte de RMI");
    }

    /**
     * Exporta con fabricas propias y filtro.
     *
     * @throws UnsupportedOperationException en esta VM
     */
    public static Remote exportObject(Remote obj, int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf, ObjectInputFilter filter) throws RemoteException {
        throw new UnsupportedOperationException("esta VM no tiene el transporte de RMI");
    }

    /**
     * Deja de exportar el objeto, con lo que puede volver a recolectarse.
     *
     * @param force si desexportar aunque haya llamadas en curso o clientes con referencias
     * @throws NoSuchObjectException si el objeto no estaba exportado — que es siempre, en esta VM
     */
    public static boolean unexportObject(Remote obj, boolean force) throws NoSuchObjectException {
        throw new NoSuchObjectException("el objeto no esta exportado");
    }
}
