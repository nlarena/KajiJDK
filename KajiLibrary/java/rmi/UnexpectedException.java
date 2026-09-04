package java.rmi;

/**
 * KajiLibrary's java.rmi.UnexpectedException -- el metodo remoto lanzo algo que no declaraba.
 *
 * <p>Un metodo remoto declara sus excepciones comprobadas, y el talon del cliente solo puede
 * propagarlas si estan en la firma. Si el servidor manda una comprobada que <b>no</b> esta declarada
 * --porque el servidor se compilo contra otra version de la interfaz-- el talon no puede lanzarla sin
 * romper el compilador, y la envuelve aca.
 *
 * <p>En la practica significa casi siempre lo mismo: cliente y servidor tienen versiones distintas de
 * la interfaz remota.
 */
public class UnexpectedException extends RemoteException {

    private static final long serialVersionUID = 1800467484195073863L;

    /** @param s el mensaje */
    public UnexpectedException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la que llego sin estar declarada
     */
    public UnexpectedException(String s, Exception ex) {
        super(s, ex);
    }
}
