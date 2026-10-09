package java.lang.management;

import javax.management.ObjectName;

/**
 * KajiLibrary's java.lang.management.PlatformManagedObject -- what can be published as a platform
 * MBean.
 *
 * <p>A single method, and the interface exists for what it <b>allows</b>: since every platform MXBean
 * extends it, {@code ManagementFactory.getPlatformMXBean(Class)} can ask for any of them with one
 * generic signature.
 *
 * <p>{@link #getObjectName} returns the name that object appears under in the platform MBean server
 * -- {@code java.lang:type=Memory}, for instance. It is the bridge between this package's typed API
 * and {@code javax.management}'s by-name API.
 */
public interface PlatformManagedObject {

    /** Under what name it appears in the platform MBean server. */
    ObjectName getObjectName();
}
