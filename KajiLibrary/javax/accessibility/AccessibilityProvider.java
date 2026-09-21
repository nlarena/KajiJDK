package javax.accessibility;

/**
 * An assistive-technology provider that plugs in as a service.
 *
 * <p>It is discovered with the platform's service mechanism, so an assistive technology is
 * installed by putting itself on the class path and not by touching the application.
 *
 * <p>The constructor is protected on purpose: the class is instantiated by the service loader, not
 * by whoever uses it.
 */
public abstract class AccessibilityProvider {

    /** For subclasses. */
    protected AccessibilityProvider() {
    }

    /** What this provider is called. */
    public abstract String getName();

    /** Puts it to work. */
    public abstract void activate();
}
