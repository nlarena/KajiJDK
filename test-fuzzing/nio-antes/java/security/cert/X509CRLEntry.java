package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

// An entry of an X.509 CRL: a revoked certificate, when, and —if it says so— why.
//
// The identity is the **serial number**, not the certificate: the CRL does not include the
// certificates it revokes, only their serials. That is why one also has to know who the issuer is,
// and that is why `getCertificateIssuer()` exists: in an indirect CRL —one that revokes
// certificates of several CAs— each entry can have an issuer different from that of the CRL.
//
public abstract class X509CRLEntry implements X509Extension {

    private static final String OID_CRL_REASON = "2.5.29.21";

    public X509CRLEntry() {
    }

    // The issuer of the certificate this entry revokes, or null if it is the same as that of the
    // CRL.
    //
    // It returns null and does not throw: it is a **concrete** method of the base class whose
    // contract is "the subclass that knows how to decode the `certificateIssuer` extension should
    // override it", and the JDK does exactly this same thing. Null does not mean "I do not know":
    // it means "the one of the CRL", which is the case of almost all of them. Only an indirect CRL
    // —one that revokes certificates of several CAs— carries that field, and there decoding it asks
    // for `GeneralName`, which this package does not have.
    public javax.security.auth.x500.X500Principal getCertificateIssuer() {
        return null;
    }

    // Two entries are the same if they encode the same bytes, just as in `Certificate`. Comparing
    // by serial number would not be enough: the same serial of two different issuers are two
    // different certificates.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof X509CRLEntry)) {
            return false;
        }
        try {
            byte[] a = this.getEncoded();
            byte[] b = ((X509CRLEntry) other).getEncoded();
            if (a.length != b.length) {
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
        } catch (CRLException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        try {
            byte[] a = this.getEncoded();
            int i = 0;
            while (i < a.length) {
                h = h * 31 + a[i];
                i = i + 1;
            }
        } catch (CRLException e) {
            return 0;
        }
        return h;
    }

    // The entry encoded in DER.
    public abstract byte[] getEncoded() throws CRLException;

    // The serial number of the revoked certificate.
    public abstract BigInteger getSerialNumber();

    // When it was revoked. With `KEY_COMPROMISE` this date is the one that decides whether an old
    // signature is still worth something, so it is not informative.
    public abstract Date getRevocationDate();

    public abstract boolean hasExtensions();

    @Override
    public abstract String toString();

    // The reason for the revocation, or null if the entry does not say.
    //
    // It is really decoded: the extension is an ENUMERATED and nothing else. Two behaviours that
    // look inconsistent and are the JDK's, replicated on purpose:
    //
    //   - a code outside the known list gives `UNSPECIFIED`, not an exception. It is right: a
    //     reason that is not understood does not change the fact that it **is revoked**, and
    //     failing there would turn a new CRL into an unreadable CRL.
    //   - a badly formed extension gives null, also without throwing. The consequence is that
    //     losing the reason never makes the revocation be lost, which is the safe side of the
    //     error.
    public CRLReason getRevocationReason() {
        if (!this.hasExtensions()) {
            return null;
        }
        byte[] ext = this.getExtensionValue(OID_CRL_REASON);
        if (ext == null) {
            return null;
        }
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_ENUMERATED);
            if (len < 1 || len > 4) {
                return null;
            }
            int from = d.skip(len);
            int code = 0;
            int i = 0;
            while (i < len) {
                code = (code << 8) | (value[from + i] & 0xff);
                i = i + 1;
            }
            CRLReason[] all = CRLReason.values();
            if (code < 0 || code >= all.length) {
                return CRLReason.UNSPECIFIED;
            }
            return all[code];
        } catch (IOException e) {
            return null;
        }
    }
}
