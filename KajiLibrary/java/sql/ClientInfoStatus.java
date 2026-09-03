package java.sql;

/**
 * KajiLibrary's java.sql.ClientInfoStatus -- por que no se pudo fijar una propiedad del cliente.
 *
 * <p>Existe porque `setClientInfo` puede fallar en **algunas** propiedades y no en otras: no es un
 * exito o un fracaso sino un mapa de propiedad a razon, y este enum es el codominio de ese mapa.
 */
public enum ClientInfoStatus {

    /** No se sabe por que. */
    REASON_UNKNOWN,

    /** El servidor no conoce esa propiedad. */
    REASON_UNKNOWN_PROPERTY,

    /** El valor no sirve para esa propiedad. */
    REASON_VALUE_INVALID,

    /** El valor era mas largo de lo que la propiedad admite. */
    REASON_VALUE_TRUNCATED
}
