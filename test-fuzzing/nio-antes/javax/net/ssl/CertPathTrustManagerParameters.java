package javax.net.ssl;

import java.security.cert.CertPathParameters;

/**
 * Wraps some {@link CertPathParameters} to hand them to a {@link TrustManagerFactory}.
 *
 * <p>It is an adapter and nothing more, and that modesty is the point: validating a certificate
 * chain is already specified in {@code java.security.cert}, with its revocations, its anchors and
 * its policies. This class repeats none of that — it only makes that configuration come in where
 * {@link TrustManagerFactory#init(ManagerFactoryParameters)} expects it.
 */
public class CertPathTrustManagerParameters implements ManagerFactoryParameters {

    private final CertPathParameters parameters;

    /**
     * A <strong>copy</strong> is kept, not the reference: {@link CertPathParameters} is mutable,
     * and a trust policy somebody can change after having handed it over is not a policy.
     *
     * @throws NullPointerException if {@code parameters} is {@code null}
     */
    public CertPathTrustManagerParameters(CertPathParameters parameters) {
        this.parameters = (CertPathParameters) parameters.clone();
    }

    /** A copy of the parameters, for the same reason. */
    public CertPathParameters getParameters() {
        return (CertPathParameters) this.parameters.clone();
    }
}
