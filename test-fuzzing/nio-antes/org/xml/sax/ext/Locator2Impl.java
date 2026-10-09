package org.xml.sax.ext;

import org.xml.sax.Locator;

/**
 * KajiLibrary's org.xml.sax.ext.Locator2Impl -- a {@link Locator2} that can be kept, and the one a
 * parser uses to hand over the position.
 *
 * <p>It serves for the usual two things, just like `LocatorImpl`: **freezing** a position --the
 * `Locator` the parser lends is alive and asking it after the event answers for somewhere else--
 * and **building** one when one generates the events oneself.
 *
 * <p>The detail that does not show in the signature is in the copy constructor: if the `Locator`
 * passed to it is **not** a `Locator2`, the version and the encoding are left at `null` instead of
 * inventing a default `"1.0"`. It is the only honest answer --it is not known which it was--. The
 * note added that this makes the case distinguishable from a `Locator2` that returned `null`
 * because the parser did not know either; it does not: both come out as `null`.
 */
public class Locator2Impl extends org.xml.sax.helpers.LocatorImpl implements Locator2 {

    private String encoding;
    private String version;

    /** Everything with no value, ready for the fields to be set. */
    public Locator2Impl() {
    }

    /**
     * The snapshot described above. It always copies the four fields of the `Locator`, and the two
     * of `Locator2` only when the object has them.
     */
    public Locator2Impl(Locator locator) {
        super(locator);
        if (locator instanceof Locator2) {
            Locator2 l2 = (Locator2) locator;
            version = l2.getXMLVersion();
            encoding = l2.getEncoding();
        }
    }

    public String getXMLVersion() {
        return version;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setXMLVersion(String version) {
        this.version = version;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
