package javax.naming.ldap;

import java.io.Serializable;

/**
 * La respuesta a un {@link ExtendedRequest}.
 *
 * <p>Deliberadamente flaca: OID y bytes. Lo que <em>significan</em> esos bytes lo sabe la subclase
 * concreta, que es la que agrega los accesores con sentido — ver {@link StartTlsResponse}, cuya
 * respuesta no lleva datos y en cambio ofrece {@code negotiate()}.
 */
public interface ExtendedResponse extends Serializable {

    /** El OID de la operacion que la genero, o {@code null}. */
    String getID();

    /** La respuesta codificada en BER, o {@code null} si no lleva datos. */
    byte[] getEncodedValue();
}
