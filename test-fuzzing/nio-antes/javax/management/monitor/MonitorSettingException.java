package javax.management.monitor;

import javax.management.JMRuntimeException;

/**
 * KajiLibrary's javax.management.monitor.MonitorSettingException -- the monitor is misconfigured.
 *
 * <p>It is a {@link JMRuntimeException}, that is, <b>unchecked</b>, and that is worth explaining:
 * the monitor's thread throws it while observing, not whoever configured it. By the time it fires,
 * whoever got the configuration wrong is long gone, so forcing them to catch it would have served
 * no purpose.
 *
 * <p>In practice it is hardly ever seen: most configuration errors are caught by the setter itself
 * with an {@code IllegalArgumentException} on the spot. This one is left for what can only be known
 * by looking at the real attribute --a threshold of a type that does not compare with the observed
 * value-- and that only happens once the monitor is running.
 */
public class MonitorSettingException extends JMRuntimeException {

    private static final long serialVersionUID = -8807913418190202007L;

    /** Without detail. */
    public MonitorSettingException() {
        super();
    }

    /** With a message saying which setting is wrong. */
    public MonitorSettingException(String message) {
        super(message);
    }
}
