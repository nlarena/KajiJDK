package javax.imageio;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.ImageWriter -- encodes images into one format.
 *
 * <p>The mirror of {@link ImageReader}. A concrete subclass has to supply five methods --the four
 * metadata ones and {@link #write(IIOMetadata, IIOImage, ImageWriteParam)}-- and inherits
 * everything else.
 *
 * <h2>Almost everything is optional, and you have to ask</h2>
 *
 * <p>It is what defines this class. It has nine {@code canXxx} methods, each guarding one or more
 * operations: the {@code can} returns false by default while the operations throw
 * {@link UnsupportedOperationException}. (An earlier note counted seventeen methods in pairs.)
 *
 * <p>The reason is that formats are very different from each other: TIFF can insert an image in
 * the middle of a file and replace pixels in place; PNG can do neither. A generic writer has to
 * ask before each thing.
 *
 * <h2>The three ways to write several images</h2>
 *
 * <ul>
 *   <li><b>sequence</b> ({@link #canWriteSequence}): {@code prepareWriteSequence}, several
 *       {@code writeToSequence}, {@code endWriteSequence}. It is the normal one;
 *   <li><b>insertion</b> ({@link #canInsertImage}): putting an image at a position of a file that
 *       already exists;
 *   <li><b>empty</b> ({@link #canWriteEmpty}): reserving the space first and filling in the pixels
 *       later with {@link #replacePixels}. It is how an enormous image that does not fit in memory
 *       is written.
 * </ul>
 *
 * <h2>{@link #getOutput} usually needs an {@link ImageOutputStream}</h2>
 *
 * <p>As when reading: it accepts {@link Object} to leave room for specialized writers, but normally
 * it only accepts {@code ImageOutputStream}. {@code ImageIO.createImageOutputStream} is what wraps
 * it.
 *
 * <h2>It implements {@link ImageTranscoder}</h2>
 *
 * <p>That is why {@link #convertStreamMetadata} and {@link #convertImageMetadata} are abstract: a
 * writer <b>has</b> to know how to translate metadata from another format into its own, because
 * that is what happens every time someone converts an image and wants to keep what it had.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The class is complete; what is missing is <b>subclasses</b>. Encoding PNG or JPEG takes the
 * encoders. With a writer registered as a service, all of this works unchanged.
 */
public abstract class ImageWriter implements ImageTranscoder {

    /** Who created it, or null. */
    protected ImageWriterSpi originatingProvider;

    /** Where it writes to, or null. */
    protected Object output = null;

    /** Which locales it can give its messages in, or null. */
    protected Locale[] availableLocales = null;

    /** Which one it gives them in, or null. */
    protected Locale locale = null;

    /** The warning listeners, or null. */
    protected List<IIOWriteWarningListener> warningListeners = null;

    /** Each one's locale when it was registered. */
    protected List<Locale> warningLocales = null;

    /** The progress listeners, or null. */
    protected List<IIOWriteProgressListener> progressListeners = null;

    /** Whether someone asked to cancel. */
    private boolean abortFlag = false;

    /** For the subclasses. */
    protected ImageWriter(ImageWriterSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    /** Who created it, or null. */
    public ImageWriterSpi getOriginatingProvider() {
        return this.originatingProvider;
    }

    /**
     * Where to write.
     *
     * @param output typically an {@link ImageOutputStream}; null disconnects it
     * @throws IllegalArgumentException if that type of output is not supported
     */
    public void setOutput(Object output) {
        if (output != null) {
            boolean found = false;
            if (this.originatingProvider != null) {
                Class<?>[] classes = this.originatingProvider.getOutputTypes();
                int i = 0;
                while (i < classes.length) {
                    if (classes[i].isInstance(output)) {
                        found = true;
                    }
                    i = i + 1;
                }
            } else if (output instanceof ImageOutputStream) {
                found = true;
            }
            if (!found) {
                throw new IllegalArgumentException("Illegal output type!");
            }
        }
        this.output = output;
    }

    /** Where it writes to, or null. */
    public Object getOutput() {
        return this.output;
    }

    /** Which locales it can give its messages in; a copy, or null. */
    public Locale[] getAvailableLocales() {
        if (this.availableLocales == null) {
            return null;
        }
        Locale[] copy = new Locale[this.availableLocales.length];
        System.arraycopy(this.availableLocales, 0, copy, 0, this.availableLocales.length);
        return copy;
    }

    /**
     * Which one to give them in; null goes back to the system's.
     *
     * @throws IllegalArgumentException if it is not one of the available ones
     */
    public void setLocale(Locale locale) {
        if (locale != null) {
            Locale[] locales = getAvailableLocales();
            boolean found = false;
            if (locales != null) {
                int i = 0;
                while (i < locales.length) {
                    if (locale.equals(locales[i])) {
                        found = true;
                    }
                    i = i + 1;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Invalid locale!");
            }
        }
        this.locale = locale;
    }

    /** Which one it gives them in, or null. */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * An empty parameter object, of the type this writer understands.
     *
     * <p>A subclass with parameters of its own redefines it.
     */
    public ImageWriteParam getDefaultWriteParam() {
        return new ImageWriteParam(getLocale());
    }

    /** The default stream metadata for those parameters, or null if it carries none. */
    public abstract IIOMetadata getDefaultStreamMetadata(ImageWriteParam param);

    /** Those of an image of that type. */
    public abstract IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
                                                        ImageWriteParam param);

    /** Translates stream metadata from another format. See the class note. */
    public abstract IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param);

    /** Same, for an image. */
    public abstract IIOMetadata convertImageMetadata(IIOMetadata inData,
                                                     ImageTypeSpecifier imageType,
                                                     ImageWriteParam param);

    /**
     * How many thumbnails it can embed; 0 if none, -1 if not known yet.
     *
     * <p>The four arguments exist because the answer may depend on all of them: some formats only
     * allow a thumbnail in certain compression modes.
     */
    public int getNumThumbnailsSupported(ImageTypeSpecifier imageType, ImageWriteParam param,
                                         IIOMetadata streamMetadata, IIOMetadata imageMetadata) {
        return 0;
    }

    /**
     * Which thumbnail sizes it prefers, in min/max pairs; null if it has no opinion.
     *
     * <p>This implementation always returns null. (An earlier note documented an
     * {@code IllegalArgumentException}; nothing here throws it.)
     */
    public Dimension[] getPreferredThumbnailSizes(ImageTypeSpecifier imageType,
                                                  ImageWriteParam param,
                                                  IIOMetadata streamMetadata,
                                                  IIOMetadata imageMetadata) {
        return null;
    }

    /**
     * Whether it can write raw pixels without a colour model. See {@link
     * ImageReader#canReadRaster}.
     */
    public boolean canWriteRasters() {
        return false;
    }

    /**
     * Writes an image with its metadata.
     *
     * <p>It is the method that does the work, and the only one a subclass <b>has</b> to write to
     * encode.
     *
     * @throws IllegalStateException if there is no output
     * @throws UnsupportedOperationException if the image carries a raster and it cannot write them
     * @throws IOException if writing failed
     */
    public abstract void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param)
        throws IOException;

    /**
     * Same, without stream metadata or parameters.
     *
     * @throws IOException if writing failed
     */
    public void write(IIOImage image) throws IOException {
        write(null, image, null);
    }

    /**
     * Same, from a bare image.
     *
     * @throws IOException if writing failed
     */
    public void write(RenderedImage image) throws IOException {
        write(null, new IIOImage(image, null, null), null);
    }

    /** Whether it can write several images in sequence. See the class note. */
    public boolean canWriteSequence() {
        return false;
    }

    /**
     * Opens a sequence.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void prepareWriteSequence(IIOMetadata streamMetadata) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Adds an image to the open sequence.
     *
     * @throws IllegalStateException if there is no open sequence
     * @throws IOException if writing failed
     */
    public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Closes it.
     *
     * @throws IllegalStateException if there is no open sequence
     * @throws IOException if writing failed
     */
    public void endWriteSequence() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Whether it can replace the stream metadata of an already written file.
     *
     * @throws IOException if reading the output failed
     */
    public boolean canReplaceStreamMetadata() throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Replaces it.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void replaceStreamMetadata(IIOMetadata streamMetadata) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Whether it can replace that image's metadata.
     *
     * @throws IOException if reading the output failed
     */
    public boolean canReplaceImageMetadata(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Replaces it.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void replaceImageMetadata(int imageIndex, IIOMetadata imageMetadata)
        throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Whether it can insert an image at that position. See the class note.
     *
     * @param imageIndex where; -1 means at the end
     * @throws IOException if reading the output failed
     */
    public boolean canInsertImage(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Inserts it.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void writeInsert(int imageIndex, IIOImage image, ImageWriteParam param)
        throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Whether it can remove an image from the file.
     *
     * @throws IOException if reading the output failed
     */
    public boolean canRemoveImage(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Removes it.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void removeImage(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Whether it can reserve an empty image to fill in later. See the class note.
     *
     * @throws IOException if reading the output failed
     */
    public boolean canWriteEmpty() throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Reserves it.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void prepareWriteEmpty(IIOMetadata streamMetadata, ImageTypeSpecifier imageType,
                                  int width, int height, IIOMetadata imageMetadata,
                                  List<? extends BufferedImage> thumbnails,
                                  ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Closes the empty image.
     *
     * @throws IllegalStateException if none is open
     * @throws IOException if writing failed
     */
    public void endWriteEmpty() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Whether it can insert an empty image at that position.
     *
     * @throws IOException if reading the output failed
     */
    public boolean canInsertEmpty(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Inserts it.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void prepareInsertEmpty(int imageIndex, ImageTypeSpecifier imageType, int width,
                                   int height, IIOMetadata imageMetadata,
                                   List<? extends BufferedImage> thumbnails,
                                   ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Closes it.
     *
     * @throws IllegalStateException if none is open
     * @throws IOException if writing failed
     */
    public void endInsertEmpty() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Whether it can rewrite pixels of an already written image.
     *
     * @throws IOException if reading the output failed
     */
    public boolean canReplacePixels(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Opens a region for rewriting.
     *
     * @param region which rectangle, or null for the whole image
     * @throws UnsupportedOperationException if it cannot
     * @throws IOException if writing failed
     */
    public void prepareReplacePixels(int imageIndex, Rectangle region) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Rewrites those pixels.
     *
     * @throws IllegalStateException if there is no open region
     * @throws IOException if writing failed
     */
    public void replacePixels(RenderedImage image, ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Same, from raw pixels.
     *
     * @throws IOException if writing failed
     */
    public void replacePixels(Raster raster, ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Closes the region.
     *
     * @throws IllegalStateException if none is open
     * @throws IOException if writing failed
     */
    public void endReplacePixels() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /** Asks to cancel. Called from another thread; see {@link ImageReader#abort}. */
    public synchronized void abort() {
        this.abortFlag = true;
    }

    /** Whether someone asked to cancel. */
    protected synchronized boolean abortRequested() {
        return this.abortFlag;
    }

    /** Clears the request. The subclass calls it when starting each operation. */
    protected synchronized void clearAbortRequest() {
        this.abortFlag = false;
    }

    /** Registers a warning listener; null does nothing. */
    public void addIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (listener == null) {
            return;
        }
        if (this.warningListeners == null) {
            this.warningListeners = new ArrayList<IIOWriteWarningListener>();
            this.warningLocales = new ArrayList<Locale>();
        }
        this.warningListeners.add(listener);
        // The locale is saved at registration; see ImageReader.
        this.warningLocales.add(getLocale());
    }

    /** Unregisters it. */
    public void removeIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (listener == null || this.warningListeners == null) {
            return;
        }
        int at = this.warningListeners.indexOf(listener);
        if (at >= 0) {
            this.warningListeners.remove(at);
            this.warningLocales.remove(at);
            if (this.warningListeners.isEmpty()) {
                this.warningListeners = null;
                this.warningLocales = null;
            }
        }
    }

    /** Unregisters them all. */
    public void removeAllIIOWriteWarningListeners() {
        this.warningListeners = null;
        this.warningLocales = null;
    }

    /** Registers a progress listener. */
    public void addIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (listener == null) {
            return;
        }
        if (this.progressListeners == null) {
            this.progressListeners = new ArrayList<IIOWriteProgressListener>();
        }
        this.progressListeners.add(listener);
    }

    /** Unregisters it. */
    public void removeIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (listener == null || this.progressListeners == null) {
            return;
        }
        this.progressListeners.remove(listener);
        if (this.progressListeners.isEmpty()) {
            this.progressListeners = null;
        }
    }

    /** Unregisters them all. */
    public void removeAllIIOWriteProgressListeners() {
        this.progressListeners = null;
    }

    /** Reports that an image begins. */
    protected void processImageStarted(int imageIndex) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).imageStarted(this, imageIndex);
            i = i + 1;
        }
    }

    /** Reports the progress. */
    protected void processImageProgress(float percentageDone) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).imageProgress(this, percentageDone);
            i = i + 1;
        }
    }

    /** Reports that it finished. */
    protected void processImageComplete() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).imageComplete(this);
            i = i + 1;
        }
    }

    /** Reports that a thumbnail begins. */
    protected void processThumbnailStarted(int imageIndex, int thumbnailIndex) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).thumbnailStarted(this, imageIndex, thumbnailIndex);
            i = i + 1;
        }
    }

    /** Reports the thumbnail's progress. */
    protected void processThumbnailProgress(float percentageDone) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).thumbnailProgress(this, percentageDone);
            i = i + 1;
        }
    }

    /** Reports that it finished. */
    protected void processThumbnailComplete() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).thumbnailComplete(this);
            i = i + 1;
        }
    }

    /**
     * Reports that it was cut short. See {@link
     * javax.imageio.event.IIOWriteProgressListener#writeAborted}.
     */
    protected void processWriteAborted() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).writeAborted(this);
            i = i + 1;
        }
    }

    /** Reports a warning. */
    protected void processWarningOccurred(int imageIndex, String warning) {
        if (this.warningListeners == null) {
            return;
        }
        if (warning == null) {
            throw new IllegalArgumentException("warning == null!");
        }
        int i = 0;
        while (i < this.warningListeners.size()) {
            this.warningListeners.get(i).warningOccurred(this, imageIndex, warning);
            i = i + 1;
        }
    }

    /**
     * Same, with the text taken from a resource bundle.
     *
     * <p>Each listener gets the message in the locale it was registered with; see
     * {@link ImageReader}.
     *
     * @throws IllegalArgumentException if the bundle or the key are null, or if the key is missing
     */
    protected void processWarningOccurred(int imageIndex, String baseName, String keyword) {
        if (this.warningListeners == null) {
            return;
        }
        if (baseName == null) {
            throw new IllegalArgumentException("baseName == null!");
        }
        if (keyword == null) {
            throw new IllegalArgumentException("keyword == null!");
        }
        int i = 0;
        while (i < this.warningListeners.size()) {
            IIOWriteWarningListener listener = this.warningListeners.get(i);
            Locale where = this.warningLocales.get(i);
            if (where == null) {
                where = Locale.getDefault();
            }
            String warning;
            try {
                java.util.ResourceBundle bundle =
                    java.util.ResourceBundle.getBundle(baseName, where,
                                                       getClass().getClassLoader());
                warning = bundle.getString(keyword);
            } catch (java.util.MissingResourceException e) {
                throw new IllegalArgumentException("Bundle not found!");
            }
            listener.warningOccurred(this, imageIndex, warning);
            i = i + 1;
        }
    }

    /** Back to the initial state. See {@link ImageReader#reset}. */
    public void reset() {
        setOutput(null);
        setLocale(null);
        removeAllIIOWriteWarningListeners();
        removeAllIIOWriteProgressListeners();
        clearAbortRequest();
    }

    /** Releases whatever it holds. After this it can no longer be used. */
    public void dispose() {
    }
}
