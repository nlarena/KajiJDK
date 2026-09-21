package javax.management.loading;

/**
 * KajiLibrary's javax.management.loading.PrivateClassLoader -- a loader that is not shared.
 *
 * <p>A marker interface, with no methods: a class loader that implements it and is registered as an
 * MBean does <b>not</b> go into the agent's {@link ClassLoaderRepository}.
 *
 * <p>It is an isolation mechanism and not a security one. What it prevents is two MBeans that bring
 * different versions of the same library resolving each other by accident when looking up a class
 * by name in the repository -- the problem that shows up just when it is too late, because the
 * first one registered wins over the other and the symptom is a {@code NoSuchMethodError} at run
 * time.
 */
public interface PrivateClassLoader {
}
