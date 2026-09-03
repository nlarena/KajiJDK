package javax.management.loading;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * KajiLibrary's javax.management.loading.DefaultLoaderRepository -- el repositorio global, obsoleto.
 *
 * <p>Metodos estaticos que buscan una clase en los repositorios de <b>todos</b> los agentes de la
 * maquina virtual. Esta obsoleta desde 1.5 y el reemplazo es
 * {@code MBeanServer.getClassLoaderRepository()}, que devuelve el repositorio de <b>un</b> agente.
 *
 * <p>El motivo de la obsolescencia es el que hace interesante a la clase: al ser estatica, no puede
 * decir de que agente sale la clase que devuelve. Con dos agentes en el mismo proceso --que es
 * justamente lo que JMX permite-- el resultado depende del orden de creacion, y un MBean puede
 * terminar cargando la clase de un agente que no es el suyo. Eso no es un detalle: los dos agentes
 * pueden existir precisamente para mantener separadas dos versiones de lo mismo.
 *
 * <p>Se conserva porque hay codigo compilado que la llama.
 */
public class DefaultLoaderRepository {

    /** Publico por compatibilidad; la clase no tiene estado y no hace falta instanciarla. */
    public DefaultLoaderRepository() {
    }

    /**
     * Busca la clase en los repositorios de todos los agentes.
     *
     * @throws ClassNotFoundException si ninguno la tiene
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return search(className, null, false);
    }

    /**
     * Igual, salteando ese cargador en cada repositorio.
     *
     * @throws ClassNotFoundException si ninguno de los demas la tiene
     */
    public static Class<?> loadClassWithout(ClassLoader loader, String className)
        throws ClassNotFoundException {
        return search(className, loader, true);
    }

    /** El recorrido comun de los dos metodos publicos. */
    private static Class<?> search(String className, ClassLoader exclude, boolean skip)
        throws ClassNotFoundException {
        List<MBeanServer> servers = new ArrayList<MBeanServer>(
            MBeanServerFactory.findMBeanServer(null));
        int i = 0;
        while (i < servers.size()) {
            ClassLoaderRepository repository = servers.get(i).getClassLoaderRepository();
            if (repository != null) {
                try {
                    if (skip) {
                        return repository.loadClassWithout(exclude, className);
                    }
                    return repository.loadClass(className);
                } catch (ClassNotFoundException e) {
                    // Este agente no la tiene; se sigue con el que viene.
                }
            }
            i = i + 1;
        }
        throw new ClassNotFoundException(className);
    }
}
