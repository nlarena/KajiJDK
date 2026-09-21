package java.awt;

/**
 * Where the mouse pointer is, and on which screen.
 *
 * <p>It is a **snapshot**, not tracking: the data are those of the instant {@link
 * MouseInfo#getPointerInfo} took it, and they do not update by themselves. That is why it cannot be
 * constructed from outside.
 */
public final class PointerInfo {

    /** Which screen it was on. */
    private final GraphicsDevice device;

    /** Where it was, in that screen's coordinates. */
    private final Point location;

    /**
     * Package-private, for {@link MouseInfo}; here that method always throws, so nothing builds
     * one.
     */
    PointerInfo(GraphicsDevice device, Point location) {
        this.device = device;
        this.location = location;
    }

    /** The screen the pointer was on. */
    public GraphicsDevice getDevice() {
        return this.device;
    }

    /**
     * Where it was.
     *
     * @return a copy; moving the returned point moves nothing
     */
    public Point getLocation() {
        return new Point(this.location);
    }
}
