package javax.security.sasl;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

/**
 * KajiLibrary's javax.security.sasl.SaslServerFactory -- de donde salen los {@link SaslServer}.
 *
 * <p>El espejo de {@link SaslClientFactory}, con una diferencia en la firma que dice mucho: aca se
 * pide <b>un</b> mecanismo y no una lista. Tiene sentido -- el cliente ya eligio y lo anuncio, y el
 * servidor solo tiene que poder atenderlo o no.
 *
 * <p>{@link #getMechanismNames} tambien depende de la politica, igual que del lado del cliente, y del
 * lado del servidor eso es todavia mas importante: la lista que devuelve es la que se le <b>anuncia
 * al cliente</b>, y anunciar un mecanismo debil es ofrecerselo a quien quiera elegirlo.
 */
public interface SaslServerFactory {

    /**
     * Un servidor para ese mecanismo.
     *
     * @return null si esta fabrica no puede con el
     */
    SaslServer createSaslServer(String mechanism, String protocol, String serverName,
                                Map<String, ?> props, CallbackHandler cbh) throws SaslException;

    /** Los mecanismos que ofrece con esas propiedades. Ver la nota de la clase. */
    String[] getMechanismNames(Map<String, ?> props);
}
