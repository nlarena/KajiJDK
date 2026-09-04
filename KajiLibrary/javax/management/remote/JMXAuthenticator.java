package javax.management.remote;

import javax.security.auth.Subject;

/**
 * KajiLibrary's javax.management.remote.JMXAuthenticator -- decide si un cliente entra.
 *
 * <p>Se le pasa al servidor en el mapa de entorno bajo la clave
 * {@link JMXConnectorServer#AUTHENTICATOR}, y se lo llama una vez por conexion.
 *
 * <p>El argumento es {@link Object} y no algo mas preciso porque depende del protocolo: el conector
 * RMI pasa un {@code String[]} de dos elementos --usuario y clave--, otro protocolo podria pasar un
 * certificado. Un autenticador tiene que comprobar el tipo antes de usarlo.
 *
 * <p>Devuelve el {@link Subject} con el que van a correr las operaciones de ese cliente. Se rechaza
 * lanzando {@link SecurityException}: devolver null significa "sin identidad", que no es lo mismo que
 * "no entra".
 */
public interface JMXAuthenticator {

    /**
     * @param credentials lo que mando el cliente; su tipo depende del protocolo
     * @return con que identidad corre, o null para ninguna
     * @throws SecurityException si no entra
     */
    Subject authenticate(Object credentials);
}
