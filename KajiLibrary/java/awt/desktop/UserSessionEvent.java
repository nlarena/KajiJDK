package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.UserSessionEvent -- the user's session became active or inactive.
 *
 * <p>It is handed over by {@link UserSessionListener}. A session becomes inactive when the user
 * switches account, locks the screen, or connects from somewhere else; the program <b>keeps
 * running</b>, it is just that nobody is watching it.
 *
 * <p>{@link #getReason} says which of those things happened, and it is what allows reacting
 * differently: a screen lock is a good moment to ask for the password again, a console switch not
 * necessarily.
 */
public final class UserSessionEvent extends AppEvent {

    private static final long serialVersionUID = 6747138462796569055L;

    /** Why it changed. */
    private final Reason reason;

    /**
     * The four reasons a session changes state.
     *
     * <p>{@link #UNSPECIFIED} is not an error: there are systems that report the change without
     * saying why, and a handler has to be ready for that.
     */
    public enum Reason {

        /** The system did not say why. */
        UNSPECIFIED,

        /** The user switched local console. */
        CONSOLE,

        /** Someone connected or disconnected remotely. */
        REMOTE,

        /** The screen was locked or unlocked. */
        LOCK
    }

    /** @param reason why it changed */
    public UserSessionEvent(final Reason reason) {
        this.reason = reason;
    }

    /** Why it changed. See the class note. */
    public Reason getReason() {
        return this.reason;
    }
}
