package java.awt;

/**
 * A screen, a printer or a buffer in memory: something that can be drawn on.
 *
 * <p>A device has one or several {@link GraphicsConfiguration}, which are the combinations of
 * colour depth and capabilities it can be used with. The same screen may offer several, and that is
 * where one picks.
 *
 * <p><strong>The exclusive full-screen mode is not supported.</strong> This note used to say that
 * `setFullScreenWindow` and `getFullScreenWindow` were missing, because they take and return a
 * `java.awt.Window` that dragged in a tree of components the library did not have. Both are written
 * now. What is still true is that no device gets taken over: {@link #isFullScreenSupported} answers
 * `false`, which is the truth about this device and not an excuse, and the window is only recorded.
 */
public abstract class GraphicsDevice {

    /** A screen. */
    public static final int TYPE_RASTER_SCREEN = 0;

    /** A printer. */
    public static final int TYPE_PRINTER = 1;

    /** A buffer in memory. */
    public static final int TYPE_IMAGE_BUFFER = 2;

    private DisplayMode originalMode;

    /** For the subclasses. */
    protected GraphicsDevice() {
    }

    /** The kinds of translucency a device may or may not support in its windows. */
    public static enum WindowTranslucency {

        /** Each pixel is either fully opaque or fully transparent. */
        PERPIXEL_TRANSPARENT,

        /** The whole window has one uniform opacity. */
        TRANSLUCENT,

        /** Each pixel has an opacity of its own. */
        PERPIXEL_TRANSLUCENT
    }

    /** `TYPE_RASTER_SCREEN`, `TYPE_PRINTER` or `TYPE_IMAGE_BUFFER`. */
    public abstract int getType();

    /** An identifier of the device. */
    public abstract String getIDstring();

    /** All of its configurations. */
    public abstract GraphicsConfiguration[] getConfigurations();

    /** The configuration it uses by default. */
    public abstract GraphicsConfiguration getDefaultConfiguration();

    /**
     * The configuration that best meets those requirements.
     *
     * @throws NullPointerException if the template is `null`
     */
    public GraphicsConfiguration getBestConfiguration(GraphicsConfigTemplate gct) {
        GraphicsConfiguration[] configs = this.getConfigurations();
        return gct.getBestConfiguration(configs);
    }

    /**
     * Whether it supports the exclusive full-screen mode.
     *
     * <p>It answers `false` because the exclusive mode means taking a device over, and there is no
     * device here to take over.
     */
    public boolean isFullScreenSupported() {
        return false;
    }

    /** Whether its display mode can be changed. */
    public boolean isDisplayChangeSupported() {
        return false;
    }

    /**
     * Changes the display mode.
     *
     * @throws UnsupportedOperationException if the device does not support the change
     * @throws IllegalArgumentException if the mode is not one of those {@link #getDisplayModes}
     *     returns
     */
    public void setDisplayMode(DisplayMode dm) {
        if (!this.isDisplayChangeSupported()) {
            throw new UnsupportedOperationException("Cannot change display mode");
        }
        if (dm == null) {
            throw new IllegalArgumentException("Invalid display mode");
        }
        DisplayMode[] modes = this.getDisplayModes();
        for (int i = 0; i < modes.length; i++) {
            if (dm.equals(modes[i])) {
                if (this.originalMode == null) {
                    this.originalMode = this.getDisplayMode();
                }
                return;
            }
        }
        throw new IllegalArgumentException("Invalid display mode");
    }

    /** The current display mode, or `null` if it is not known. */
    public DisplayMode getDisplayMode() {
        return null;
    }

    /** The available display modes. */
    public DisplayMode[] getDisplayModes() {
        DisplayMode[] single = new DisplayMode[1];
        single[0] = this.getDisplayMode();
        if (single[0] == null) {
            return new DisplayMode[0];
        }
        return single;
    }

    /**
     * How much accelerated memory is left, or -1 if it is not known.
     *
     * <p>The -1 is an answer: it means there is no way to find out, which is different from none
     * being left.
     */
    public int getAvailableAcceleratedMemory() {
        return -1;
    }

    /**
     * Whether it supports that kind of translucency in windows.
     *
     * @throws NullPointerException if the kind is `null`
     */
    public boolean isWindowTranslucencySupported(WindowTranslucency translucencyKind) {
        if (translucencyKind == null) {
            throw new NullPointerException("translucencyKind cannot be null");
        }
        return false;
    }

    /**
     * The window that is in exclusive full-screen mode.
     *
     * @return the window handed to {@link #setFullScreenWindow}, or `null` if there is none
     */
    public Window getFullScreenWindow() {
        return this.fullScreenWindow;
    }

    /**
     * Puts a window into exclusive full screen, or takes out whichever one was there by passing
     * `null`.
     *
     * <p>Since {@link #isFullScreenSupported} answers `false`, this does what the JDK does when the
     * exclusive mode is not available: it **simulates** it, growing the window to the size of the
     * screen and showing it, without taking the device over. Here there is not even a screen to
     * measure, so the only observable effect is that the window is recorded and {@link
     * #getFullScreenWindow} gives it back.
     *
     * <p>When the window is replaced or cleared, the original display mode is restored if it had
     * been changed —which cannot happen here, because {@link #isDisplayChangeSupported} is `false`
     * and {@link #setDisplayMode} throws before changing anything.
     */
    public void setFullScreenWindow(Window w) {
        if (this.fullScreenWindow != null && this.originalMode != null) {
            this.setDisplayMode(this.originalMode);
            this.originalMode = null;
        }
        this.fullScreenWindow = w;
    }

    /** The full-screen window, or `null`. */
    private Window fullScreenWindow;
}
