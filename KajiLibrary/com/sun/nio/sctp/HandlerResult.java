package com.sun.nio.sctp;

/**
 * What a {@link NotificationHandler} answers the channel after attending to a notification.
 *
 * <p>The channel is in the middle of a {@code receive} when a notification arrives, so after
 * handling it, it has to decide whether it goes on waiting for the message it was asked for or
 * comes back empty-handed. Who decides is the handler, because it is the only one that knows
 * whether what has just happened invalidates the wait -- a {@code COMM_LOST} invalidates it, an
 * address change does not.
 */
public enum HandlerResult {

    /** To go on waiting: the {@code receive} continues. */
    CONTINUE,
    /** To come back now: the {@code receive} finishes with no message. */
    RETURN
}
