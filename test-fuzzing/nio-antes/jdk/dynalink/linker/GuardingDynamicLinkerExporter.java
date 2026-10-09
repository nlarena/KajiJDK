package jdk.dynalink.linker;

import java.util.List;
import java.util.function.Supplier;

/**
 * The door through which a third-party linker enters Dynalink without anyone naming it.
 *
 * <h2>How it works</h2>
 *
 * <p>It is a {@code ServiceLoader} service: whoever wants to contribute linkers extends this class,
 * declares it in their module with {@code provides}, and {@code DynamicLinkerFactory} finds it on
 * its own while building the chain. The Dynalink host need never hear about it.
 *
 * <p>It extends {@link Supplier} and returns not a linker but a <strong>list</strong>: a library
 * contributing support for several kinds of object publishes them all at once.
 *
 * <h2>The constant that stayed</h2>
 *
 * <p>{@link #AUTOLOAD_PERMISSION_NAME} named the permission that automatic loading required. It is
 * still public and {@code final} because it is API, even though the {@code SecurityManager} has been
 * disabled since JDK 24 and the check no longer happens.
 *
 * @since 9
 */
public abstract class GuardingDynamicLinkerExporter
        implements Supplier<List<GuardingDynamicLinker>> {

    /** Name of the {@code RuntimePermission} that guarded the automatic loading of linkers. */
    public static final String AUTOLOAD_PERMISSION_NAME = "dynalink.exportLinkersAutomatically";

    /** For the subclasses. */
    protected GuardingDynamicLinkerExporter() {
    }
}
