package java.rmi;

/**
 * KajiLibrary's java.rmi.ServerError -- el servidor tiro un {@link Error}.
 *
 * <p>Un {@code Error} del lado servidor no se puede propagar tal cual: alla significaria que la
 * maquina virtual <b>del cliente</b> esta rota, y no lo esta. Asi que se envuelve en una
 * {@link RemoteException}, que es lo que el cliente ya tiene que atajar.
 *
 * <p>Es el mismo razonamiento que {@code javax.management.remote.JMXServerErrorException}, y no es
 * casualidad: los dos resuelven el problema de cruzar un error de maquina por una red.
 *
 * <p>El nombre confunde: <b>no</b> es un {@code Error}, es una {@code RemoteException} que lleva uno
 * adentro.
 */
public class ServerError extends RemoteException {

    private static final long serialVersionUID = 8455284893909696482L;

    /**
     * @param s el mensaje
     * @param err el error del servidor
     */
    public ServerError(String s, Error err) {
        super(s, err);
    }
}
