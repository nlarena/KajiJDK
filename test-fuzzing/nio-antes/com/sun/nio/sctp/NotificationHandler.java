package com.sun.nio.sctp;

/**
 * Who attends to the {@link Notification}s that arrive during a {@code receive}.
 *
 * <p>Returning {@link HandlerResult} is what lets it decide whether the {@code receive} goes on
 * waiting for a message or comes back. See {@link AbstractNotificationHandler} for the
 * comfortable way of writing one.
 *
 * @param <T> the context object that is passed to the {@code receive} and arrives here without
 *     the channel looking at it
 */
public interface NotificationHandler<T> {

    /** It attends to a notification and says whether the {@code receive} goes on. */
    HandlerResult handleNotification(Notification notification, T attachment);
}
