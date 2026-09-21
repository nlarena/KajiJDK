package javax.transaction.xa;

/**
 * KajiLibrary's javax.transaction.xa.XAResource -- a resource that knows how to take part in a
 * transaction that spans others.
 *
 * <p>It is the **two-phase commit** put into an interface: first each participant is asked whether
 * it can commit ({@link #prepare}), and only if they all say yes are they ordered to do it
 * ({@link #commit}). That separation is what allows two different databases to commit as if they
 * were one: between the "I can" and the "do it" there is nothing left that can fail on the
 * resource's side.
 *
 * <p>The price is in {@link XAException#XA_HEURMIX}: if a participant tires of waiting between the
 * two phases and decides on its own, the atomicity breaks and no protocol recovers it.
 */
public interface XAResource {

    /** The branch can be committed. */
    int XA_OK = 0;

    /** The branch was read-only: that is that, no second phase is needed. */
    int XA_RDONLY = 3;

    /** No flags. */
    int TMNOFLAGS = 0;

    /** To join a branch that exists already. */
    int TMJOIN = 2097152;

    /** To finish walking the in-doubt transactions. */
    int TMENDRSCAN = 8388608;

    /** The work of the branch failed. */
    int TMFAIL = 536870912;

    /** To commit in a single phase: possible only if the resource is the only participant. */
    int TMONEPHASE = 1073741824;

    /** To resume a suspended branch. */
    int TMRESUME = 134217728;

    /** To start walking the in-doubt transactions. */
    int TMSTARTRSCAN = 16777216;

    /** The work of the branch finished well. */
    int TMSUCCESS = 67108864;

    /** To suspend the branch without ending it. */
    int TMSUSPEND = 33554432;

    /** The work of that branch starts. */
    void start(Xid xid, int flags) throws XAException;

    /** The work of that branch ends. */
    void end(Xid xid, int flags) throws XAException;

    /**
     * First phase: whether the resource can commit.
     *
     * @return {@link #XA_OK}, or {@link #XA_RDONLY} if there was nothing to write
     */
    int prepare(Xid xid) throws XAException;

    /** Second phase: commit. `onePhase` skips the first, valid only if there are no others. */
    void commit(Xid xid, boolean onePhase) throws XAException;

    /** To roll the branch back. */
    void rollback(Xid xid) throws XAException;

    /** To forget a branch that decided on its own. */
    void forget(Xid xid) throws XAException;

    /**
     * The transactions that were left **in doubt**.
     *
     * <p>It is how one gets out of a crash between the two phases: on restarting, the coordinator asks
     * what was left prepared and unresolved and finishes it.
     */
    Xid[] recover(int flag) throws XAException;

    /** Whether this resource and the other are the same manager -- it decides whether they share a
     * branch. */
    boolean isSameRM(XAResource xares) throws XAException;

    int getTransactionTimeout() throws XAException;

    boolean setTransactionTimeout(int seconds) throws XAException;
}
