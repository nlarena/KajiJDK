package java.awt;

import java.io.IOException;
import java.net.URL;

/**
 * The welcome screen the JVM shows **before** the program starts.
 *
 * <p>What is peculiar is when it appears: the launcher shows it, with the `-splash:` option or the
 * `SplashScreen-Image` attribute of the manifest, before loading any class. That is why one cannot
 * be created: by the time the program runs, either it is already there or it never will be. The
 * only thing that can be done is drawing over it —a progress bar— and closing it.
 *
 * <p><strong>Here there is never one.</strong> {@link #getSplashScreen} throws
 * {@link HeadlessException}, it does not return `null`, and the difference is the usual one: `null`
 * means "none was asked for", and what happens here is that **there is nowhere to show it**. They
 * are two different situations and the JDK tells them apart the same way. The instance methods are
 * declared because they are part of the class, but there is no way to reach them: no instance
 * exists.
 */
public final class SplashScreen {

    /** The native handle of the window; always 0 here. */
    private final long splashPtr;

    /** Whether it was closed. */
    private boolean closed;

    /** The image it shows. */
    private URL url;

    /** The launcher builds it, nobody else. */
    SplashScreen(long ptr) {
        this.splashPtr = ptr;
    }

    /**
     * The welcome screen of this program.
     *
     * @return the screen, or `null` if the program did not start with one
     * @throws HeadlessException always here: with no screen there is nowhere to show it
     */
    public static SplashScreen getSplashScreen() {
        synchronized (SplashScreen.class) {
            if (GraphicsEnvironment.isHeadless()) {
                throw new HeadlessException();
            }
            return null;
        }
    }

    /**
     * Changes the image it shows.
     *
     * <p>In the JDK the size of the window is adjusted to the new image and the window is
     * re-centred, which is what makes it worth it: it serves for a start-up animation. Here there
     * is no window, so the only thing that happens is that the address is recorded; {@code
     * IOException} stays in the signature because the image is never read.
     *
     * @throws NullPointerException if the address is `null`
     * @throws IOException if the image cannot be read
     * @throws IllegalStateException if the screen was already closed
     */
    public void setImageURL(URL imageURL) throws NullPointerException, IOException,
            IllegalStateException {
        this.checkAlive();
        if (imageURL == null) {
            throw new NullPointerException("imageURL");
        }
        this.url = imageURL;
    }

    /**
     * Where the image came from.
     *
     * @throws IllegalStateException if the screen was already closed
     */
    public URL getImageURL() throws IllegalStateException {
        this.checkAlive();
        return this.url;
    }

    /**
     * Where it is and how big it is, in screen coordinates.
     *
     * @throws IllegalStateException always: there is no screen to measure, closed or not
     */
    public Rectangle getBounds() throws IllegalStateException {
        this.checkAlive();
        throw new IllegalStateException("no splash screen available");
    }

    /**
     * How big it is.
     *
     * @throws IllegalStateException always, for the same reason as {@link #getBounds}
     */
    public Dimension getSize() throws IllegalStateException {
        return this.getBounds().getSize();
    }

    /**
     * A context to draw over it.
     *
     * <p>In the JDK what is drawn goes onto a **transparent** layer above the image, so drawing
     * does not erase what was there: that is why {@link #update} is needed for it to be seen.
     *
     * @throws IllegalStateException always: there is no screen to draw over
     */
    public Graphics2D createGraphics() throws IllegalStateException {
        this.checkAlive();
        throw new IllegalStateException("no splash screen available");
    }

    /**
     * Shows what has been drawn since the last time.
     *
     * @throws IllegalStateException if the screen was already closed
     */
    public void update() throws IllegalStateException {
        this.checkAlive();
    }

    /**
     * Closes it and releases its resources.
     *
     * <p>After this the instance is useless: every other method throws.
     *
     * @throws IllegalStateException if it was already closed
     */
    public void close() throws IllegalStateException {
        this.checkAlive();
        this.closed = true;
    }

    /** Marks that it was closed from outside —when the program's first window is shown—. */
    void markClosed() {
        this.closed = true;
    }

    /** Whether it is still on the screen. */
    public boolean isVisible() {
        return !this.closed && this.splashPtr != 0;
    }

    /** Throws if it was already closed. */
    private void checkAlive() {
        if (this.closed) {
            throw new IllegalStateException("no splash screen available");
        }
    }
}
