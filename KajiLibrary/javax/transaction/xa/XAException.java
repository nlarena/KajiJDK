package javax.transaction.xa;

/**
 * KajiLibrary's javax.transaction.xa.XAException -- something went wrong in a distributed
 * transaction.
 *
 * <p>The code goes in a **public field** and not in a getter, something that would not be done
 * today; it stays that way because the signature is the contract and the code that exists reads it
 * as a field.
 *
 * <p>The codes are read by families, and they are worth seeing: the `XA_RB*` say "it was rolled
 * back, and for this"; the `XA_HEUR*` say something worse --that the resource **decided on its own**
 * without waiting for the coordinator, which is the failure that breaks atomicity and always ends in
 * human intervention--; and the `XAER_*`, negative, are errors of the protocol or of the manager,
 * not of the transaction.
 */
public class XAException extends Exception {

    /** The code of the error. */
    public int errorCode;

    // ---- the transaction was rolled back ------------------------------------------------------------

    /** The first of the rollback codes. */
    public static final int XA_RBBASE = 100;

    /** It was rolled back with no more specific reason. */
    public static final int XA_RBROLLBACK = XA_RBBASE;

    /** It was rolled back because of a communication failure. */
    public static final int XA_RBCOMMFAIL = XA_RBBASE + 1;

    /** It was rolled back because a deadlock was detected. */
    public static final int XA_RBDEADLOCK = XA_RBBASE + 2;

    /** It was rolled back because it violated an integrity constraint. */
    public static final int XA_RBINTEGRITY = XA_RBBASE + 3;

    /** It was rolled back for a reason the manager does not classify. */
    public static final int XA_RBOTHER = XA_RBBASE + 4;

    /** It was rolled back because of a protocol error inside the resource. */
    public static final int XA_RBPROTO = XA_RBBASE + 5;

    /** It was rolled back because it took too long. */
    public static final int XA_RBTIMEOUT = XA_RBBASE + 6;

    /** It was rolled back for something transient: retrying may work. */
    public static final int XA_RBTRANSIENT = XA_RBBASE + 7;

    /** The last of the rollback codes. */
    public static final int XA_RBEND = XA_RBTRANSIENT;

    // ---- the rest -----------------------------------------------------------------------------------

    /** The transaction was resumed in a place where it cannot migrate. */
    public static final int XA_NOMIGRATE = 9;

    /** It may have decided on its own; it is not known. */
    public static final int XA_HEURHAZ = 8;

    /** It committed on its own. */
    public static final int XA_HEURCOM = 7;

    /** It rolled back on its own. */
    public static final int XA_HEURRB = 6;

    /** Part committed and part rolled back on its own: the worst. */
    public static final int XA_HEURMIX = 5;

    /** There is no work yet; ask again. */
    public static final int XA_RETRY = 4;

    /** The branch was read-only and has committed already. */
    public static final int XA_RDONLY = 3;

    /** There is an asynchronous operation pending. */
    public static final int XAER_ASYNC = -2;

    /** An error of the resource manager while carrying out the operation. */
    public static final int XAER_RMERR = -3;

    /** The identifier does not correspond to any transaction. */
    public static final int XAER_NOTA = -4;

    /** An invalid argument. */
    public static final int XAER_INVAL = -5;

    /** The operation was called out of sequence. */
    public static final int XAER_PROTO = -6;

    /** The resource manager is not available. */
    public static final int XAER_RMFAIL = -7;

    /** There is a transaction with that identifier already. */
    public static final int XAER_DUPID = -8;

    /** The resource is working outside the global transaction. */
    public static final int XAER_OUTSIDE = -9;

    public XAException() {
        super();
    }

    public XAException(String s) {
        super(s);
    }

    public XAException(int errcode) {
        super();
        this.errorCode = errcode;
    }
}
