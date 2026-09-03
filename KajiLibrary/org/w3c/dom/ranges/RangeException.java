package org.w3c.dom.ranges;

/**
 * KajiLibrary's org.w3c.dom.ranges.RangeException -- una operacion de rango imposible.
 *
 * <p>Es <b>no chequeada</b>, igual que {@code DOMException}, y por el mismo motivo: los dos errores
 * que reporta son de programa y no de datos. Obligar a atrapar lo que no puede pasar en codigo
 * correcto ensuciaria cada llamada.
 *
 * <p>El codigo va en un campo publico y no en un accesor. Es la convencion del DOM -- las interfaces
 * se definen en IDL, que no tiene propiedades -- y se reproduce tal cual aunque hoy no se escribiria
 * asi.
 */
public class RangeException extends RuntimeException {

    private static final long serialVersionUID = 2427623564573316628L;

    /**
     * Los dos extremos no delimitan un rango: estan en arboles distintos, o el final va antes que el
     * principio.
     */
    public static final short BAD_BOUNDARYPOINTS_ERR = 1;

    /** Ese tipo de nodo no puede ser el contenedor de un extremo, o no se puede seleccionar. */
    public static final short INVALID_NODE_TYPE_ERR = 2;

    /** Cual de los dos. Publico por la convencion del DOM; ver la nota de la clase. */
    public short code;

    public RangeException(short code, String message) {
        super(message);
        this.code = code;
    }
}
