package java.rmi.dgc;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ObjID;

/**
 * El recolector distribuido, visto desde el cliente.
 *
 * <p>Cada VM que exporta objetos remotos exporta tambien un `DGC` bajo el identificador fijo
 * {@code ObjID.DGC_ID}. El protocolo son dos llamadas y una idea: **contar referencias con
 * vencimiento**.
 *
 * <ul>
 *   <li>{@link #dirty} lo llama el cliente cuando recibe una referencia a un objeto de este
 *       servidor. El servidor anota que esa VM lo tiene y devuelve un {@link Lease}: el permiso
 *       vence, asi que un cliente que se cuelgue o se muera no deja la referencia viva para
 *       siempre. El cliente que quiera seguir teniendola tiene que renovar antes del vencimiento.
 *   <li>{@link #clean} lo llama el cliente cuando suelta la referencia. Es la via rapida: sin ella
 *       el servidor igual libera al vencer el plazo, pero tarde.
 * </ul>
 *
 * <p>Cuando no queda ninguna referencia --ni local ni remota-- el objeto queda a merced del
 * recolector de siempre.
 *
 * <p>El contador de referencias no ve ciclos entre VM: dos objetos en dos servidores que se
 * apuntan mutuamente no se liberan nunca. Es una limitacion conocida del diseño, no un descuido.
 */
public interface DGC extends Remote {

    /**
     * Pide, o renueva, el permiso para retener las referencias dadas.
     *
     * @param ids los objetos que el cliente quiere retener
     * @param sequenceNum el numero de secuencia de la llamada, para que el servidor descarte las
     *     que le lleguen fuera de orden --con `dirty` y `clean` cruzados, el orden importa
     * @param lease el permiso que pide: solo la duracion es una peticion, el servidor decide
     * @return el permiso concedido, con la duracion que el servidor haya decidido
     * @throws RemoteException si falla la llamada
     */
    Lease dirty(ObjID[] ids, long sequenceNum, Lease lease) throws RemoteException;

    /**
     * Avisa que el cliente solto las referencias dadas.
     *
     * @param ids los objetos que el cliente ya no retiene
     * @param sequenceNum el numero de secuencia de la llamada
     * @param vmid la VM que las suelta
     * @param strong si la llamada tiene que ganarle a un `dirty` anterior aunque llegue con un
     *     numero de secuencia menor
     * @throws RemoteException si falla la llamada
     */
    void clean(ObjID[] ids, long sequenceNum, VMID vmid, boolean strong) throws RemoteException;
}
