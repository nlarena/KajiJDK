package javax.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.net.URL;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * An icon made of an image.
 *
 * <h2>It loads before returning, and that is why it measures</h2>
 *
 * <p>An AWT {@link Image} loads <em>bit by bit</em>: on creating it one does not know how much
 * it measures, and the measurements arrive while the file is read. An icon cannot afford that
 * -- the first thing it is asked is {@link #getIconWidth} -- so this class waits for the image
 * to finish loading before answering.
 *
 * <p>Who waits is a {@link MediaTracker}, and there is <strong>a single one for the whole
 * application</strong> ({@link #tracker}): each icon signs up with an identifier of its own,
 * waits for its part and signs off. Sharing it is what avoids creating one tracker per icon on
 * a screen with a hundred.
 *
 * <h2>The description is not drawn</h2>
 *
 * <p>{@link #setDescription} is text for whoever does not see the image -- a screen reader --.
 * It appears nowhere; its only use is accessibility.
 *
 * <h2>With no screen</h2>
 *
 * <p>Loading an image does not need a screen: it is read, decoded and how much it measures is
 * known. What needs a screen is drawing it, and {@link #paintIcon} receives whoever's
 * {@link Graphics} -- an image in memory serves.
 */
public class ImageIcon implements Icon, Serializable, Accessible {

    /** The image. */
    transient Image image;

    /** How the loading came out; see {@link MediaTracker}. */
    transient int loadStatus = 0;

    ImageObserver imageObserver;

    String description = null;

    /** The component the tracker is hung from; it is never drawn. */
    protected static final Component component = new LoadComponent();

    /** The shared tracker; see the class note. */
    protected static final MediaTracker tracker = new MediaTracker(component);

    int width = -1;
    int height = -1;

    private static int nextId = 0;

    /** A component that exists only in order to hang the tracker from it. */
    private static class LoadComponent extends Component {
    }

    /** From a file, with a description. */
    public ImageIcon(String filename, String description) {
        image = Toolkit.getDefaultToolkit().getImage(filename);
        this.description = description;
        if (image == null) {
            // With no decoder the toolkit returns null where the JDK returns an image that fails to
                        // load afterwards. The result that is seen is the same, and it is noted the
                        // same: measurements at -1 and the loading at ERRORED. See the class note.
            loadStatus = MediaTracker.ERRORED;
            return;
        }
        loadImage(image);
    }

    /** From a file; the description is the file's name. */
    public ImageIcon(String filename) {
        this(filename, filename);
    }

    /** From an address, with a description. */
    public ImageIcon(URL location, String description) {
        image = Toolkit.getDefaultToolkit().getImage(location);
        this.description = description;
        if (image == null) {
            // With no decoder the toolkit returns null where the JDK returns an image that fails to
                        // load afterwards. The result that is seen is the same, and it is noted the
                        // same: measurements at -1 and the loading at ERRORED. See the class note.
            loadStatus = MediaTracker.ERRORED;
            return;
        }
        loadImage(image);
    }

    /** From an address; the description is the address. */
    public ImageIcon(URL location) {
        this(location, location.toExternalForm());
    }

    /** From an image already made, with a description. */
    public ImageIcon(Image image, String description) {
        this(image);
        this.description = description;
    }

    /**
     * From an image already made.
     *
     * <p>If the image brings a description inside -- it can be set with
     * {@code setProperty("comment", ...)} -- it is used.
     */
    public ImageIcon(Image image) {
        this.image = image;
        Object o = image.getProperty("comment", imageObserver);
        if (o instanceof String) {
            description = (String) o;
        }
        loadImage(image);
    }

    /** From the bytes of an image file, with a description. */
    public ImageIcon(byte[] imageData, String description) {
        this.image = Toolkit.getDefaultToolkit().createImage(imageData);
        if (image == null) {
            return;
        }
        this.description = description;
        loadImage(image);
    }

    /** From the bytes; the description comes from the image if it brings one. */
    public ImageIcon(byte[] imageData) {
        this.image = Toolkit.getDefaultToolkit().createImage(imageData);
        if (image == null) {
            return;
        }
        Object o = image.getProperty("comment", imageObserver);
        if (o instanceof String) {
            description = (String) o;
        }
        loadImage(image);
    }

    /** An empty icon, to be filled in afterwards with {@link #setImage}. */
    public ImageIcon() {
    }

    /**
     * It waits for the image to finish loading and notes how much it measures.
     *
     * <p>The identifier has to be different per icon: the tracker is a single one and two icons
     * with the same number would wait for each other. See the class note.
     */
    protected void loadImage(Image image) {
        MediaTracker mTracker = tracker;
        int id = nextId();
        synchronized (mTracker) {
            mTracker.addImage(image, id);
            try {
                mTracker.waitForID(id, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                loadStatus = mTracker.statusID(id, false);
                mTracker.removeImage(image, id);
            }
        }
        width = image.getWidth(imageObserver);
        height = image.getHeight(imageObserver);
    }

    private static synchronized int nextId() {
        nextId = nextId + 1;
        return nextId;
    }

    /** How the loading came out; the constants are {@link MediaTracker}'s. */
    public int getImageLoadStatus() {
        return loadStatus;
    }

    public Image getImage() {
        return image;
    }

    /** It changes the image; it waits and measures again. */
    public void setImage(Image image) {
        this.image = image;
        loadImage(image);
    }

    /** The text for whoever does not see the image; see the class note. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** It draws the image at that position. */
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        if (imageObserver == null) {
            g.drawImage(image, x, y, c);
        } else {
            g.drawImage(image, x, y, imageObserver);
        }
    }

    /** The width, or -1 if the image could not be loaded. */
    public int getIconWidth() {
        return width;
    }

    /** The height, or -1 if the image could not be loaded. */
    public int getIconHeight() {
        return height;
    }

    /**
     * Who learns that the image advanced.
     *
     * <p>It is needed for the images that go on changing after being loaded -- an animated GIF --:
     * with no observer the first frame is drawn and there it stays.
     */
    public void setImageObserver(ImageObserver observer) {
        imageObserver = observer;
    }

    public ImageObserver getImageObserver() {
        return imageObserver;
    }

    public String toString() {
        if (description != null) {
            return description;
        }
        return super.toString();
    }

    private AccessibleContext accessibleContext = null;

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
