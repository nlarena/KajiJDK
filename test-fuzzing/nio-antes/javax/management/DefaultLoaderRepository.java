package javax.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Looks for a class among the class loaders registered as MBeans, in <b>all</b> the agents.
 *
 * <p>It has been deprecated since JMX 1.2 and it is worth understanding why, because the reason is
 * the design and not the age: "all the agents" is exactly the problem. Two applications sharing a
 * virtual machine, each with its own agent, end up seeing each other's loaders, and a class asked
 * for by one is resolved with the other's loader. The successor,
 * {@code javax.management.loading.ClassLoaderRepository}, is <b>per agent</b> precisely to close
 * that.
 *
 * @deprecated Use the per-agent repository. This class searches all of them.
 */
@Deprecated
public class DefaultLoaderRepository {

    public DefaultLoaderRepository() {
    }

    /**
     * @throws ClassNotFoundException if no loader knows it
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return find(className, null, false);
    }

    /**
     * The same, skipping one loader.
     *
     * <p>It exists to break the recursion: a loader that does not find a class consults the
     * repository, and if the repository asked it again, the cycle would not end.
     *
     * @param loader the one <b>not</b> consulted; {@code null} skips none
     */
    public static Class<?> loadClassWithout(ClassLoader loader, String className)
            throws ClassNotFoundException {
        return find(className, loader, true);
    }

    private static Class<?> find(String className, ClassLoader excluded, boolean excluding)
            throws ClassNotFoundException {
        for (ClassLoader cl : loaders()) {
            if (excluding && cl == excluded) {
                continue;
            }
            try {
                return Class.forName(className, false, cl);
            } catch (ClassNotFoundException e) {
                // Normal: the repository is a search, not a resolution. Carry on.
            }
        }
        throw new ClassNotFoundException(className);
    }

    /**
     * The visible loaders: the MBeans that <b>are</b> loaders, in each findable agent.
     *
     * <p>They are discovered through the public API --{@code queryNames} and then {@code
     * getClassLoader}-- and not through an internal table, because they also have to come out of an
     * {@link MBeanServer} written by someone else. At the end goes this class's loader, which is
     * the one that resolves everything on the class path: without it, a repository with no
     * registered loaders would find nothing.
     */
    private static List<ClassLoader> loaders() {
        List<ClassLoader> r = new ArrayList<ClassLoader>();
        for (MBeanServer s : MBeanServerFactory.findMBeanServer(null)) {
            Set<ObjectName> names;
            try {
                names = s.queryNames(null, null);
            } catch (Exception e) {
                continue;
            }
            for (ObjectName n : names) {
                try {
                    ClassLoader cl = s.getClassLoader(n);
                    if (cl != null && !r.contains(cl)) {
                        r.add(cl);
                    }
                } catch (Exception e) {
                    // That MBean is not a loader: it is the normal case, not an error.
                }
            }
        }
        ClassLoader own = DefaultLoaderRepository.class.getClassLoader();
        if (own != null && !r.contains(own)) {
            r.add(own);
        }
        return r;
    }
}
