package java.rmi;

import java.io.IOException;

/**
 * KajiLibrary's java.rmi.RemoteException -- a remote call failed.
 *
 * <p>The base of almost everything that can go wrong in RMI, and the one that <b>every</b> method
 * of a {@link Remote} interface has to declare. It is checked on purpose: it forces whoever
 * writes the client to take on board that the network exists.
 *
 * <h2>What is not known when it is thrown</h2>
 *
 * <p>It is the important thing about this class and it is almost never taken into account: in
 * general <b>it is not known whether the remote method got to run</b>. If the connection dropped
 * after sending the call and before receiving the reply, the operation may have been done anyway.
 *
 * <p>That is why retrying blindly is dangerous, and why remote operations are best designed
 * idempotent. This note used to say the only ones about which it is known are
 * {@link MarshalException} --it did not go out-- and {@link NoSuchObjectException} --it does not
 * exist--. The first half is not true: the JDK's contract for {@code MarshalException} covers
 * marshalling the return value as well as the call, so the call may or may not have reached the
 * server (nothing in KajiLibrary throws it; checked with grep). The ones that do settle it are
 * {@link NoSuchObjectException} --the object does not exist-- and {@link ConnectException} --the
 * connection was refused, so the call never went out--.
 *
 * <h2>The {@link #detail} field and the cause</h2>
 *
 * <p>{@code detail} is public and dates from 1996, before the chained-cause mechanism. When that
 * arrived, in 1.4, {@link #getCause} was made to return it, so <b>they are the same thing</b>.
 *
 * <p>That has two consequences: {@link #getMessage} appends the detail to its own message, and
 * {@code initCause} throws {@link IllegalStateException} because the constructor already set it --
 * even when it was built without a cause. (This note used to say {@code getMessage} appends the
 * detail's message; it appends {@code detail.toString()}, class name included, as the code below
 * shows.)
 */
public class RemoteException extends IOException {

    private static final long serialVersionUID = -5148567311918794206L;

    /**
     * The original exception, if there is one.
     *
     * <p>Public for compatibility; the same thing {@link #getCause} returns.
     */
    public Throwable detail;

    /** Without detail. */
    public RemoteException() {
        initCause(null);
    }

    /** @param s the message */
    public RemoteException(String s) {
        super(s);
        initCause(null);
    }

    /**
     * @param s the message
     * @param cause the original one
     */
    public RemoteException(String s, Throwable cause) {
        super(s);
        initCause(null);
        this.detail = cause;
    }

    /** Its own message, with the detail's {@code toString()} underneath if there is one. */
    @Override
    public String getMessage() {
        if (this.detail == null) {
            return super.getMessage();
        }
        return super.getMessage() + "; nested exception is: \n\t" + this.detail.toString();
    }

    /** The detail. See the class note. */
    @Override
    public Throwable getCause() {
        return this.detail;
    }
}
