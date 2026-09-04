package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;

/**
 * KajiLibrary's javax.naming.spi.ObjectFactory -- convierte lo que hay guardado en un objeto util.
 *
 * <p>Un directorio no guarda objetos Java: guarda una {@code Reference} --el nombre de una clase y
 * unos datos-- o algo del protocolo de abajo. Esta fabrica es la que convierte eso en el objeto que
 * la aplicacion espera recibir de un {@code lookup}.
 *
 * <p>Es lo que permite guardar en LDAP algo como una fuente de datos: lo que se guarda es la receta
 * --el driver, el URL, el usuario-- y lo que se recibe es la fuente ya armada.
 *
 * <p>Devolver <b>null</b> es lo normal y no un error: significa "esto no es lo mio", y la plataforma
 * le pregunta a la fabrica que sigue. Una fabrica que devuelve algo para todo rompe la cadena.
 */
public interface ObjectFactory {

    /**
     * El objeto que corresponde a esos datos.
     *
     * @param obj lo que estaba guardado
     * @param name su nombre relativo a {@code nameCtx}, o null
     * @param nameCtx contra que contexto es relativo el nombre; null es el inicial
     * @param environment el ambiente de la operacion
     * @return null si esta fabrica no reconoce esos datos; ver la nota de la clase
     */
    Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
        throws Exception;
}
