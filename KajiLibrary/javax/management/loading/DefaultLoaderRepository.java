package javax.management.loading;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * KajiLibrary's javax.management.loading.DefaultLoaderRepository -- the global repository,
 * deprecated.
 *
 * <p>Static methods that look for a class in the repositories of <b>all</b> the virtual machine's
 * agents. It has been deprecated since JMX 1.2 (which shipped in Java SE 5) and the replacement is
 * {@code MBeanServer.getClassLoaderRepository()}, which returns <b>one</b> agent's repository.
 *
 * <p>The reason for the deprecation is what makes the class interesting: being static, it cannot
 * say which agent the class it returns comes from. With two agents in the same process --which is
 * exactly what JMX allows-- the result depends on the creation order, and an MBean may end up
 * loading the class of an agent that is not its own. That is not a detail: the two agents may exist
 * precisely to keep two versions of the same thing apart.
 *
 * <p>It is kept because there is compiled code that calls it.
 */
public class DefaultLoaderRepository {

    /** Public for compatibility; the class has no state and does not need instantiating. */
    public DefaultLoaderRepository() {
    }

    /**
     * Looks for the class in the repositories of all the agents.
     *
     * @throws ClassNotFoundException if none has it
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return search(className, null, false);
    }

    /**
     * The same, skipping that loader in each repository.
     *
     * @throws ClassNotFoundException if none of the others has it
     */
    public static Class<?> loadClassWithout(ClassLoader loader, String className)
        throws ClassNotFoundException {
        return search(className, loader, true);
    }

    /** The common walk of the two public methods. */
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
                    // This agent does not have it; carry on with the next.
                }
            }
            i = i + 1;
        }
        throw new ClassNotFoundException(className);
    }
}
