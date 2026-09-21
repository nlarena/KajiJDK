package java.awt;

import java.awt.datatransfer.Clipboard;
import java.awt.im.InputMethodHighlight;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The toolkit of a machine without a screen.
 *
 * <p>The rule is a single one and it is applied everywhere: **what can be answered without a screen
 * is answered truthfully; what needs one throws {@link HeadlessException}**. There is not one
 * invented value. A screen size that does not exist is not approximated, a font that cannot be
 * measured is not estimated.
 *
 * <p>It is more than it looks. The event queue with its dispatch thread works, and so do the colour
 * model, the building of images from a producer of pixels, the preparing and checking of images,
 * and the desktop properties.
 *
 * <p>The clipboard it returns is **private**: it lives inside this process. It is not a substitute
 * for the system clipboard in disguise — it is a real clipboard, good for moving data between parts
 * of the same application, which is all that can be done without a desktop to share with.
 *
 * <p>It is not public: one gets to it through {@link Toolkit#getDefaultToolkit}.
 */
class HeadlessToolkit extends Toolkit {

    private final EventQueue eventQueue = new EventQueue();
    private final Clipboard clipboard = new Clipboard("System");

    /** The toolkit without a screen. */
    HeadlessToolkit() {
    }

    /**
     * The size of the screen.
     *
     * @throws HeadlessException always: there is no screen to measure
     */
    public Dimension getScreenSize() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * The dots per inch.
     *
     * @throws HeadlessException always, for the same reason
     */
    public int getScreenResolution() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * The pixel format.
     *
     * <p>This one **can** be answered: it is eight-bit-per-channel ARGB, which is the format the
     * library works with colour in, whatever screen it is shown on.
     */
    public ColorModel getColorModel() {
        return ColorModel.getRGBdefault();
    }

    /**
     * The installed fonts.
     *
     * <p>The five logical families, which are the ones that exist without a font engine: they are
     * names, not files, and this library recognises them.
     */
    public String[] getFontList() {
        String[] out = new String[5];
        out[0] = Font.DIALOG;
        out[1] = Font.DIALOG_INPUT;
        out[2] = Font.SANS_SERIF;
        out[3] = Font.SERIF;
        out[4] = Font.MONOSPACED;
        return out;
    }

    /**
     * The metrics of the only font of this VM, whatever the one asked for.
     *
     * <p>See {@link KajiFontMetrics}: every font is substituted by the same face, and the metrics
     * are the ones of that face, which is what the rasteriser actually paints.
     *
     * <p>This note used to say that measuring text demanded reading the glyphs of the font file and
     * that the method therefore threw {@code UnsupportedOperationException}. It does not throw: the
     * substitute face is measured instead.
     */
    public FontMetrics getFontMetrics(Font font) {
        return new KajiFontMetrics(font);
    }

    /** There is nothing waiting to be drawn: there is no screen. */
    public void sync() {
    }

    /**
     * An image read from a file.
     *
     * @return `null` if the file cannot be read or is not an image it knows how to decode
     */
    public Image getImage(String filename) {
        return this.createImage(filename);
    }

    /**
     * An image read from an address.
     *
     * @return `null` if it cannot be read
     */
    public Image getImage(URL url) {
        return this.createImage(url);
    }

    /**
     * An image read from a file.
     *
     * @return `null` always: decoding PNG or JPEG is the work of `javax.imageio`, which this
     *     library does not ship. Returning an empty image would be worse: whoever drew it would see
     *     nothing and would not know why.
     */
    public Image createImage(String filename) {
        return null;
    }

    /**
     * An image read from an address.
     *
     * @return `null` always, for the same reason
     */
    public Image createImage(URL url) {
        return null;
    }

    /**
     * An image from a producer of pixels.
     *
     * <p>This one **does** work, and it is the one that matters: there is nothing to decode, the
     * pixels come given. It is what makes the filter pipeline of {@code java.awt.image} useful end
     * to end.
     *
     * @return the image, or `null` if the producer did not get to deliver it
     */
    public Image createImage(ImageProducer producer) {
        PixelGrabber pg = new PixelGrabber(producer, 0, 0, -1, -1, null, 0, 0);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            return null;
        }
        int w = pg.getWidth();
        int h = pg.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, (int[]) pg.getPixels(), 0, w);
        return out;
    }

    /**
     * An image decoded from some bytes.
     *
     * @return `null` always: a decoder of image formats is needed
     */
    public Image createImage(byte[] imagedata, int imageoffset, int imagelength) {
        return null;
    }

    /**
     * Starts preparing an image.
     *
     * <p>An image that is already in memory does not need preparing: it answers yes straight away.
     */
    public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
        return image != null && image.getWidth(observer) >= 0;
    }

    /**
     * How much of an image was prepared.
     *
     * <p>An image in memory is whole from the start.
     */
    public int checkImage(Image image, int width, int height, ImageObserver observer) {
        if (image == null) {
            return ImageObserver.ERROR | ImageObserver.ABORT;
        }
        if (image.getWidth(observer) < 0) {
            return ImageObserver.ERROR | ImageObserver.ABORT;
        }
        return ImageObserver.WIDTH | ImageObserver.HEIGHT | ImageObserver.ALLBITS;
    }

    /**
     * A print job.
     *
     * @return `null` always: there is no printing system to send it to
     */
    public PrintJob getPrintJob(Frame frame, String jobtitle, Properties props) {
        return null;
    }

    /** There is no bell to ring. */
    public void beep() {
    }

    /**
     * The clipboard.
     *
     * <p>It is one **private** to this process, not the system's: there is no system to share with.
     * It is still good for moving data between parts of the same application.
     */
    public Clipboard getSystemClipboard() {
        return this.clipboard;
    }

    /** Whether it supports that modality scope. */
    public boolean isModalityTypeSupported(Dialog.ModalityType modalityType) {
        return modalityType == Dialog.ModalityType.MODELESS;
    }

    /** Whether it supports that kind of exclusion. */
    public boolean isModalExclusionTypeSupported(Dialog.ModalExclusionType type) {
        return type == Dialog.ModalExclusionType.NO_EXCLUDE;
    }

    /** The event queue: this one really works, with its dispatch thread. */
    protected EventQueue getSystemEventQueueImpl() {
        return this.eventQueue;
    }

    /**
     * How to draw a stretch that is being composed.
     *
     * @return an empty map: there is no input method composing anything
     */
    public Map<java.awt.font.TextAttribute, ?> mapInputMethodHighlight(
            InputMethodHighlight highlight) {
        return new HashMap<java.awt.font.TextAttribute, Object>();
    }
}
