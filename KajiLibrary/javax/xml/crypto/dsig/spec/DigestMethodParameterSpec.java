package javax.xml.crypto.dsig.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.DigestMethodParameterSpec -- the parameters of a digest.
 *
 * <p>A marker, and <b>none</b> of the standard digest algorithms takes parameters: SHA-256 is not
 * configured. It exists all the same because the API has to be able to receive them from an
 * algorithm that does take them, and because without it {@code newDigestMethod} would accept the
 * parameters of anything else.
 */
public interface DigestMethodParameterSpec extends AlgorithmParameterSpec {
}
