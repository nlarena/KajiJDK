package javax.imageio.spi;

import java.util.Locale;

/**
 * KajiLibrary's javax.imageio.spi.IIOServiceProvider -- the base of all image providers.
 *
 * <p>What all the provider kinds share: who made it, which version, and a localizable
 * description.
 *
 * <p>It implements {@link RegisterableService} with both methods empty, so a subclass that does not
 * need to be told anything does not have to write them.
 *
 * <p>The no-argument constructor exists for providers loaded by {@link java.util.ServiceLoader},
 * which requires a public one without parameters. It leaves both fields null, and the subclass has
 * to fill them in before anyone reads them.
 */
public abstract class IIOServiceProvider implements RegisterableService {

    /** Who made it. */
    protected String vendorName;

    /** Which version. */
    protected String version;

    /**
     * @throws IllegalArgumentException if either of the two is null
     */
    public IIOServiceProvider(String vendorName, String version) {
        if (vendorName == null) {
            throw new IllegalArgumentException("vendorName == null!");
        }
        if (version == null) {
            throw new IllegalArgumentException("version == null!");
        }
        this.vendorName = vendorName;
        this.version = version;
    }

    /** The one the service loader requires. See the class note. */
    public IIOServiceProvider() {
    }

    /** Does nothing; a subclass that needs to be told redefines it. */
    public void onRegistration(ServiceRegistry registry, Class<?> category) {
    }

    /** Does nothing. */
    public void onDeregistration(ServiceRegistry registry, Class<?> category) {
    }

    /** Who made it. */
    public String getVendorName() {
        return this.vendorName;
    }

    /** Which version. */
    public String getVersion() {
        return this.version;
    }

    /**
     * What this provider does, in words.
     *
     * @param locale in which locale, or null for the system's
     */
    public abstract String getDescription(Locale locale);
}
