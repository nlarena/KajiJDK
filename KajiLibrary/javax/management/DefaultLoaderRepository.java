package javax.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Busca una clase entre los cargadores registrados como MBeans, en <b>todos</b> los agentes.
 *
 * <p>Esta obsoleta desde 1.2 y conviene entender por que, porque el motivo es el diseno y no la
 * edad: "todos los agentes" es exactamente el problema. Dos aplicaciones que comparten maquina
 * virtual y cada una con su agente terminan viendo los cargadores de la otra, y una clase que se
 * pidio para una se resuelve con el cargador de la otra. La sucesora,
 * {@code javax.management.loading.ClassLoaderRepository}, es <b>por agente</b> justamente para
 * cerrar eso.
 *
 * @deprecated Usar el repositorio por agente. Esta clase busca en todos.
 */
@Deprecated
public class DefaultLoaderRepository {

    public DefaultLoaderRepository() {
    }

    /**
     * @throws ClassNotFoundException si ningun cargador la conoce
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return buscar(className, null, false);
    }

    /**
     * Igual, salteando un cargador.
     *
     * <p>Existe para romper la recursion: un cargador que no encuentra una clase consulta al
     * repositorio, y si el repositorio le volviera a preguntar a el, el ciclo no termina.
     *
     * @param loader el que <b>no</b> se consulta; `null` no saltea a ninguno
     */
    public static Class<?> loadClassWithout(ClassLoader loader, String className)
            throws ClassNotFoundException {
        return buscar(className, loader, true);
    }

    private static Class<?> buscar(String className, ClassLoader excluido, boolean excluyendo)
            throws ClassNotFoundException {
        for (ClassLoader cl : cargadores()) {
            if (excluyendo && cl == excluido) {
                continue;
            }
            try {
                return Class.forName(className, false, cl);
            } catch (ClassNotFoundException e) {
                // Normal: el repositorio es una busqueda, no una resolucion. Se sigue.
            }
        }
        throw new ClassNotFoundException(className);
    }

    /**
     * Los cargadores visibles: los MBeans que <b>son</b> cargadores, en cada agente encontrable.
     *
     * <p>Se descubren por la API publica --`queryNames` y despues `getClassLoader`-- y no por una
     * tabla interna, porque tienen que salir tambien de un {@link MBeanServer} escrito por otro.
     * Al final va el cargador de esta clase, que es el que resuelve todo lo que este en el
     * classpath: sin el, un repositorio sin cargadores registrados no encontraria nada.
     */
    private static List<ClassLoader> cargadores() {
        List<ClassLoader> r = new ArrayList<ClassLoader>();
        for (MBeanServer s : MBeanServerFactory.findMBeanServer(null)) {
            Set<ObjectName> nombres;
            try {
                nombres = s.queryNames(null, null);
            } catch (Exception e) {
                continue;
            }
            for (ObjectName n : nombres) {
                try {
                    ClassLoader cl = s.getClassLoader(n);
                    if (cl != null && !r.contains(cl)) {
                        r.add(cl);
                    }
                } catch (Exception e) {
                    // Ese MBean no es un cargador: es el caso normal, no un error.
                }
            }
        }
        ClassLoader propio = DefaultLoaderRepository.class.getClassLoader();
        if (propio != null && !r.contains(propio)) {
            r.add(propio);
        }
        return r;
    }
}
