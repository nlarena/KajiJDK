package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;

// A store to take certificates and CRLs from.
//
// It is where the path builder takes the links it is missing from: the chain that arrives in a
// handshake is usually incomplete, and the intermediate certificates have to be fetched from
// somewhere.
//
// There is a difference of contract with `KeyStore` worth marking and that its similar name hides:
// **a `CertStore` does not imply trust**. It is a source of material, not a list of anchors. A
// certificate that came out of here still has to be validated against a `TrustAnchor`; treating the
// contents of a store as trustworthy would give anybody who can write into it the ability to put in
// a root.
//
// The methods are thread-safe by contract —several threads can query the same store at a time— and
// return collections **possibly empty, never null**: "I found nothing" is normal.
//
// A KajiLibrary subset: there is no registered `CertStore` provider, so the three overloads of
// `getInstance` always throw `NoSuchAlgorithmException`. The two standard types —"Collection" and
// "LDAP"— are not there: the first is easy but would need the complete selectors in order to
// filter, and the second asks for a network connection. The structure is left ready.
public class CertStore {

    private final CertStoreSpi storeSpi;
    private final Provider provider;
    private final String type;
    private final CertStoreParameters params;

    protected CertStore(CertStoreSpi storeSpi, Provider provider, String type,
                        CertStoreParameters params) {
        this.storeSpi = storeSpi;
        this.provider = provider;
        this.type = type;
        // It is copied because the parameters are mutable: without this, changing them after
        // creating the store would change where it reads from.
        this.params = (params == null ? null : (CertStoreParameters) params.clone());
    }

    // The certificates that meet the criterion. An empty collection if there are none.
    public final Collection<? extends Certificate> getCertificates(CertSelector selector)
            throws CertStoreException {
        return this.storeSpi.engineGetCertificates(selector);
    }

    // The CRLs that meet the criterion. An empty collection if there are none.
    public final Collection<? extends CRL> getCRLs(CRLSelector selector)
            throws CertStoreException {
        return this.storeSpi.engineGetCRLs(selector);
    }

    public static CertStore getInstance(String type, CertStoreParameters params)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertStore", type);
            if (s != null) {
                return build(s, type, params);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(type + " CertStore not available");
    }

    public static CertStore getInstance(String type, CertStoreParameters params, String provider)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
                   NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, params, p);
    }

    public static CertStore getInstance(String type, CertStoreParameters params, Provider provider)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider.Service s = provider.getService("CertStore", type);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such type: " + type + " for provider " + provider.getName());
        }
        return build(s, type, params);
    }

    private static CertStore build(Provider.Service s, String type, CertStoreParameters params)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(params);
        if (!(o instanceof CertStoreSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for CertStore is not a CertStoreSpi: " + s.getClassName());
        }
        return new CertStore((CertStoreSpi) o, s.getProvider(), type, params);
    }

    // A copy of the parameters it was created with, or null if there were none.
    public final CertStoreParameters getCertStoreParameters() {
        return (this.params == null ? null : (CertStoreParameters) this.params.clone());
    }

    public final String getType() {
        return this.type;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    // The default type, from the security property `certstore.type`. "LDAP" if it is not set, which
    // is a fossil default: nobody publishes certificates in LDAP today.
    public static final String getDefaultType() {
        String t = Security.getProperty("certstore.type");
        if (t == null) {
            return "LDAP";
        }
        return t;
    }
}
