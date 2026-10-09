package java.security.cert;

// A list of revoked certificates.
//
// The class is minimal on purpose: the only thing every CRL knows how to do, whatever its format,
// is say whether a certificate is in it. Everything else —issuer, dates, extensions— is specific to
// X.509 and lives in `X509CRL`.
//
// The model of CRLs has a structural problem worth keeping in mind: they are a photograph with a
// date. Between one being issued and the next being issued, a revocation is not seen. That window
// is the reason OCSP exists, and the reason `PKIXRevocationChecker` lets one choose which is
// preferred.
public abstract class CRL {

    private final String type;

    protected CRL(String type) {
        this.type = type;
    }

    // The type: "X.509". `final` because the constructor fixes it, and a subclass that lied about
    // it would make the code that dispatches by type choose the wrong parser.
    public final String getType() {
        return this.type;
    }

    @Override
    public abstract String toString();

    // Whether this certificate appears as revoked in this list.
    public abstract boolean isRevoked(Certificate cert);
}
