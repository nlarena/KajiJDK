package javax.security.sasl;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

/**
 * KajiLibrary's javax.security.sasl.SaslClientFactory -- de donde salen los {@link SaslClient}.
 *
 * <p>Se registra como servicio de un proveedor de seguridad, con el tipo
 * {@code "SaslClientFactory"} y el nombre del mecanismo como algoritmo. Una misma fabrica puede
 * atender varios mecanismos, y por eso {@link #getMechanismNames} devuelve un arreglo.
 *
 * <p>Ese metodo recibe las propiedades, y ahi esta el detalle interesante: la lista de mecanismos
 * que una fabrica ofrece <b>depende de la politica</b>. Con {@link Sasl#POLICY_NOPLAINTEXT} puesto,
 * una fabrica que sabe PLAIN no lo debe listar. Es lo que permite que la seleccion de mecanismo
 * respete la politica sin que quien llama tenga que conocerlos uno por uno.
 *
 * <p>{@link #createSaslClient} puede devolver null: significa que, con esas propiedades y ese
 * manejador, esta fabrica no puede atender ninguno de los mecanismos pedidos. No es un error, y por
 * eso {@link Sasl#createSaslClient} sigue con la que viene.
 */
public interface SaslClientFactory {

    /**
     * Un cliente para el primero de esos mecanismos que esta fabrica pueda.
     *
     * @param mechanisms los mecanismos aceptables, en orden de preferencia
     * @param authorizationId en nombre de quien actuar, o null para el autenticado
     * @param protocol el protocolo de arriba, por ejemplo {@code "ldap"}
     * @param serverName el nombre del servidor
     * @param props la configuracion; ver las constantes de {@link Sasl}
     * @return null si no puede con ninguno
     */
    SaslClient createSaslClient(String[] mechanisms, String authorizationId, String protocol,
                                String serverName, Map<String, ?> props, CallbackHandler cbh)
        throws SaslException;

    /** Los mecanismos que ofrece con esas propiedades. Ver la nota de la clase. */
    String[] getMechanismNames(Map<String, ?> props);
}
