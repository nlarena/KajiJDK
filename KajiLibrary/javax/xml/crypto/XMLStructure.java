package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.XMLStructure -- any piece of an XML cryptographic structure.
 *
 * <p>The root interface of the package, and almost empty on purpose: a single method, which asks
 * whether a feature is supported. Everything else --that it is a signature, a reference, a key-- is
 * said by the subinterfaces.
 *
 * <p>It exists because XML-DSig structures nest in ways that cannot be typed beforehand: the
 * content of an {@code Object} or of a {@code KeyInfo} is "whatever the document brings", and that
 * needs a common type.
 *
 * <p>{@link #isFeatureSupported} receives a feature name --like those of an {@code XMLReader}-- and
 * returns false for the ones it does not know. There is no standard list; each implementation
 * defines its own.
 */
public interface XMLStructure {

    /**
     * Whether this implementation supports that feature.
     *
     * @throws NullPointerException if the name is null
     */
    boolean isFeatureSupported(String feature);
}
