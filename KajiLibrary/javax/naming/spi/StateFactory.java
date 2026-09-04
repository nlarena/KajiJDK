package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.StateFactory -- convierte un objeto en algo que se pueda guardar.
 *
 * <p>El camino inverso de {@link ObjectFactory}: la aplicacion pasa un objeto a {@code bind} y esta
 * fabrica lo convierte en lo que el directorio sabe almacenar --tipicamente una
 * {@code Reference}--.
 *
 * <p>Que sean dos interfaces y no una con dos metodos es a proposito: guardar y recuperar los suele
 * hacer gente distinta. Quien publica un servicio escribe la de estado; quien lo consume necesita la
 * de objeto, que casi siempre viene con la biblioteca del servicio.
 *
 * <p>Devolver null significa "no es lo mio" y la plataforma sigue con la que viene, igual que del
 * otro lado.
 */
public interface StateFactory {

    /**
     * Lo que hay que guardar en lugar de ese objeto.
     *
     * @return null si esta fabrica no lo reconoce
     */
    Object getStateToBind(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
        throws NamingException;
}
