package javax.naming.ldap;

/**
 * Asks the server <strong>not</strong> to follow referrals: to return the referral object instead
 * of sending you to look for it elsewhere.
 *
 * <p>An LDAP directory may be spread across servers, and when one does not have what it is asked
 * for it returns a <em>referral</em>. By default the client follows it, which is what you want
 * almost always. This control turns that off.
 *
 * <p>What turning it off is for: <strong>managing the referral itself</strong>. Without this
 * control there is no way to delete or modify a referral object -- any operation on it is
 * redirected to the server it points to, which is exactly what you do not want.
 */
public final class ManageReferralControl extends BasicControl {

    private static final long serialVersionUID = 3017756160149982566L;

    /** The OID of this control. */
    public static final String OID = "2.16.840.1.113730.3.4.2";

    /** Critical: if the server does not understand it, the operation fails. */
    public ManageReferralControl() {
        super(OID, true, null);
    }

    /**
     * @param criticality whether the operation must fail when the server does not understand it.
     *     Setting it to {@code false} here is almost always a mistake: it would mean managing the
     *     referral if possible and following it if not, which are two completely different things
     */
    public ManageReferralControl(boolean criticality) {
        super(OID, criticality, null);
    }
}
