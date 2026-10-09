package java.lang.management;

import java.util.List;
import java.util.Map;

/**
 * KajiLibrary's java.lang.management.RuntimeMXBean -- how this virtual machine was started.
 *
 * <p>The start-up snapshot: version, class paths, arguments, system properties, and how long it has
 * been running.
 *
 * <h2>{@link #getInputArguments} does not bring everything</h2>
 *
 * <p>It brings what was passed to the virtual machine, and explicitly <b>not</b> {@code main}'s
 * arguments. Nor what came in through {@code JAVA_TOOL_OPTIONS} or an options file, depending on the
 * virtual machine. This is not enough to reproduce a run.
 *
 * <h2>{@link #getName} promises nothing</h2>
 *
 * <p>The documentation says it can be any string at all. In practice the JDKs return
 * {@code pid@machine}, and there is code out there parsing it to get the pid. That is fragile, and
 * {@link #getPid} is there for it -- it arrived later for exactly that reason.
 *
 * <h2>{@link #getUptime} and {@link #getStartTime}</h2>
 *
 * <p>The first is a millisecond counter since start-up and does not depend on the wall clock; the
 * second does. Measuring intervals calls for the first: the system clock can jump.
 */
public interface RuntimeMXBean extends PlatformManagedObject {

    /**
     * The process identifier.
     *
     * <p>By default it takes it from {@code ProcessHandle.current()}, so it inherits whatever that
     * class can or cannot do on this platform.
     */
    default long getPid() {
        return ProcessHandle.current().pid();
    }

    /** This virtual machine's name. See the class's note: it promises no format. */
    String getName();

    /** The implementation's name. */
    String getVmName();

    /** Who made it. */
    String getVmVendor();

    /** Its version. */
    String getVmVersion();

    /** The name of the specification it meets. */
    String getSpecName();

    /** Who wrote it. */
    String getSpecVendor();

    /** Which version of the specification. */
    String getSpecVersion();

    /** Which version of the management specification this MBean meets. */
    String getManagementSpecVersion();

    /** The class path. */
    String getClassPath();

    /** The native library path. */
    String getLibraryPath();

    /** Whether this virtual machine supports the boot class path concept. */
    boolean isBootClassPathSupported();

    /**
     * The boot class path.
     *
     * @throws UnsupportedOperationException if this virtual machine does not support it
     */
    String getBootClassPath();

    /** The start-up arguments. See the class's note: {@code main}'s are not among them. */
    List<String> getInputArguments();

    /** Milliseconds running. See the class's note. */
    long getUptime();

    /** When it started, in milliseconds since the epoch. */
    long getStartTime();

    /** The system properties, as a map of strings. */
    Map<String, String> getSystemProperties();
}
