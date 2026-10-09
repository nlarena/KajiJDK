package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.util.Collection;

// What a provider has to write in order to offer a store of certificates.
//
// The constructor receives the parameters and can reject them, which is different from the rest of
// the SPIs of the area: here there is no separate `engineInit` because a store with no source has
// no useful state to get to.
//
// Both methods return **collections, possibly empty, never null**, and that is of the contract: "I
// found nothing" is a normal result in a store, not an error.
public abstract class CertStoreSpi {

    public CertStoreSpi(CertStoreParameters params) throws InvalidAlgorithmParameterException {
    }

    public abstract Collection<? extends Certificate> engineGetCertificates(CertSelector selector)
        throws CertStoreException;

    public abstract Collection<? extends CRL> engineGetCRLs(CRLSelector selector)
        throws CertStoreException;
}
