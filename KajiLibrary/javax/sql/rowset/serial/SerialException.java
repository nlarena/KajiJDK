package javax.sql.rowset.serial;

import java.sql.SQLException;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialException -- fallo al copiar un dato SQL a memoria.
 *
 * <p>La lanzan todas las clases de este paquete, y significa una de dos cosas: el dato que se quiso
 * copiar no se pudo leer del servidor, o se pidio una porcion fuera de rango de una copia.
 *
 * <p>Hereda de {@link SQLException} porque quien usa estas clases ya esta atajando errores de base de
 * datos; una jerarquia aparte obligaria a un segundo {@code catch} que haria lo mismo.
 */
public class SerialException extends SQLException {

    private static final long serialVersionUID = -489794565168592690L;

    /** Sin detalle. */
    public SerialException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public SerialException(String msg) {
        super(msg);
    }
}
