package java.rmi;

/**
 * KajiLibrary's java.rmi.ServerRuntimeException -- el servidor tiro una excepcion no comprobada.
 *
 * <p>Obsoleta desde 1.2. Antes, una {@link RuntimeException} del metodo remoto se envolvia aca; ahora
 * se propaga al cliente <b>tal cual</b>, sin envolver.
 *
 * <p>El cambio fue a mejor y vale entender por que: envolverla obligaba al cliente a desenvolver para
 * poder atrapar lo que le interesaba, y perdia la posibilidad de escribir un {@code catch} del tipo
 * concreto. Como una excepcion no comprobada significa lo mismo de los dos lados --alguien programo
 * mal-- no hay razon para traducirla, a diferencia de un {@link Error}, que si tiene
 * {@link ServerError}.
 *
 * <p>Se mantiene para que el codigo viejo compile.
 */
@Deprecated
public class ServerRuntimeException extends RemoteException {

    private static final long serialVersionUID = 7054464920481467219L;

    /**
     * @param s el mensaje
     * @param ex la original
     */
    @Deprecated
    public ServerRuntimeException(String s, Exception ex) {
        super(s, ex);
    }
}
