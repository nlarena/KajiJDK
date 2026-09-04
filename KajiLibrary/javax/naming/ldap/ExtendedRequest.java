package javax.naming.ldap;

import java.io.Serializable;
import javax.naming.NamingException;

/**
 * Una operacion que LDAP no define, identificada por un OID.
 *
 * <h2>La otra mitad de la extensibilidad</h2>
 *
 * <p>Un {@link Control} modifica una operacion que ya existe; esto <strong>agrega una nueva</strong>.
 * Es como se hacen el cambio de contrasena, la cancelacion de una operacion en curso o el
 * {@link StartTlsRequest} de este mismo paquete.
 *
 * <h2>Por que el pedido fabrica su propia respuesta</h2>
 *
 * <p>{@link #createExtendedResponse} sorprende hasta que se ve el motivo: el proveedor LDAP recibe
 * del servidor un OID y unos bytes, y <strong>no sabe interpretarlos</strong> — la operacion es de
 * quien la definio. El unico que sabe que tipo construir es el pedido que la origino, asi que el
 * proveedor le devuelve los bytes crudos y le pide que arme el objeto.
 *
 * <p>Es inversion de control, y es lo que permite agregar una operacion sin tocar el proveedor.
 */
public interface ExtendedRequest extends Serializable {

    /** El OID de la operacion. */
    String getID();

    /** Los argumentos codificados en BER, o {@code null} si no lleva. */
    byte[] getEncodedValue();

    /**
     * Arma la respuesta a partir de lo que llego.
     *
     * @param id el OID que devolvio el servidor, que puede ser {@code null}
     * @param berValue el buffer con la respuesta, o {@code null}
     * @param offset donde empieza dentro del buffer
     * @param length cuantos bytes ocupa
     * @throws NamingException si la respuesta no se pudo construir
     */
    ExtendedResponse createExtendedResponse(String id, byte[] berValue, int offset, int length)
            throws NamingException;
}
