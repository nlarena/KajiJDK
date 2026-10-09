package javax.management.loading;

/**
 * KajiLibrary's javax.management.loading.ClassLoaderRepository -- the loaders an agent knows.
 *
 * <p>A JMX agent has to be able to load classes that are not on its own class path: a class name
 * arrives over the network, from a remote client, and the MBean that implements it may have been
 * brought in by any of the registered loaders. This repository is that list, and it is consulted in
 * registration order.
 *
 * <h2>Why there are three methods and not one</h2>
 *
 * <p>The two that take a loader exist to <b>cut recursions</b>, not for convenience. A loader that
 * is in the repository and that, on a failure, asks the repository would call itself forever. With
 * {@link #loadClassWithout} it excludes itself, and with {@link #loadClassBefore} it also excludes
 * everyone that comes after it.
 *
 * <p>The difference between the two matters: {@code loadClassWithout} keeps consulting the
 * <b>later</b> ones, so two loaders that ask each other can still hang. {@code loadClassBefore}
 * cannot, because every call looks at a strictly shorter prefix of the list. That is why it is the
 * one to use when the caller is part of the repository.
 */
public interface ClassLoaderRepository {

    /**
     * Looks for the class in all the loaders, in registration order.
     *
     * @throws ClassNotFoundException if none has it
     */
    Class<?> loadClass(String className) throws ClassNotFoundException;

    /**
     * The same, skipping that loader.
     *
     * @param exclude the one not consulted; see the class note
     * @throws ClassNotFoundException if none of the others has it
     */
    Class<?> loadClassWithout(ClassLoader exclude, String className) throws ClassNotFoundException;

    /**
     * The same, but only with the ones <b>before</b> that one.
     *
     * <p>The search stops when reaching {@code stop}, which is not consulted.
     *
     * @throws ClassNotFoundException if none of the earlier ones has it
     */
    Class<?> loadClassBefore(ClassLoader stop, String className) throws ClassNotFoundException;
}
