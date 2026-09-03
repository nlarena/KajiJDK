package org.w3c.dom.ls;

/**
 * KajiLibrary's org.w3c.dom.ls.LSException -- fallo cargar o guardar.
 *
 * <p>Es de las excepciones del W3C, con el mismo diseno que {@code DOMException}: un codigo numerico
 * en un campo <b>publico</b> en vez de una jerarquia de subclases. Viene de que la especificacion
 * esta escrita en IDL y tiene que poder traducirse a lenguajes sin herencia de excepciones.
 *
 * <p>Solo dos codigos, y separan las dos direcciones: {@link #PARSE_ERR} al leer,
 * {@link #SERIALIZE_ERR} al escribir. Es poca informacion a proposito -- el detalle de <b>que</b>
 * estaba mal no va aca sino al manejador de errores, que lo recibe mientras el analisis todavia esta
 * parado en el punto malo y puede decir en que linea fue.
 */
public class LSException extends RuntimeException {

    private static final long serialVersionUID = 5371691160978884690L;

    /** El codigo; publico por la traduccion del IDL. Ver la nota de la clase. */
    public short code;

    /** El documento no se pudo leer. */
    public static final short PARSE_ERR = 81;

    /** El documento no se pudo escribir. */
    public static final short SERIALIZE_ERR = 82;

    /**
     * @param code uno de los dos de arriba
     * @param message que paso
     */
    public LSException(short code, String message) {
        super(message);
        this.code = code;
    }
}
