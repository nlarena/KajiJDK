package javax.transaction.xa;

/**
 * KajiLibrary's javax.transaction.xa.Xid -- the identifier of a distributed transaction.
 *
 * <p>It has **three** parts and not a number, and there is the whole idea: a global identifier that
 * is the same in every system that takes part, a branch qualifier that tells each one's part apart,
 * and a format that says who invented the identifier. A loose integer would not serve -- two
 * different transaction managers would choose the same one and there would be no way of noticing.
 */
public interface Xid {

    /** The maximum number of bytes of the global identifier. */
    int MAXGTRIDSIZE = 64;

    /** The maximum number of bytes of the branch qualifier. */
    int MAXBQUALSIZE = 64;

    /** Who defined the format of this identifier. */
    int getFormatId();

    /** The global identifier, common to every branch. */
    byte[] getGlobalTransactionId();

    /** Which branch of that transaction this is. */
    byte[] getBranchQualifier();
}
