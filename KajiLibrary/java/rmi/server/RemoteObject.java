package java.rmi.server;

import java.io.Serializable;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;

/**
 * La base de todo objeto remoto y de todo stub.
 *
 * <h2>Por que {@code equals} y {@code hashCode} estan aca</h2>
 *
 * <p>Porque para un objeto remoto la identidad <strong>no es la del objeto Java</strong>. Dos stubs
 * distintos que apuntan al mismo objeto del otro lado tienen que ser iguales, y con la
 * implementacion de {@link Object} no lo serian nunca. Esta clase delega en la
 * {@link RemoteRef}, que es la que sabe a que apunta.
 *
 * <p>Sin eso, guardar stubs en un {@code HashSet} guardaria duplicados de la misma cosa.
 *
 * <p>El campo {@link #ref} es {@code transient} porque no se serializa como un campo comun: se
 * escribe a mano, con el nombre de su clase adelante, para que el otro lado sepa que implementacion
 * reconstruir.
 */
public abstract class RemoteObject implements Remote, Serializable {

    private static final long serialVersionUID = -3215090123894869218L;

    /** A que objeto remoto apunta esto. */
    protected transient RemoteRef ref;

    /** Sin referencia todavia. */
    protected RemoteObject() {
        this.ref = null;
    }

    /** Con esa referencia. */
    protected RemoteObject(RemoteRef newref) {
        this.ref = newref;
    }

    /** La referencia. */
    public RemoteRef getRef() {
        return this.ref;
    }

    /**
     * El stub del objeto remoto que se le pase.
     *
     * @throws NoSuchObjectException si el objeto no esta exportado
     */
    public static Remote toStub(Remote obj) throws NoSuchObjectException {
        if (obj instanceof RemoteStub) {
            return obj;
        }
        throw new NoSuchObjectException("el objeto no esta exportado");
    }

    /** El del objeto remoto, no el del stub. */
    public int hashCode() {
        return this.ref == null ? super.hashCode() : this.ref.remoteHashCode();
    }

    /**
     * Si los dos apuntan al mismo objeto remoto.
     *
     * <p>Un {@code RemoteObject} sin referencia cae en la igualdad por identidad de
     * {@link Object}, que es lo unico sensato: sin referencia no hay nada que comparar.
     */
    public boolean equals(Object obj) {
        if (obj instanceof RemoteObject) {
            if (this.ref == null) {
                return obj == this;
            }
            return this.ref.remoteEquals(((RemoteObject) obj).ref);
        }
        return obj != null && obj.equals(this);
    }

    public String toString() {
        String nombre = this.getClass().getName();
        return this.ref == null ? nombre : nombre + "[" + this.ref.remoteToString() + "]";
    }
}
