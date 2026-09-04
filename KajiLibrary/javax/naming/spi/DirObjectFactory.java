package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.directory.Attributes;

/**
 * KajiLibrary's javax.naming.spi.DirObjectFactory -- una {@link ObjectFactory} que ademas ve los
 * atributos.
 *
 * <p>Agrega una sobrecarga con {@link Attributes}, y no es comodidad: en un directorio, <b>lo que
 * distingue</b> a una entrada suele estar en sus atributos --su clase de objeto, sus campos-- y no en
 * lo que el {@code lookup} devuelve como valor. Sin los atributos, la fabrica no tiene con que
 * decidir si le corresponde.
 *
 * <p>Ademas ahorra una vuelta al servidor: la plataforma ya los leyo para resolver el nombre, y
 * pasarlos evita que la fabrica los pida de nuevo.
 *
 * <p>El metodo heredado de {@link ObjectFactory} sigue existiendo y se llama cuando no hay atributos
 * que pasar.
 */
public interface DirObjectFactory extends ObjectFactory {

    /**
     * El objeto que corresponde a esos datos y esos atributos.
     *
     * @param attrs los de la entrada, o null si no se leyeron
     * @return null si esta fabrica no los reconoce
     */
    Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment,
                             Attributes attrs) throws Exception;
}
