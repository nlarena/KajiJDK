package java.security.cert;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;

// A certificate: a public key tied to an identity by the signature of a third party.
//
// ===============================================================================================
// WHY THIS CLASS CAN BE HONEST WITHOUT KNOWING CRYPTOGRAPHY
// ===============================================================================================
//
// Everything that decides whether a certificate is to be trusted —`verify`— is **abstract**. This
// class does not implement it and could not: it does not know what format the certificate is in or
// which algorithm it is signed with. What it does define is the structural part: the type, the
// equality by encoding, and the contract that `verify` throws if the signature does not validate.
//
// That contract deserves reading twice because it is the other way round from
// `Signature.verify`'s: here there is **no return value**. A verification that goes well returns
// without saying anything, and one that goes badly throws. Whoever writes
// `try { c.verify(k); } catch (Exception e) {}` is not handling the error: they are accepting any
// certificate.
//
// ===============================================================================================
// A KajiLibrary subset
// ===============================================================================================
//
// **There is no subclass.** `X509Certificate` and the factories (`CertificateFactory`) are not
// there: parsing an X.509 is reading ASN.1/DER and verifying its signature is RSA or ECDSA, and
// none of that is implemented in this library. This class exists because it is the type
// `CodeSource`, `CodeSigner`, `CertPath` and `UnresolvedPermission` name, and because its
// structural part can be written whole without lying.
//
// The equality is defined **by the encoding**, not by identity or by fields: two different objects
// that encode the same bytes are the same certificate. It is the only right thing —the certificate
// is its bytes— and it is what makes comparing chains of certificates work between different
// implementations.
public abstract class Certificate implements Serializable {

    private final String type;

    protected Certificate(String type) {
        this.type = type;
    }

    // The type: "X.509".
    public final String getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Certificate)) {
            return false;
        }
        try {
            byte[] a = this.getEncoded();
            byte[] b = ((Certificate) other).getEncoded();
            if (a == null || b == null || a.length != b.length) {
                return false;
            }
            int i = 0;
            while (i < a.length) {
                if (a[i] != b[i]) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        } catch (CertificateEncodingException e) {
            // A certificate that cannot be encoded cannot be compared. Saying "different" is safer
            // than saying "equal": the worst that happens is that something valid is rejected.
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            byte[] a = this.getEncoded();
            int h = 0;
            int i = 0;
            while (i < a.length) {
                h = h * 31 + a[i];
                i = i + 1;
            }
            return h;
        } catch (CertificateEncodingException e) {
            return 0;
        }
    }

    // The encoded form of the certificate.
    public abstract byte[] getEncoded() throws CertificateEncodingException;

    // It verifies the signature of the certificate with `key`. **It returns nothing: if it does not
    // throw, it is valid.**
    public abstract void verify(PublicKey key)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    public abstract void verify(PublicKey key, String sigProvider)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    // The variant that receives an already resolved `Provider`.
    //
    // It throws `UnsupportedOperationException` and **it is like that in the JDK**: it was added in
    // Java 8 with a base implementation that does nothing, so as not to break the subclasses that
    // already existed. One that wants to support it overrides it. Copying the behaviour is the
    // right thing — inventing a verification here would be exactly the hole.
    public void verify(PublicKey key, Provider sigProvider)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
                   SignatureException {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract String toString();

    // The public key this certificate certifies.
    public abstract PublicKey getPublicKey();

    // It serialises the certificate by its type and its encoding, not by its fields.
    //
    // A KajiLibrary subset: in the JDK it returns a `CertificateRep`, an internal class that on
    // deserialising rebuilds the certificate with a `CertificateFactory`. Here there are no
    // factories, so there is no way back: it throws instead of writing something that cannot be
    // read afterwards.
    protected Object writeReplace() throws ObjectStreamException {
        throw new java.io.NotSerializableException(
            "java.security.cert.Certificate: no CertificateFactory available to restore it");
    }
}
