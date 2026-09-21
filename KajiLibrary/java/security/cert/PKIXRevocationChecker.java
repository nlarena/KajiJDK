package java.security.cert;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The configuration of how revocation is checked: OCSP, CRLs, and what to do when it cannot be
// found out.
//
// ===============================================================================================
// SOFT_FAIL IS THE OPTION THAT HAS TO BE UNDERSTOOD
// ===============================================================================================
//
// By default, if the revocation state cannot be found out —the OCSP responder does not answer, the
// CRL could not be downloaded— the validation **fails**. With `SOFT_FAIL` it does not fail: it goes
// on as if the certificate were not revoked, and the problem is noted in
// `getSoftFailExceptions()`.
//
// That turns the revocation check into something an attacker can turn off: whoever can block the
// traffic to the responder gets a stolen and already revoked certificate to pass. It is used all
// the same because the alternative —falling over every time an OCSP has a bad day— is worse for
// availability, but the choice has to be a conscious one. That the exceptions are kept and not lost
// is what allows one at least to find out.
//
// The order matters too: by default OCSP is tried first and CRL afterwards; `PREFER_CRLS` turns it
// round and `NO_FALLBACK` leaves only the first. With `NO_FALLBACK` plus `SOFT_FAIL`, a single
// point being down is enough for nothing to be checked.
//
// This class is abstract and checks nothing by itself: it keeps the configuration and leaves the
// only method that does work —`getSoftFailExceptions()`— unimplemented. This library brings no
// provider that implements it: there is no OCSP client and no downloading of CRLs, and neither of
// the two things can be written without `java.net` and without verification of signatures.
public abstract class PKIXRevocationChecker extends PKIXCertPathChecker {

    // The four knobs. See above: `SOFT_FAIL` is the only one that changes whether the validation
    // can fail or not.
    public enum Option {

        // Check only the end certificate, not the intermediate ones. Cheaper and weaker.
        ONLY_END_ENTITY,

        // Try CRLs before OCSP.
        PREFER_CRLS,

        // Do not try the second mechanism if the first did not work.
        NO_FALLBACK,

        // Do not fail when the state cannot be found out. See the note of the class.
        SOFT_FAIL
    }

    private URI ocspResponder;
    private X509Certificate ocspResponderCert;
    private List<Extension> ocspExtensions = Collections.<Extension>emptyList();
    private Map<X509Certificate, byte[]> ocspResponses = Collections.emptyMap();
    private Set<Option> options = Collections.emptySet();

    protected PKIXRevocationChecker() {
    }

    // Which OCSP responder to ask. Null means using the one the AIA extension of each certificate
    // says, which is the normal thing: fixing it here only makes sense with a responder of one's
    // own.
    public void setOcspResponder(URI uri) {
        this.ocspResponder = uri;
    }

    public URI getOcspResponder() {
        return this.ocspResponder;
    }

    // Which certificate to verify the signature of the OCSP answers with. Null lets the mechanism
    // of the RFC be used, where the issuer delegates to a responder. A **signed** OCSP answer is
    // the only thing that makes it trustworthy: without verifying the signature, the answer is
    // whatever the network says.
    public void setOcspResponderCert(X509Certificate cert) {
        this.ocspResponderCert = cert;
    }

    public X509Certificate getOcspResponderCert() {
        return this.ocspResponderCert;
    }

    // Extensions to send in the OCSP query. The one that matters is the nonce: it binds the answer
    // to this query and prevents an old one from being replayed —from when the certificate was not
    // yet revoked—.
    public void setOcspExtensions(List<Extension> extensions) {
        if (extensions == null) {
            this.ocspExtensions = Collections.<Extension>emptyList();
        } else {
            this.ocspExtensions = Collections.unmodifiableList(
                new ArrayList<Extension>(extensions));
        }
    }

    public List<Extension> getOcspExtensions() {
        return this.ocspExtensions;
    }

    // OCSP answers obtained already, so as not to query again. It is the mechanism of TLS
    // "stapling": the server attaches a recent answer and the client does not have to talk to
    // anybody else. They are still verified: coming from here does not make them trustworthy.
    public void setOcspResponses(Map<X509Certificate, byte[]> responses) {
        if (responses == null) {
            this.ocspResponses = Collections.emptyMap();
        } else {
            Map<X509Certificate, byte[]> copyOf = new HashMap<X509Certificate, byte[]>();
            Iterator<Map.Entry<X509Certificate, byte[]>> it = responses.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<X509Certificate, byte[]> e = it.next();
                byte[] v = e.getValue();
                byte[] c = new byte[v.length];
                System.arraycopy(v, 0, c, 0, v.length);
                copyOf.put(e.getKey(), c);
            }
            this.ocspResponses = copyOf;
        }
    }

    // A deep copy: the arrays are mutable and whoever receives the map cannot alter the state of
    // the checker.
    public Map<X509Certificate, byte[]> getOcspResponses() {
        Map<X509Certificate, byte[]> copyOf = new HashMap<X509Certificate, byte[]>();
        Iterator<Map.Entry<X509Certificate, byte[]>> it = this.ocspResponses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<X509Certificate, byte[]> e = it.next();
            byte[] v = e.getValue();
            byte[] c = new byte[v.length];
            System.arraycopy(v, 0, c, 0, v.length);
            copyOf.put(e.getKey(), c);
        }
        return copyOf;
    }

    public void setOptions(Set<Option> options) {
        if (options == null) {
            this.options = Collections.emptySet();
        } else {
            this.options = Collections.unmodifiableSet(EnumSet.copyOf(options));
        }
    }

    public Set<Option> getOptions() {
        return this.options;
    }

    // The problems `SOFT_FAIL` let through, in order. Empty if there were none —or if `SOFT_FAIL`
    // was not set, because there they would have made the validation fail—. **Reviewing it is not
    // optional if `SOFT_FAIL` was turned on**: it is the only place where there is a record that
    // nothing was checked.
    public abstract List<CertPathValidatorException> getSoftFailExceptions();

    // A shallow copy plus the containers. The return type is covariant so that whoever clones does
    // not have to cast.
    @Override
    public PKIXRevocationChecker clone() {
        PKIXRevocationChecker copyOf = (PKIXRevocationChecker) super.clone();
        copyOf.ocspExtensions = new ArrayList<Extension>(this.ocspExtensions);
        copyOf.ocspResponses = new HashMap<X509Certificate, byte[]>(this.ocspResponses);
        return copyOf;
    }
}
