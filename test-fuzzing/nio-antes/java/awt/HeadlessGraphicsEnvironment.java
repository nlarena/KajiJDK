package java.awt;

import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * The graphics environment of a machine without a screen.
 *
 * <p>It is to {@link GraphicsEnvironment} what {@link HeadlessToolkit} is to {@link Toolkit}, and
 * it follows the same rule: it answers truthfully whatever needs no screen, throws {@link
 * HeadlessException} on whatever does, and invents nothing in between.
 *
 * <p>The font lists come out empty. What is missing there is not a screen but a font engine: there
 * is no way to read the installed ones nor to create one from a file, so there is none to name. The
 * reason is in {@link GraphicsEnvironment#registerFont}.
 */
final class HeadlessGraphicsEnvironment extends GraphicsEnvironment {

    /** {@link GraphicsEnvironment#getLocalGraphicsEnvironment} builds it. */
    HeadlessGraphicsEnvironment() {
    }

    /**
     * The screens.
     *
     * @throws HeadlessException always: there is none
     */
    public GraphicsDevice[] getScreenDevices() throws HeadlessException {
        throw new HeadlessException(GraphicsEnvironment.getHeadlessMessage());
    }

    /**
     * The main screen.
     *
     * @throws HeadlessException always, for the same reason
     */
    public GraphicsDevice getDefaultScreenDevice() throws HeadlessException {
        throw new HeadlessException(GraphicsEnvironment.getHeadlessMessage());
    }

    /**
     * A drawing context over that image.
     *
     * <p>No screen stands in the way —drawing onto a {@link BufferedImage} is all in memory— but
     * the rasteriser is missing: nobody knows how to turn a line or a letter into pixels.
     *
     * @throws UnsupportedOperationException always, with that reason. Returning a `Graphics2D` that
     *     accepts everything and draws nothing would be worse: the caller would believe the image
     *     has something in it.
     * @throws NullPointerException if the image is `null`
     */
    public Graphics2D createGraphics(BufferedImage img) {
        if (img == null) {
            throw new NullPointerException("BufferedImage cannot be null");
        }
        throw new UnsupportedOperationException(
                "this implementation has no rasteriser: there is nothing to draw on the image with");
    }

    /**
     * All the fonts.
     *
     * @return an empty array; never `null`
     */
    public Font[] getAllFonts() {
        return new Font[0];
    }

    /**
     * The family names.
     *
     * @return an empty array
     */
    public String[] getAvailableFontFamilyNames() {
        return new String[0];
    }

    /**
     * The same, with the names in that locale.
     *
     * @param l the locale; `null` is accepted because there is nothing to translate
     * @return an empty array
     */
    public String[] getAvailableFontFamilyNames(Locale l) {
        return new String[0];
    }
}
