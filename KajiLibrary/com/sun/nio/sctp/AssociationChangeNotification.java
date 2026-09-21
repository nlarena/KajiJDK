package com.sun.nio.sctp;

/**
 * The association changed state.
 *
 * <p>It is the notification that tells the whole life cycle: it was born, it died, it
 * restarted, it is closing, or it could not be established. {@link AssocChangeEvent} is which
 * of the five.
 */
public abstract class AssociationChangeNotification implements Notification {

    /**
     * What happened to the association.
     *
     * <p>The distinction that matters most is {@link #COMM_LOST} against {@link #SHUTDOWN}: the
     * first is that it was lost, the second that it was closed as it should be. Confusing them
     * makes a normal close be reported as a network failure.
     */
    public enum AssocChangeEvent {

        /** It was established and may be used. */
        COMM_UP,
        /** It was lost: the peer stopped answering. */
        COMM_LOST,
        /** The peer restarted it. What was in flight was lost. */
        RESTART,
        /** It was closed in an orderly way. */
        SHUTDOWN,
        /** It could not be established. */
        CANT_START
    }

    /** For the SCTP implementations. */
    protected AssociationChangeNotification() {
    }

    /** The association; it may be {@code null} with {@link AssocChangeEvent#CANT_START}. */
    public abstract Association association();

    /** Which of the five events it was. */
    public abstract AssocChangeEvent event();
}
