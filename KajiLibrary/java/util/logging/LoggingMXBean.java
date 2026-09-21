package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LoggingMXBean -- looking at and moving the levels from outside.
 *
 * <p>It is the management view of the logger tree: which loggers there are, what level each is at,
 * who is whose parent, and --the only one that writes-- changing one's level. It serves for raising
 * the logging detail of a service that is already running without restarting it, which is when it is
 * most needed and when the configuration file can least be touched.
 *
 * <p><strong>Why this class CAN be brought in, even though it names JMX.</strong> Its javadoc in the
 * JDK speaks of `ManagementFactory` and of `PlatformLoggingMXBean`, and that is where the idea comes
 * from that it depends on `java.lang.management` --which does not exist in this tree. But that is
 * **how it is published**, not **what it declares**: the interface is four methods over `String` and
 * `List<String>` and not one of them mentions a `java.lang.management` type. Registering it in an
 * MBean server is another matter, and that other matter is what is missing; these four methods'
 * contract is honoured in full against the {@link LogManager} that is already here. There is nothing
 * to simulate, so it is brought in.
 *
 * <p><strong>The three return values not to be confused</strong>, because they are three different
 * states and two of them look alike:
 *
 * <ul>
 * <li><b>`null`</b> -- there **is no** logger by that name. It is the answer to a badly asked
 *     question, and that is why it is told from the other two.
 * <li><b>`""`</b> -- the logger exists and **has no level of its own**: it inherits its parent's.
 *     Empty and not `null` precisely so that `null` can be reserved for the case above.
 * <li><b>the level's name</b> -- the logger has a level of its own.
 * </ul>
 *
 * <p>The same in {@link #getParentLoggerName}: `""` is the **root** --it exists and has no parent--
 * and `null` is "there is no such logger". A single value for both cases would make it impossible to
 * tell a misspelled name from the root, which is the mistake one makes when writing the tool that
 * consumes this.
 *
 * <p>It has been deprecated since 9 --what replaces it is
 * `java.lang.management.PlatformLoggingMXBean`, which lives on the other side of the border-- but it
 * is **not** marked for removal: JDK 25 annotates it `forRemoval=false`, and that is why the
 * annotation here says the same. Putting `forRemoval=true` would be warning of a removal the
 * reference does not announce, and a warning too many is as much a lie as one too few.
 */
@Deprecated(since = "9")
public interface LoggingMXBean {

    /**
     * The names of every registered logger.
     *
     * <p>A **snapshot**, not a live view: whoever walks it does not find out about the loggers
     * created while they walk it, and that is what suits -- creating a logger is something any class
     * does on being loaded, and a list that changed by itself under the iterator would turn a listing
     * into a race.
     */
    java.util.List<String> getLoggerNames();

    /**
     * The name of that logger's own level.
     *
     * @return the level's name, `""` if the logger inherits its level, or `null` if it does not
     *     exist
     */
    String getLoggerLevel(String loggerName);

    /**
     * It sets the level on a logger that already exists.
     *
     * <p>A `null` `levelName` is **not an error**: it is the way of taking its own level away and
     * making it inherit from its parent again. It is the inverse of setting one, and without it one
     * could lower a service's detail live but not put it back as it was.
     *
     * <p>It does not create the logger if it does not exist: moving the level of something that is
     * not there is doing nothing, and creating it here would leave an ownerless logger nobody
     * writes.
     *
     * @throws IllegalArgumentException if there is no logger by that name, or if `levelName` is not
     *         a known level
     */
    void setLoggerLevel(String loggerName, String levelName);

    /**
     * The parent's name in the tree.
     *
     * @return the parent's name, `""` if it is the root --which has none-- or `null` if it does not
     *     exist
     */
    String getParentLoggerName(String loggerName);
}
