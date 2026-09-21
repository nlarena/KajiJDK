package java.security.cert;

import java.util.Set;

// What an X.509 object with extensions —a certificate, a CRL, a CRL entry— knows how to say about
// them.
//
// The distinction between critical and non-critical is **the** security rule of the extensions, and
// it is in the name of the methods: a critical extension whoever validates does not understand
// forces the whole object to be rejected. It is not a recommendation. It is what allows a CA to add
// a new restriction knowing that no old client is going to ignore it silently.
//
// `hasUnsupportedCriticalExtension()` exists precisely in order to ask that once and for all.
public interface X509Extension {

    // Whether there is any critical extension this implementation does not know how to process. If
    // it gives true, the object must **not** be used.
    boolean hasUnsupportedCriticalExtension();

    // The OIDs of the critical extensions, or null if there is none.
    Set<String> getCriticalExtensionOIDs();

    // The OIDs of the non-critical ones, or null if there is none.
    Set<String> getNonCriticalExtensionOIDs();

    // The DER value of an extension by its OID, or null if it is not there. They are the bytes of
    // the OCTET STRING that wraps the value, unwrapped: whoever asks knows already what they expect
    // inside.
    byte[] getExtensionValue(String oid);
}
