package java.rmi.dgc;

import java.io.Serializable;

/**
 * El permiso, con vencimiento, que el servidor le da a un cliente para retener una referencia.
 *
 * <p>Es la pieza que hace que el recolector distribuido tolere que un cliente se muera sin avisar:
 * en vez de esperar un `clean` que quiza nunca llegue, el servidor entrega la referencia por un
 * plazo, y el cliente que la quiere conservar tiene que volver a pedirla antes de que venza. Si el
 * cliente desaparece, la referencia se libera sola cuando pasa el plazo.
 *
 * <p>Es inmutable, y tiene que serlo: viaja serializado y lo leen los dos lados de la conexion.
 *
 * @see DGC#dirty(java.rmi.server.ObjID[], long, Lease)
 */
public final class Lease implements Serializable {

    private static final long serialVersionUID = -5713411624328831948L;

    /** La VM a la que se le dio el permiso. */
    private VMID vmid;

    /** Cuanto dura, en milisegundos. */
    private long value;

    /**
     * Un permiso para la VM dada, por la duracion dada.
     *
     * @param id la VM del cliente; puede ser nulo, y entonces el servidor le asigna uno
     * @param duration la duracion en milisegundos
     */
    public Lease(VMID id, long duration) {
        this.vmid = id;
        this.value = duration;
    }

    /** La VM a la que se le dio este permiso, o `null` si no se le asigno ninguna. */
    public VMID getVMID() {
        return this.vmid;
    }

    /** La duracion del permiso, en milisegundos. */
    public long getValue() {
        return this.value;
    }
}
