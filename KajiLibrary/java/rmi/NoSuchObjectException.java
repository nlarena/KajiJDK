package java.rmi;

/**
 * KajiLibrary's java.rmi.NoSuchObjectException -- Ese objeto ya no esta en el servidor.
 *
 * <p>El objeto remoto no existe mas en la maquina virtual del servidor: se lo exporto y despues se
 * lo dio de baja, o el servidor se reinicio.
 *
 * <p>Es <b>final</b> en el sentido practico: reintentar con la misma referencia no va a funcionar
 * nunca. Hay que volver a buscar en el registro para conseguir una referencia nueva.
 */
public class NoSuchObjectException extends RemoteException {

    private static final long serialVersionUID = 6619395951570472985L;

    /** @param s el mensaje */
    public NoSuchObjectException(String s) {
        super(s);
    }
}
