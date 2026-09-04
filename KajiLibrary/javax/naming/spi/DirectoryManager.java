package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.CannotProceedException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

/**
 * KajiLibrary's javax.naming.spi.DirectoryManager -- lo mismo que {@link NamingManager}, con
 * atributos.
 *
 * <p>Extiende {@link NamingManager} y agrega las tres operaciones que necesitan ver los atributos de
 * una entrada. La herencia es de conveniencia --se hereda para tener los metodos estaticos a mano
 * bajo un solo nombre-- y no significa nada mas: todo lo de las dos clases es estatico.
 *
 * <p>Las sobrecargas con {@link Attributes} prefieren las fabricas que los entienden --
 * {@link DirObjectFactory} y {@link DirStateFactory}-- y caen en las comunes cuando no las hay. Ese
 * orden importa: una fabrica que ignora los atributos puede aceptar una entrada que no le
 * corresponde, y en un directorio los atributos son lo que la identifica.
 */
public class DirectoryManager extends NamingManager {

    /** Publico por compatibilidad; la clase es solo metodos estaticos. */
    DirectoryManager() {
    }

    /**
     * El contexto de directorio donde seguir una operacion que se corto.
     *
     * @throws NotContextException si lo que se resuelve no es un {@link DirContext}
     */
    public static DirContext getContinuationDirContext(CannotProceedException cpe)
        throws NamingException {
        Context ctx = getContinuationContext(cpe);
        if (ctx instanceof DirContext) {
            return (DirContext) ctx;
        }
        throw new NotContextException(
            "Not an instance of DirContext: " + ctx.getClass().getName());
    }

    /**
     * El objeto que corresponde a esos datos y esos atributos.
     *
     * <p>Ver el orden de preferencia en la nota de la clase.
     *
     * @return el objeto, o {@code refInfo} tal cual si ninguna fabrica lo reconocio
     */
    public static Object getObjectInstance(Object refInfo, Name name, Context nameCtx,
                                           Hashtable<?, ?> environment, Attributes attrs)
        throws Exception {
        String list = property(environment, Context.OBJECT_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                Object factory = instantiate(names[i].trim());
                if (factory instanceof DirObjectFactory) {
                    Object made = ((DirObjectFactory) factory)
                        .getObjectInstance(refInfo, name, nameCtx, environment, attrs);
                    if (made != null) {
                        return made;
                    }
                } else if (factory instanceof ObjectFactory) {
                    Object made = ((ObjectFactory) factory)
                        .getObjectInstance(refInfo, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
                i = i + 1;
            }
        }
        // Sin fabricas de directorio, vale el camino comun: incluye el de la propia referencia.
        return NamingManager.getObjectInstance(refInfo, name, nameCtx, environment);
    }

    /**
     * Lo que hay que guardar y con que atributos.
     *
     * @param inAttrs los que ya se pensaba escribir, o null
     * @return nunca null: si ninguna fabrica reconoce el objeto, se devuelve lo que entro
     */
    public static DirStateFactory.Result getStateToBind(Object obj, Name name, Context nameCtx,
                                                        Hashtable<?, ?> environment,
                                                        Attributes inAttrs)
        throws NamingException {
        String list = property(environment, Context.STATE_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                Object factory = instantiate(names[i].trim());
                if (factory instanceof DirStateFactory) {
                    DirStateFactory.Result made = ((DirStateFactory) factory)
                        .getStateToBind(obj, name, nameCtx, environment, inAttrs);
                    if (made != null) {
                        return made;
                    }
                } else if (factory instanceof StateFactory) {
                    Object made = ((StateFactory) factory)
                        .getStateToBind(obj, name, nameCtx, environment);
                    if (made != null) {
                        return new DirStateFactory.Result(made, inAttrs);
                    }
                }
                i = i + 1;
            }
        }
        return new DirStateFactory.Result(obj, inAttrs);
    }

    /**
     * Instancia esa clase, o null.
     *
     * <p>Duplica el helper privado de {@link NamingManager} porque alla es privado, y el JDK tampoco
     * lo comparte. Se traga la falla por lo mismo: una fabrica que no carga es una fabrica menos.
     */
    private static Object instantiate(String className) {
        try {
            Class<?> found = Class.forName(className, true, contextLoader());
            return found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
