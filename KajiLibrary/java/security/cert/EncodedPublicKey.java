package java.security.cert;

import java.io.IOException;
import java.security.PublicKey;

// A public key read from its `SubjectPublicKeyInfo`, with no cryptography provider.
//
//   SubjectPublicKeyInfo ::= SEQUENCE {
//       algorithm        AlgorithmIdentifier,
//       subjectPublicKey BIT STRING }
//
// ===============================================================================================
// WHAT IT PROMISES AND WHAT IT DOES NOT
// ===============================================================================================
//
// `PublicKey` promises exactly three things -- `getAlgorithm()`, `getFormat()` and `getEncoded()`
// -- and this class fulfils all three by reading them from the DER. It promises nothing more, and
// that is why it can be written without `KeyFactory`: **a public key is not a cryptographic
// operation**, it is a datum with the name of an algorithm. Whoever wants to verify a signature
// needs a `Signature`, which this library does not have, and for that no implementation of
// `PublicKey` would be enough for them.
//
// **Noted difference with the JDK**: there `X509CertSelector.getSubjectPublicKey()` returns a
// `sun.security.rsa.RSAPublicKeyImpl`, which also implements
// `java.security.interfaces.RSAPublicKey` and answers `getModulus()`. Here it returns this class,
// which does **not** implement that interface. A caller that casts to `RSAPublicKey` receives
// `ClassCastException` instead of the key. It was chosen this way because the alternative was
// worse: breaking out the modulus and the exponent to answer `getModulus()` would leave a key that
// looks usable for encrypting and is not.
//
// For what this package uses it for it is more than enough: `X509CertSelector.match` compares the
// encoding of the key of the certificate against that of the criterion, byte by byte.
final class EncodedPublicKey implements PublicKey {

    private static final long serialVersionUID = 4718264291549890431L;

    // The algorithms it knows how to name. An OID that is not in the table is not an error: the OID
    // is returned as the name, which is what the JDK does with an algorithm it does not know and is
    // more useful than a null.
    private static final String[][] NAMES = {
        {"1.2.840.113549.1.1.1", "RSA"},
        {"1.2.840.113549.1.1.10", "RSASSA-PSS"},
        {"1.2.840.10040.4.1", "DSA"},
        {"1.2.840.10046.2.1", "DiffieHellman"},
        {"1.2.840.113549.1.3.1", "DiffieHellman"},
        {"1.2.840.10045.2.1", "EC"},
        {"1.3.101.110", "XDH"},
        {"1.3.101.111", "XDH"},
        {"1.3.101.112", "Ed25519"},
        {"1.3.101.113", "Ed448"},
    };

    private final String algorithm;
    private final byte[] encoded;

    private EncodedPublicKey(String algorithm, byte[] encoded) {
        this.algorithm = algorithm;
        this.encoded = encoded;
    }

    /**
     * It reads the key from its `SubjectPublicKeyInfo`.
     *
     * <p>The whole structure is walked --not only as far as the OID-- so that a truncated DER is
     * rejected here and not later, when there would be no knowing where it came from.
     *
     * @throws IOException if the DER is not a well formed SubjectPublicKeyInfo
     */
    static EncodedPublicKey of(byte[] der) throws IOException {
        DerReader outer = new DerReader(der, 0, der.length);
        int len = outer.expect(DerReader.TAG_SEQUENCE);
        DerReader info = new DerReader(der, outer.position(), len);
        int algLen = info.expect(DerReader.TAG_SEQUENCE);
        DerReader alg = new DerReader(der, info.position(), algLen);
        info.skip(algLen);
        int oidLen = alg.expect(DerReader.TAG_OID);
        int oidAt = alg.skip(oidLen);
        String oid = alg.readOid(oidAt, oidLen);
        // The BIT STRING of the key: it is checked that it is there and that it closes, although it
        // is not looked at inside.
        int bitsLen = info.expect(0x03);
        info.skip(bitsLen);
        if (info.hasMore()) {
            throw new IOException("DER: extra data after the SubjectPublicKeyInfo");
        }
        byte[] copy = new byte[der.length];
        System.arraycopy(der, 0, copy, 0, der.length);
        return new EncodedPublicKey(nameOf(oid), copy);
    }

    private static String nameOf(String oid) {
        int i = 0;
        while (i < NAMES.length) {
            if (NAMES[i][0].equals(oid)) {
                return NAMES[i][1];
            }
            i = i + 1;
        }
        return oid;
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    /** Always {@code "X.509"}: it is the name of the SubjectPublicKeyInfo format. */
    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        byte[] c = new byte[this.encoded.length];
        System.arraycopy(this.encoded, 0, c, 0, this.encoded.length);
        return c;
    }

    @Override
    public String toString() {
        return this.algorithm + " public key, " + this.encoded.length + " encoded bytes";
    }
}
