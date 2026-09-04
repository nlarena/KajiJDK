package java.rmi;

/**
 * KajiLibrary's java.rmi.AccessException -- El registro no deja hacer eso.
 *
 * <p>Sale de {@code bind}, {@code rebind} y {@code unbind} cuando el que llama no esta autorizado.
 *
 * <p>La regla es de siempre y sorprende: un registro RMI solo acepta esas tres operaciones desde la
 * <b>misma maquina</b>. Un cliente remoto puede buscar y listar, no puede modificar. Es la unica
 * defensa que tiene un registro, que por lo demas no autentica a nadie.
 */
public class AccessException extends RemoteException {

    private static final long serialVersionUID = 6314925228044966088L;

    /** @param s el mensaje */
    public AccessException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public AccessException(String s, Exception ex) {
        super(s, ex);
    }
}
