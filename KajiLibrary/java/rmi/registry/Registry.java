package java.rmi.registry;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Un registro RMI: la libreta de direcciones desde la que arranca todo cliente.
 *
 * <p>Es el arranque en frio de RMI. Para llamar a un objeto remoto hace falta una referencia, y
 * para conseguir la primera hace falta un lugar conocido de antemano: el registro, que vive en un
 * puerto fijo ({@link #REGISTRY_PORT}) y se busca por nombre y no por referencia. De ahi en mas
 * las referencias viajan como argumentos y valores de retorno, y el registro no vuelve a hacer
 * falta.
 *
 * <p>{@link java.rmi.Naming} es la misma cosa con nombres en forma de URL; esta interfaz es la
 * cruda, con el registro ya localizado.
 *
 * <h2>Quien puede modificarlo</h2>
 *
 * <p>{@link #bind}, {@link #rebind} y {@link #unbind} solo se aceptan desde la **misma maquina**
 * en la que corre el registro; de afuera lanzan {@link AccessException}. {@link #lookup} y
 * {@link #list} no tienen esa restriccion.
 *
 * <p>No es autenticacion: es lo unico que hay. Cualquier proceso de la maquina puede reemplazar
 * cualquier anotacion, y cualquiera que llegue al puerto puede listar todo lo anotado.
 */
public interface Registry extends Remote {

    /** El puerto de siempre: 1099. */
    int REGISTRY_PORT = 1099;

    /**
     * La referencia anotada bajo ese nombre.
     *
     * @throws NotBoundException si no hay nada anotado con ese nombre
     * @throws AccessException si el registro rechaza la llamada
     * @throws RemoteException si falla la comunicacion
     */
    Remote lookup(String name) throws RemoteException, NotBoundException, AccessException;

    /**
     * Anota la referencia bajo ese nombre, si el nombre esta libre.
     *
     * @throws AlreadyBoundException si el nombre ya esta anotado
     * @throws AccessException si la llamada no viene de la maquina del registro
     * @throws RemoteException si falla la comunicacion
     */
    void bind(String name, Remote obj) throws RemoteException, AlreadyBoundException,
            AccessException;

    /**
     * Borra la anotacion de ese nombre.
     *
     * @throws NotBoundException si no habia nada anotado
     * @throws AccessException si la llamada no viene de la maquina del registro
     * @throws RemoteException si falla la comunicacion
     */
    void unbind(String name) throws RemoteException, NotBoundException, AccessException;

    /**
     * Anota la referencia bajo ese nombre, pisando lo que hubiera.
     *
     * @throws AccessException si la llamada no viene de la maquina del registro
     * @throws RemoteException si falla la comunicacion
     */
    void rebind(String name, Remote obj) throws RemoteException, AccessException;

    /**
     * Los nombres anotados, en el momento de la llamada.
     *
     * @throws AccessException si el registro rechaza la llamada
     * @throws RemoteException si falla la comunicacion
     */
    String[] list() throws RemoteException, AccessException;
}
