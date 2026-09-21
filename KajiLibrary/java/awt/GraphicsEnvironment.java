package java.awt;

import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * What there is to draw on: the screens, the printers and the fonts.
 *
 * <p>It is the way in to everything that depends on the graphics hardware, and that is why it is
 * the one that can answer the question deciding half of AWT's behaviour: {@link #isHeadless}. If
 * there is no screen, everything that needs one has to throw {@link HeadlessException} instead of
 * inventing.
 *
 * <p><strong>Here it is always without a screen.</strong> This implementation has no way of talking
 * to a windowing system, so {@link #isHeadless} gives `true` and the screen methods throw.
 *
 * <p>The font list comes out **empty**, and not for lack of will. A font can only be registered if
 * it was created with {@link Font#createFont} —that is what the JDK asks for and that is how it
 * behaves here— and that method needs a font engine this library does not have. Reading the ones
 * installed on the system is not possible either. An empty list is then the right answer; returning
 * well-known names —"Dialog", "SansSerif"— would be inventing fonts that afterwards can neither be
 * measured nor drawn.
 */
public abstract class GraphicsEnvironment {

    /** The only environment, built the first time it is asked for. */
    private static GraphicsEnvironment local;

    /** For the subclasses. */
    protected GraphicsEnvironment() {
    }

    /**
     * The environment of this machine.
     *
     * <p>It is unique and built only once: asking about the screens twice cannot give two different
     * sets of screens.
     */
    public static GraphicsEnvironment getLocalGraphicsEnvironment() {
        synchronized (GraphicsEnvironment.class) {
            if (local == null) {
                local = new HeadlessGraphicsEnvironment();
            }
            return local;
        }
    }

    /**
     * Whether this machine has no screen, keyboard or mouse.
     *
     * @return `true` always: this implementation talks to no windowing system
     */
    public static boolean isHeadless() {
        return true;
    }

    /**
     * The message the {@link HeadlessException} carries. In the JDK it is `null` when there is a
     * screen; here there never is, so the text always comes back.
     */
    static String getHeadlessMessage() {
        return "\nNo X11 DISPLAY variable was set, "
                + "or no headful library support was found, "
                + "but this program performed an operation which requires it.";
    }

    /**
     * Throws if there is no screen.
     *
     * @throws HeadlessException always
     */
    static void checkHeadless() throws HeadlessException {
        throw new HeadlessException(getHeadlessMessage());
    }

    /**
     * Whether **this** environment has no screen.
     *
     * <p>It is different from {@link #isHeadless}, which talks about the machine: an environment
     * can be without a screen on a machine that does have one. Here they come to the same.
     */
    public boolean isHeadlessInstance() {
        return true;
    }

    /**
     * All the screens.
     *
     * @throws HeadlessException if there is none
     */
    public abstract GraphicsDevice[] getScreenDevices() throws HeadlessException;

    /**
     * The main screen.
     *
     * @throws HeadlessException if there is none
     */
    public abstract GraphicsDevice getDefaultScreenDevice() throws HeadlessException;

    /** A drawing context over that image. */
    public abstract Graphics2D createGraphics(BufferedImage img);

    /** All the fonts, each one at size 1. */
    public abstract Font[] getAllFonts();

    /** The family names of all the fonts. */
    public abstract String[] getAvailableFontFamilyNames();

    /** The same, with the names translated into that locale. */
    public abstract String[] getAvailableFontFamilyNames(Locale l);

    /**
     * Registers a font so that {@link #getAllFonts} and whoever asks for it by name can see it.
     *
     * <p>It is how a font that comes in a file and is not installed on the system gets used. It
     * only accepts the ones **created** with {@link Font#createFont}: one built with `new
     * Font(name, ...)` carries no glyphs with it, only a name, so registering it would add nothing.
     *
     * @return `false` always here: {@link Font#createFont} needs a font engine this library does
     *     not have, so no font ever gets to be a created one
     * @throws NullPointerException if the font is `null`
     */
    public boolean registerFont(Font font) {
        if (font == null) {
            throw new NullPointerException("font cannot be null.");
        }
        return false;
    }

    /**
     * Asks that the fonts of the current locale be preferred.
     *
     * <p>It does nothing: it is a preference about how to choose a substitute when a glyph is
     * missing, and with no installed fonts there is nothing to choose between. The JDK also ignores
     * it when its font manager does not support it.
     */
    public void preferLocaleFonts() {
    }

    /** Asks that the proportional ones be preferred; it does nothing, for the same reason. */
    public void preferProportionalFonts() {
    }

    /**
     * The centre of the useful area of the screen, which is where a window is centred.
     *
     * @throws HeadlessException if there is no screen
     */
    public Point getCenterPoint() throws HeadlessException {
        Rectangle r = this.getMaximumWindowBounds();
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    /**
     * The area of the screen a maximised window can occupy: everything but the taskbar.
     *
     * <p>It asks the {@link Toolkit} for the size of the screen, so here it always throws.
     *
     * @throws HeadlessException if there is no screen
     */
    public Rectangle getMaximumWindowBounds() throws HeadlessException {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Rectangle(d);
    }
}
