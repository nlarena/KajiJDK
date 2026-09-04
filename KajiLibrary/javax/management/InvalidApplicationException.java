package javax.management;

/**
 * Se intento aplicar una consulta a un MBean de una clase que no corresponde.
 *
 * <p>No tiene mensaje ni accesor: el valor ofensivo se guarda pero no se publica. Es asi en el JDK
 * y se respeta -- agregarle un getter que el JDK no tiene seria API inventada.
 */
public class InvalidApplicationException extends Exception {

    private static final long serialVersionUID = -3048022274675537269L;

    /**
     * @serial el objeto sobre el que no se pudo aplicar la consulta
     */
    private Object val;

    /** @param val el objeto sobre el que fallo la aplicacion */
    public InvalidApplicationException(Object val) {
        this.val = val;
    }
}
