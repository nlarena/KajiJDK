package javax.net.ssl;

/**
 * Whoever decides whether the <em>other</em> side's credentials are trustworthy.
 *
 * <p>The counterpart of {@link KeyManager}: one presents, the other judges. It is also a marker
 * interface, and for the same reason — see {@link X509TrustManager} for the form used in practice.
 */
public interface TrustManager {
}
