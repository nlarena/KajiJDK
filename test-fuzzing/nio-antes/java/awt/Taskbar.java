package java.awt;

/**
 * The taskbar or the dock: where the system shows the program while it runs.
 *
 * <p>It serves three things: changing the icon, showing progress, and asking for the user's
 * attention —the bouncing of the dock on macOS, the flashing on Windows—.
 *
 * <p><strong>Each thing is supported or not on its own</strong>, and that is where
 * {@link #isSupported} comes from: there is not one taskbar but three or four implementations that
 * do different things. Windows has progress on the window and not on the program's icon; macOS the
 * other way round. Asking first is mandatory, and the methods throw if they are called without
 * asking.
 *
 * <p>Here none of it is supported: {@link #isTaskbarSupported} gives `false` and
 * {@link #getTaskbar} throws, which is what the JDK does without a desktop.
 */
public class Taskbar {

    /** Each thing a taskbar may know how to do. */
    public static enum Feature {

        /** A small text over the program's icon. */
        ICON_BADGE_TEXT,

        /** A number over the program's icon. */
        ICON_BADGE_NUMBER,

        /** An image over a window's icon. */
        ICON_BADGE_IMAGE_WINDOW,

        /** Change the program's icon. */
        ICON_IMAGE,

        /** A menu of its own on the program's icon. */
        MENU,

        /** The state of a window's progress bar. */
        PROGRESS_STATE_WINDOW,

        /** The progress value of the program. */
        PROGRESS_VALUE,

        /** The progress value of a window. */
        PROGRESS_VALUE_WINDOW,

        /** Ask for the user's attention about the program. */
        USER_ATTENTION,

        /** Ask for it about a window. */
        USER_ATTENTION_WINDOW
    }

    /** What state a progress bar is in. */
    public static enum State {

        /** No bar. */
        OFF,

        /** Advancing normally. */
        NORMAL,

        /** Paused: it is seen but does not move. */
        PAUSED,

        /** With no known percentage: the bar moves by itself. */
        INDETERMINATE,

        /** Something failed: the bar is seen in red. */
        ERROR
    }

    /** The only taskbar, if it ever gets asked for. */
    private static Taskbar instance;

    /** The icon's own menu. */
    private PopupMenu menu;

    /** The program's icon. */
    private Image icon;

    /** Not instantiated from outside. */
    private Taskbar() {
    }

    /**
     * The taskbar of this session.
     *
     * <p>The JDK checks for a screen first and throws {@link HeadlessException} there; here the
     * answer comes earlier, because no taskbar is supported at all.
     *
     * @throws UnsupportedOperationException always here: there is no desktop
     */
    public static synchronized Taskbar getTaskbar() {
        if (!isTaskbarSupported()) {
            throw new UnsupportedOperationException("Taskbar API is not supported on the current platform");
        }
        if (instance == null) {
            instance = new Taskbar();
        }
        return instance;
    }

    /**
     * Whether this platform has a taskbar that can be driven.
     *
     * @return `false` always
     */
    public static boolean isTaskbarSupported() {
        return false;
    }

    /**
     * Whether it supports that feature.
     *
     * @return `false` for all of them: there is no taskbar to do them
     * @throws NullPointerException if the feature is `null`
     */
    public boolean isSupported(Feature feature) {
        if (feature == null) {
            throw new NullPointerException("feature");
        }
        return false;
    }

    /**
     * Asks for the user's attention about the program.
     *
     * @param enabled whether to turn the notice on or off
     * @param critical whether the notice is insistent
     * @throws UnsupportedOperationException if {@link Feature#USER_ATTENTION} is not supported
     */
    public void requestUserAttention(boolean enabled, boolean critical) {
        this.require(Feature.USER_ATTENTION);
    }

    /**
     * Asks for attention about that window.
     *
     * @throws UnsupportedOperationException if {@link Feature#USER_ATTENTION_WINDOW} is not
     *     supported
     * @throws IllegalArgumentException if the window is `null`
     */
    public void requestWindowUserAttention(Window w) {
        this.checkWindow(w);
        this.require(Feature.USER_ATTENTION_WINDOW);
    }

    /**
     * Gives the program's icon a menu of its own.
     *
     * @throws UnsupportedOperationException if {@link Feature#MENU} is not supported
     */
    public void setMenu(PopupMenu menu) {
        this.require(Feature.MENU);
        this.menu = menu;
    }

    /**
     * The icon's own menu.
     *
     * @throws UnsupportedOperationException if {@link Feature#MENU} is not supported
     */
    public PopupMenu getMenu() {
        this.require(Feature.MENU);
        return this.menu;
    }

    /**
     * Changes the program's icon.
     *
     * @throws UnsupportedOperationException if {@link Feature#ICON_IMAGE} is not supported
     */
    public void setIconImage(Image image) {
        this.require(Feature.ICON_IMAGE);
        this.icon = image;
    }

    /**
     * The program's icon.
     *
     * @throws UnsupportedOperationException if {@link Feature#ICON_IMAGE} is not supported
     */
    public Image getIconImage() {
        this.require(Feature.ICON_IMAGE);
        return this.icon;
    }

    /**
     * Puts a text over the program's icon.
     *
     * @param badge the text, or `null` to take it away
     * @throws UnsupportedOperationException if neither {@link Feature#ICON_BADGE_TEXT} nor
     *     {@link Feature#ICON_BADGE_NUMBER} is supported
     */
    public void setIconBadge(String badge) {
        if (!this.isSupported(Feature.ICON_BADGE_TEXT)
                && !this.isSupported(Feature.ICON_BADGE_NUMBER)) {
            throw new UnsupportedOperationException("The ICON_BADGE_TEXT feature is not supported on the current platform");
        }
    }

    /**
     * Puts an image over a window's icon.
     *
     * @throws UnsupportedOperationException if {@link Feature#ICON_BADGE_IMAGE_WINDOW} is not
     *     supported
     * @throws IllegalArgumentException if the window is `null`
     */
    public void setWindowIconBadge(Window w, Image badge) {
        this.checkWindow(w);
        this.require(Feature.ICON_BADGE_IMAGE_WINDOW);
    }

    /**
     * Shows the progress of the program.
     *
     * @param value from 0 to 100; outside that range, the bar goes off
     * @throws UnsupportedOperationException if {@link Feature#PROGRESS_VALUE} is not supported
     */
    public void setProgressValue(int value) {
        this.require(Feature.PROGRESS_VALUE);
    }

    /**
     * Shows the progress of a window.
     *
     * @param value from 0 to 100; outside that range, the bar goes off
     * @throws UnsupportedOperationException if {@link Feature#PROGRESS_VALUE_WINDOW} is not
     *     supported
     * @throws IllegalArgumentException if the window is `null`
     */
    public void setWindowProgressValue(Window w, int value) {
        this.checkWindow(w);
        this.require(Feature.PROGRESS_VALUE_WINDOW);
    }

    /**
     * Changes the state of a window's progress bar.
     *
     * @throws UnsupportedOperationException if {@link Feature#PROGRESS_STATE_WINDOW} is not
     *     supported
     * @throws IllegalArgumentException if the window is `null`
     * @throws NullPointerException if the state is `null`
     */
    public void setWindowProgressState(Window w, State state) {
        this.checkWindow(w);
        if (state == null) {
            throw new NullPointerException("state");
        }
        this.require(Feature.PROGRESS_STATE_WINDOW);
    }

    /** Throws if that feature is not supported. */
    private void require(Feature f) {
        if (!this.isSupported(f)) {
            throw new UnsupportedOperationException("The " + f.name()
                    + " feature is not supported on the current platform");
        }
    }

    /** That the window is not `null`. */
    private void checkWindow(Window w) {
        if (w == null) {
            throw new IllegalArgumentException("Window must not be null");
        }
    }
}
