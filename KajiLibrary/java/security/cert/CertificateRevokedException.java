package java.security.cert;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// The certificate is revoked, and besides: when, why, who said so and with which extensions.
//
// ===============================================================================================
// WHY IT IS AN EXCEPTION WITH DATA AND NOT A MESSAGE
// ===============================================================================================
//
// The rest of the exceptions of this package say that something failed. This one says **what the
// validation found out**, and that has practical consequences: whoever catches it may want to tell
// a certificate revoked for a compromised key —which invalidates everything that key ever signed—
// apart from one revoked because the holder changed jobs, which invalidates nothing from before. A
// text message is of no use for that; the fields are.
//
// The invalidity date is the datum that is forgotten most and the one that changes the answer most.
// `getRevocationDate()` says when the CA **published** the revocation; `getInvalidityDate()` says
// since when the key is believed to have been compromised, which can be much earlier. A signature
// made between the two dates is suspicious even though at the time the CRL said nothing.
//
// The object is **immutable towards the outside**: the date is copied on the way in and on the way
// out, and the map of extensions is copied on the way in and returned immutable. Without that,
// whoever catches it could change the data of the exception before whoever rethrows it looks at
// them.
public class CertificateRevokedException extends CertificateException {

    private static final long serialVersionUID = 7839996631571608627L;

    private static final String OID_INVALIDITY_DATE = "2.5.29.24";

    private Date revocationDate;
    private final CRLReason reason;
    private final javax.security.auth.x500.X500Principal authority;
    private transient Map<String, Extension> extensions;

    // The four arguments are compulsory. There is none with a reasonable default: a revocation with
    // no date, no reason or no authority cannot be evaluated, and a null map would be confused with
    // "no extensions", which is different from "I do not know".
    public CertificateRevokedException(Date revocationDate, CRLReason reason,
            javax.security.auth.x500.X500Principal authority, Map<String, Extension> extensions) {
        if (revocationDate == null || reason == null || authority == null || extensions == null) {
            throw new NullPointerException();
        }
        this.revocationDate = new Date(revocationDate.getTime());
        this.reason = reason;
        this.authority = authority;
        this.extensions = new HashMap<String, Extension>(extensions);
    }

    // When the CA published the revocation. A copy.
    public Date getRevocationDate() {
        return new Date(this.revocationDate.getTime());
    }

    public CRLReason getRevocationReason() {
        return this.reason;
    }

    // Who published it. `X500Principal` is immutable, so the same instance is returned.
    public javax.security.auth.x500.X500Principal getAuthorityName() {
        return this.authority;
    }

    // Since when the certificate is believed to have stopped being trustworthy, or null if it was
    // not said.
    //
    // It comes from the InvalidityDate extension, which is a bare GeneralizedTime. If the extension
    // is not there —or if it is and cannot be read— null is returned: it is "it was not said",
    // which is the only honest thing that can be asserted. It is the JDK that decides to return
    // null instead of throwing, and it makes sense: an unreadable accessory datum should not cover
    // the main fact, which is that it is revoked.
    public Date getInvalidityDate() {
        Extension ext = this.getExtensions().get(OID_INVALIDITY_DATE);
        if (ext == null) {
            return null;
        }
        try {
            byte[] value = ext.getValue();
            if (value == null) {
                return null;
            }
            DerReader d = new DerReader(value, 0, value.length);
            // 0x18 is GeneralizedTime. The value of the extension comes **without** the OCTET
            // STRING from outside: that is what `Extension.getValue()` promises.
            int len = d.expect(0x18);
            int from = d.skip(len);
            return new Date(DerReader.generalizedTime(value, from, len));
        } catch (IOException e) {
            return null;
        }
    }

    // The extensions of the CRL entry, indexed by OID. Immutable.
    public Map<String, Extension> getExtensions() {
        return Collections.unmodifiableMap(this.extensions);
    }

    @Override
    public String getMessage() {
        return "Certificate has been revoked, reason: "
            + this.reason + ", revocation date: " + this.revocationDate
            + ", authority: " + this.authority + ", extension OIDs: "
            + this.extensions.keySet();
    }
}
