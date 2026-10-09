package javax.imageio;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * KajiLibrary's javax.imageio.ImageReader -- decodes images of one format.
 *
 * <p>What whoever adds support for reading a format implements. A concrete subclass has to supply
 * seven methods --{@link #getNumImages}, {@link #getWidth}, {@link #getHeight},
 * {@link #getImageTypes}, {@link #getStreamMetadata}, {@link #getImageMetadata} and
 * {@link #read(int, ImageReadParam)}-- and inherits everything else. (An earlier note said six.)
 *
 * <h2>A file may hold several images</h2>
 *
 * <p>That is why almost every method takes an index. TIFF, animated GIF and ICO carry several; PNG
 * and JPEG carry one and the index is always 0.
 *
 * <h2>{@code seekForwardOnly} is a promise that pays off</h2>
 *
 * <p>{@link #setInput(Object, boolean)} with true promises that the images will be read <b>in order
 * and without going back</b>. In exchange, the reader may drop what has passed, which is why a
 * one-gigabyte TIFF can be read from a socket.
 *
 * <p>The price: after reading image 3, asking for 1 throws {@link IndexOutOfBoundsException}.
 * {@link #getMinIndex} says how far back you can still go.
 *
 * <h2>So is {@code ignoreMetadata}</h2>
 *
 * <p>Promising not to ask for the metadata lets the reader skip whole blocks of the file. In a JPEG
 * with Exif and an embedded thumbnail that is half the work.
 *
 * <h2>The input almost always has to be an {@link ImageInputStream}</h2>
 *
 * <p>{@link #setInput} accepts an {@link Object} because a specialized reader may accept something
 * else, but normally it only accepts {@code ImageInputStream}. Passing it a {@code File} directly
 * throws {@link IllegalArgumentException}; {@code ImageIO.createImageInputStream} is what wraps
 * it.
 *
 * <h2>{@link #abort} is called from another thread</h2>
 *
 * <p>It is the only part of the class designed for concurrency: {@code read} blocks, so cancelling
 * can only come from outside. A subclass has to check {@link #abortRequested} <b>often</b> while
 * decoding, and call {@link #processReadAborted} when it stops.
 *
 * <p>And it has to call {@link #clearAbortRequest} when starting each operation: otherwise an old
 * cancellation aborts the next read.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The class is complete. What this library does not ship is <b>subclasses</b>: decoding PNG,
 * JPEG or GIF takes the decoders, and that is another project. With a reader registered as a
 * service, all of this works unchanged.
 */
public abstract class ImageReader {

    /** Who created it, or null. */
    protected ImageReaderSpi originatingProvider;

    /** Where it reads from, or null. */
    protected Object input = null;

    /** Whether not going back was promised. See the class note. */
    protected boolean seekForwardOnly = false;

    /** Whether not asking for metadata was promised. */
    protected boolean ignoreMetadata = false;

    /** The lowest image that can still be asked for. */
    protected int minIndex = 0;

    /** Which locales it can give its messages in, or null. */
    protected Locale[] availableLocales = null;

    /** Which one it gives them in, or null for the system's. */
    protected Locale locale = null;

    /** The warning listeners, or null. */
    protected List<IIOReadWarningListener> warningListeners = null;

    /** Each one's locale when it was registered. */
    protected List<Locale> warningLocales = null;

    /** The progress listeners, or null. */
    protected List<IIOReadProgressListener> progressListeners = null;

    /** The partial-image listeners, or null. */
    protected List<IIOReadUpdateListener> updateListeners = null;

    /** Whether someone asked to cancel. */
    private boolean abortFlag = false;

    /** For the subclasses. */
    protected ImageReader(ImageReaderSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    /**
     * What the format it reads is called.
     *
     * @throws IOException if it cannot be found out
     */
    public String getFormatName() throws IOException {
        return this.originatingProvider.getFormatNames()[0];
    }

    /** Who created it, or null if it was instantiated by hand. */
    public ImageReaderSpi getOriginatingProvider() {
        return this.originatingProvider;
    }

    /**
     * Where to read from, with both promises. See the class note.
     *
     * @param input typically an {@link ImageInputStream}; null disconnects it
     * @throws IllegalArgumentException if that type of input is not supported
     */
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        if (input != null) {
            boolean found = false;
            if (this.originatingProvider != null) {
                Class<?>[] classes = this.originatingProvider.getInputTypes();
                int i = 0;
                while (i < classes.length) {
                    if (classes[i].isInstance(input)) {
                        found = true;
                    }
                    i = i + 1;
                }
            } else if (input instanceof ImageInputStream) {
                found = true;
            }
            if (!found) {
                throw new IllegalArgumentException("Incorrect input type!");
            }
            this.seekForwardOnly = seekForwardOnly;
            this.ignoreMetadata = ignoreMetadata;
            this.minIndex = 0;
        }
        this.input = input;
    }

    /** Same, promising nothing about the metadata. */
    public void setInput(Object input, boolean seekForwardOnly) {
        setInput(input, seekForwardOnly, false);
    }

    /** Same, promising nothing. */
    public void setInput(Object input) {
        setInput(input, false, false);
    }

    /** Where it reads from, or null. */
    public Object getInput() {
        return this.input;
    }

    /** Whether not going back was promised. */
    public boolean isSeekForwardOnly() {
        return this.seekForwardOnly;
    }

    /** Whether not asking for metadata was promised. */
    public boolean isIgnoringMetadata() {
        return this.ignoreMetadata;
    }

    /** The lowest image that can still be asked for. See the class note. */
    public int getMinIndex() {
        return this.minIndex;
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
     * How many images there are.
     *
     * @param allowSearch whether walking the file to count them is allowed. With false, a reader
     *     that does not know beforehand returns -1 instead of taking its time
     * @throws IllegalStateException if there is no input
     * @throws IllegalStateException if searching is asked for and not going back was promised
     * @throws IOException if reading failed
     */
    public abstract int getNumImages(boolean allowSearch) throws IOException;

    /**
     * The width of that image.
     *
     * @throws IllegalStateException if there is no input
     * @throws IndexOutOfBoundsException if that image does not exist or was left behind
     * @throws IOException if reading failed
     */
    public abstract int getWidth(int imageIndex) throws IOException;

    /**
     * The height.
     *
     * @throws IOException if reading failed
     */
    public abstract int getHeight(int imageIndex) throws IOException;

    /**
     * Whether reading loose pieces of that image is cheap.
     *
     * <p>False by default, which is the conservative answer: whoever asks will read it whole
     * instead of in parts, and that always works.
     *
     * @throws IOException if reading failed
     */
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        return false;
    }

    /**
     * The ratio between width and height <b>as it has to be displayed</b>.
     *
     * <p>By default it is the width divided by the height, which assumes square pixels. A format
     * with non-square pixels --old video, some TIFFs-- redefines this, and then the number does not
     * match the division.
     *
     * @throws IOException if reading failed
     */
    public float getAspectRatio(int imageIndex) throws IOException {
        return (float) getWidth(imageIndex) / getHeight(imageIndex);
    }

    /**
     * What type the pixels are as they sit in the file, or null if that does not apply.
     *
     * <p>By default, the first of {@link #getImageTypes}. It serves to decode without converting,
     * which is the fastest and the only lossless way.
     *
     * @throws IOException if reading failed
     */
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        return getImageTypes(imageIndex).next();
    }

    /**
     * Which types it can deliver that image as, the most natural first.
     *
     * @throws IOException if reading failed
     */
    public abstract Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException;

    /**
     * An empty parameter object, of the type this reader understands.
     *
     * <p>A subclass with parameters of its own redefines it to return its own.
     */
    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    /**
     * The metadata of the whole file, or null if there is none.
     *
     * @throws IOException if reading failed
     */
    public abstract IIOMetadata getStreamMetadata() throws IOException;

    /**
     * Same, in a specific format and asking only for those nodes.
     *
     * <p>The set of nodes allows reading a large tree without building it whole.
     *
     * @throws IllegalArgumentException if the format is not one this reader understands
     * @throws IOException if reading failed
     */
    public IIOMetadata getStreamMetadata(String formatName, Set<String> nodeNames)
        throws IOException {
        return getMetadata(getStreamMetadata(), formatName, nodeNames);
    }

    /**
     * The metadata of that image, or null.
     *
     * @throws IOException if reading failed
     */
    public abstract IIOMetadata getImageMetadata(int imageIndex) throws IOException;

    /**
     * Same, in a specific format.
     *
     * @throws IOException if reading failed
     */
    public IIOMetadata getImageMetadata(int imageIndex, String formatName, Set<String> nodeNames)
        throws IOException {
        return getMetadata(getImageMetadata(imageIndex), formatName, nodeNames);
    }

    /**
     * That image, with the default parameters.
     *
     * @throws IOException if reading failed
     */
    public BufferedImage read(int imageIndex) throws IOException {
        return read(imageIndex, null);
    }

    /**
     * That image.
     *
     * <p>It is the method that does the work, and the only one a subclass <b>has</b> to write to
     * decode.
     *
     * @param param which part and how, or null for everything
     * @throws IllegalStateException if there is no input
     * @throws IndexOutOfBoundsException if that image does not exist or was left behind
     * @throws IOException if reading failed
     */
    public abstract BufferedImage read(int imageIndex, ImageReadParam param) throws IOException;

    /**
     * That image with its thumbnails and its metadata.
     *
     * @throws IOException if reading failed
     */
    public IIOImage readAll(int imageIndex, ImageReadParam param) throws IOException {
        if (imageIndex < getMinIndex()) {
            throw new IndexOutOfBoundsException("imageIndex < getMinIndex()!");
        }
        BufferedImage im = read(imageIndex, param);
        ArrayList<BufferedImage> thumbnails = null;
        int numThumbnails = getNumThumbnails(imageIndex);
        if (numThumbnails > 0) {
            thumbnails = new ArrayList<BufferedImage>();
            int j = 0;
            while (j < numThumbnails) {
                thumbnails.add(readThumbnail(imageIndex, j));
                j = j + 1;
            }
        }
        IIOMetadata metadata = getImageMetadata(imageIndex);
        return new IIOImage(im, thumbnails, metadata);
    }

    /**
     * All the images, one at a time.
     *
     * <p>The iterator it returns is <b>lazy</b>: each {@code next()} decodes the following image.
     * It is what allows walking a hundred-page TIFF without having them all in memory.
     *
     * @param params one parameter per image; null uses the defaults for all
     * @throws IOException if reading failed
     */
    public Iterator<IIOImage> readAll(Iterator<? extends ImageReadParam> params)
        throws IOException {
        List<IIOImage> output = new ArrayList<IIOImage>();
        int imageIndex = getMinIndex();
        processSequenceStarted(imageIndex);
        while (true) {
            ImageReadParam param = null;
            if (params != null && params.hasNext()) {
                Object o = params.next();
                if (o != null && !(o instanceof ImageReadParam)) {
                    throw new IllegalArgumentException("Non-ImageReadParam supplied as part of params!");
                }
                param = (ImageReadParam) o;
            }
            BufferedImage bi;
            try {
                bi = read(imageIndex, param);
            } catch (IndexOutOfBoundsException e) {
                // There are no more images. It is how the walk ends for a format that does not say
                // beforehand how many it has.
                break;
            }
            ArrayList<BufferedImage> thumbnails = null;
            int numThumbnails = getNumThumbnails(imageIndex);
            if (numThumbnails > 0) {
                thumbnails = new ArrayList<BufferedImage>();
                int j = 0;
                while (j < numThumbnails) {
                    thumbnails.add(readThumbnail(imageIndex, j));
                    j = j + 1;
                }
            }
            output.add(new IIOImage(bi, thumbnails, getImageMetadata(imageIndex)));
            imageIndex = imageIndex + 1;
        }
        processSequenceComplete();
        return output.iterator();
    }

    /**
     * Whether it can deliver raw pixels without a colour model.
     *
     * <p>False by default. See {@link #readRaster}.
     */
    public boolean canReadRaster() {
        return false;
    }

    /**
     * The raw pixels, uninterpreted.
     *
     * <p>It exists for formats whose data <b>are not colours</b>: a medical image, a satellite
     * band. Forcing a colour model there would be making things up; see {@link IIOImage}.
     *
     * @throws UnsupportedOperationException if this reader cannot
     * @throws IOException if reading failed
     */
    public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
        throw new UnsupportedOperationException("readRaster not supported!");
    }

    /**
     * Whether that image is split into tiles.
     *
     * @throws IOException if reading failed
     */
    public boolean isImageTiled(int imageIndex) throws IOException {
        return false;
    }

    /**
     * The tile width; the image's if it is not tiled.
     *
     * @throws IOException if reading failed
     */
    public int getTileWidth(int imageIndex) throws IOException {
        return getWidth(imageIndex);
    }

    /**
     * The tile height.
     *
     * @throws IOException if reading failed
     */
    public int getTileHeight(int imageIndex) throws IOException {
        return getHeight(imageIndex);
    }

    /**
     * The tile grid offset in X.
     *
     * @throws IOException if reading failed
     */
    public int getTileGridXOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * Same in Y.
     *
     * @throws IOException if reading failed
     */
    public int getTileGridYOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * A tile.
     *
     * <p>By default, asking for tile (0,0) reads the whole image. Any other one throws, because it
     * does not exist.
     *
     * @throws IllegalArgumentException if that tile does not exist
     * @throws IOException if reading failed
     */
    public BufferedImage readTile(int imageIndex, int tileX, int tileY) throws IOException {
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return read(imageIndex);
    }

    /**
     * A tile as raw pixels.
     *
     * @throws UnsupportedOperationException if this reader cannot read rasters
     * @throws IOException if reading failed
     */
    public Raster readTileRaster(int imageIndex, int tileX, int tileY) throws IOException {
        if (!canReadRaster()) {
            throw new UnsupportedOperationException("readTileRaster not supported!");
        }
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return readRaster(imageIndex, null);
    }

    /**
     * That image as a {@link RenderedImage}.
     *
     * <p>A reader that can decode by tiles may return something <b>lazy</b>, which decodes each
     * tile when asked for it. This implementation decodes everything at once.
     *
     * @throws IOException if reading failed
     */
    public RenderedImage readAsRenderedImage(int imageIndex, ImageReadParam param)
        throws IOException {
        return read(imageIndex, param);
    }

    /** Whether this reader can deliver the embedded thumbnails. */
    public boolean readerSupportsThumbnails() {
        return false;
    }

    /**
     * Whether that image has thumbnails.
     *
     * @throws IOException if reading failed
     */
    public boolean hasThumbnails(int imageIndex) throws IOException {
        return getNumThumbnails(imageIndex) > 0;
    }

    /**
     * How many.
     *
     * @throws IOException if reading failed
     */
    public int getNumThumbnails(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * The width of a thumbnail.
     *
     * @throws UnsupportedOperationException if this reader does not handle thumbnails
     * @throws IOException if reading failed
     */
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getWidth();
    }

    /**
     * The height.
     *
     * @throws IOException if reading failed
     */
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getHeight();
    }

    /**
     * A thumbnail.
     *
     * @throws UnsupportedOperationException if this reader does not handle them
     * @throws IOException if reading failed
     */
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        throw new UnsupportedOperationException("Thumbnails not supported!");
    }

    /** Asks to cancel. Called from another thread; see the class note. */
    public synchronized void abort() {
        this.abortFlag = true;
    }

    /** Whether someone asked to cancel. The subclass checks it often. */
    protected synchronized boolean abortRequested() {
        return this.abortFlag;
    }

    /**
     * Clears the request. The subclass calls it when starting each operation; see the class note.
     */
    protected synchronized void clearAbortRequest() {
        this.abortFlag = false;
    }

    /** Registers a warning listener; null does nothing. */
    public void addIIOReadWarningListener(IIOReadWarningListener listener) {
        if (listener == null) {
            return;
        }
        if (this.warningListeners == null) {
            this.warningListeners = new ArrayList<IIOReadWarningListener>();
            this.warningLocales = new ArrayList<Locale>();
        }
        this.warningListeners.add(listener);
        // The locale is saved at registration: a listener registered in French must keep
        // receiving French even if the reader changes locale later.
        this.warningLocales.add(getLocale());
    }

    /** Unregisters it. */
    public void removeIIOReadWarningListener(IIOReadWarningListener listener) {
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
    public void removeAllIIOReadWarningListeners() {
        this.warningListeners = null;
        this.warningLocales = null;
    }

    /** Registers a progress listener. */
    public void addIIOReadProgressListener(IIOReadProgressListener listener) {
        if (listener == null) {
            return;
        }
        if (this.progressListeners == null) {
            this.progressListeners = new ArrayList<IIOReadProgressListener>();
        }
        this.progressListeners.add(listener);
    }

    /** Unregisters it. */
    public void removeIIOReadProgressListener(IIOReadProgressListener listener) {
        if (listener == null || this.progressListeners == null) {
            return;
        }
        this.progressListeners.remove(listener);
        if (this.progressListeners.isEmpty()) {
            this.progressListeners = null;
        }
    }

    /** Unregisters them all. */
    public void removeAllIIOReadProgressListeners() {
        this.progressListeners = null;
    }

    /** Registers a partial-image listener. */
    public void addIIOReadUpdateListener(IIOReadUpdateListener listener) {
        if (listener == null) {
            return;
        }
        if (this.updateListeners == null) {
            this.updateListeners = new ArrayList<IIOReadUpdateListener>();
        }
        this.updateListeners.add(listener);
    }

    /** Unregisters it. */
    public void removeIIOReadUpdateListener(IIOReadUpdateListener listener) {
        if (listener == null || this.updateListeners == null) {
            return;
        }
        this.updateListeners.remove(listener);
        if (this.updateListeners.isEmpty()) {
            this.updateListeners = null;
        }
    }

    /** Unregisters them all. */
    public void removeAllIIOReadUpdateListeners() {
        this.updateListeners = null;
    }

    /** Reports that a sequence begins. */
    protected void processSequenceStarted(int minIndex) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).sequenceStarted(this, minIndex);
            i = i + 1;
        }
    }

    /** Reports that it finished. */
    protected void processSequenceComplete() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).sequenceComplete(this);
            i = i + 1;
        }
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

    /** Reports that the image finished. */
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

    /** Reports that it was cut short. The subclass calls it when serving an {@link #abort}. */
    protected void processReadAborted() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).readAborted(this);
            i = i + 1;
        }
    }

    /** Reports that a pass begins. */
    protected void processPassStarted(BufferedImage theImage, int pass, int minPass, int maxPass,
                                      int minX, int minY, int periodX, int periodY, int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).passStarted(this, theImage, pass, minPass, maxPass, minX,
                                                    minY, periodX, periodY, bands);
            i = i + 1;
        }
    }

    /** Reports that a piece of the image changed. */
    protected void processImageUpdate(BufferedImage theImage, int minX, int minY, int width,
                                      int height, int periodX, int periodY, int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).imageUpdate(this, theImage, minX, minY, width, height,
                                                    periodX, periodY, bands);
            i = i + 1;
        }
    }

    /** Reports that the pass finished. */
    protected void processPassComplete(BufferedImage theImage) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).passComplete(this, theImage);
            i = i + 1;
        }
    }

    /** Same, for a thumbnail. */
    protected void processThumbnailPassStarted(BufferedImage theThumbnail, int pass, int minPass,
                                               int maxPass, int minX, int minY, int periodX,
                                               int periodY, int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).thumbnailPassStarted(this, theThumbnail, pass, minPass,
                                                             maxPass, minX, minY, periodX,
                                                             periodY, bands);
            i = i + 1;
        }
    }

    /** Same. */
    protected void processThumbnailUpdate(BufferedImage theThumbnail, int minX, int minY,
                                          int width, int height, int periodX, int periodY,
                                          int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).thumbnailUpdate(this, theThumbnail, minX, minY, width,
                                                        height, periodX, periodY, bands);
            i = i + 1;
        }
    }

    /** Same. */
    protected void processThumbnailPassComplete(BufferedImage theThumbnail) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).thumbnailPassComplete(this, theThumbnail);
            i = i + 1;
        }
    }

    /** Reports a warning. */
    protected void processWarningOccurred(String warning) {
        if (this.warningListeners == null) {
            return;
        }
        if (warning == null) {
            throw new IllegalArgumentException("warning == null!");
        }
        int i = 0;
        while (i < this.warningListeners.size()) {
            this.warningListeners.get(i).warningOccurred(this, warning);
            i = i + 1;
        }
    }

    /**
     * Same, with the text taken from a resource bundle.
     *
     * <p>Each listener gets the message <b>in the locale it was registered with</b>, not the one
     * the reader has set now; see {@link #addIIOReadWarningListener}.
     *
     * @throws IllegalArgumentException if the bundle or the key are null, or if the key is missing
     */
    protected void processWarningOccurred(String baseName, String keyword) {
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
            IIOReadWarningListener listener = this.warningListeners.get(i);
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
            listener.warningOccurred(this, warning);
            i = i + 1;
        }
    }

    /**
     * Back to the initial state: no input, no listeners, no locale.
     *
     * <p>It is what allows reusing a reader with another file. It does not release native
     * resources; that is what {@link #dispose} is for.
     */
    public void reset() {
        setInput(null, false, false);
        setLocale(null);
        removeAllIIOReadUpdateListeners();
        removeAllIIOReadWarningListeners();
        removeAllIIOReadProgressListeners();
        clearAbortRequest();
    }

    /**
     * Releases whatever the reader holds.
     *
     * <p>After this the reader <b>can no longer be used</b>, unlike {@link #reset}. This
     * implementation does nothing: only a subclass with resources of its own needs something here.
     */
    public void dispose() {
    }

    /**
     * Which region of the source has to be read, already clipped to the real size.
     *
     * <p>It is the computation every subclass needs when it starts decoding, and it is here so that
     * each does not repeat it --and so that none forgets to clip against the real size, which is
     * how you end up reading outside the file.
     *
     * @param param the parameters, or null for the whole image
     */
    protected static Rectangle getSourceRegion(ImageReadParam param, int srcWidth,
                                               int srcHeight) {
        Rectangle sourceRegion = new Rectangle(0, 0, srcWidth, srcHeight);
        if (param != null) {
            Rectangle region = param.getSourceRegion();
            if (region != null) {
                sourceRegion = sourceRegion.intersection(region);
            }
            int subsampleXOffset = param.getSubsamplingXOffset();
            int subsampleYOffset = param.getSubsamplingYOffset();
            sourceRegion.x = sourceRegion.x + subsampleXOffset;
            sourceRegion.y = sourceRegion.y + subsampleYOffset;
            sourceRegion.width = sourceRegion.width - subsampleXOffset;
            sourceRegion.height = sourceRegion.height - subsampleYOffset;
        }
        return sourceRegion;
    }

    /**
     * Computes what is read and where it is written, taking crop, subsampling and offset into
     * account.
     *
     * <p>It fills in the two rectangles passed to it. It is the counterpart of
     * {@link #getSourceRegion} on the destination side, and does the part most often got wrong:
     * clipping against the destination image's size too.
     *
     * @throws IllegalArgumentException if either rectangle is null, or if not a single pixel is
     *     left to read
     */
    protected static void computeRegions(ImageReadParam param, int srcWidth, int srcHeight,
                                         BufferedImage image, Rectangle srcRegion,
                                         Rectangle destRegion) {
        if (srcRegion == null) {
            throw new IllegalArgumentException("srcRegion == null!");
        }
        if (destRegion == null) {
            throw new IllegalArgumentException("destRegion == null!");
        }
        srcRegion.setBounds(getSourceRegion(param, srcWidth, srcHeight));
        int periodX = 1;
        int periodY = 1;
        int gridX = 0;
        int gridY = 0;
        if (param != null) {
            periodX = param.getSourceXSubsampling();
            periodY = param.getSourceYSubsampling();
            java.awt.Point p = param.getDestinationOffset();
            gridX = p.x;
            gridY = p.y;
        }
        destRegion.setBounds(gridX, gridY,
                             (srcRegion.width + periodX - 1) / periodX,
                             (srcRegion.height + periodY - 1) / periodY);
        if (gridX < 0) {
            // A negative offset discards pixels on the left of what was read; the source region
            // has to advance so that what remains matches.
            int delta = -gridX * periodX;
            srcRegion.x = srcRegion.x + delta;
            srcRegion.width = srcRegion.width - delta;
            destRegion.x = 0;
            destRegion.width = destRegion.width + gridX;
        }
        if (gridY < 0) {
            int delta = -gridY * periodY;
            srcRegion.y = srcRegion.y + delta;
            srcRegion.height = srcRegion.height - delta;
            destRegion.y = 0;
            destRegion.height = destRegion.height + gridY;
        }
        if (image != null) {
            Rectangle destBounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            destRegion.setBounds(destRegion.intersection(destBounds));
        }
        if (destRegion.isEmpty()) {
            throw new IllegalArgumentException("Empty destination region!");
        }
        int deltaX = destRegion.x - gridX;
        if (deltaX > 0) {
            srcRegion.x = srcRegion.x + deltaX * periodX;
        }
        int deltaY = destRegion.y - gridY;
        if (deltaY > 0) {
            srcRegion.y = srcRegion.y + deltaY * periodY;
        }
        srcRegion.width = destRegion.width * periodX;
        srcRegion.height = destRegion.height * periodY;
        if (srcRegion.isEmpty()) {
            throw new IllegalArgumentException("Empty source region!");
        }
    }

    /**
     * That the requested bands exist on both sides and are the same number.
     *
     * @throws IllegalArgumentException if the counts do not match, or if a band does not exist (an
     *     earlier note said {@code IndexOutOfBoundsException} for the latter; the code and the JDK
     *     both throw {@code IllegalArgumentException})
     */
    protected static void checkReadParamBandSettings(ImageReadParam param, int numSrcBands,
                                                     int numDstBands) {
        int[] sourceBands = null;
        int[] destinationBands = null;
        if (param != null) {
            sourceBands = param.getSourceBands();
            destinationBands = param.getDestinationBands();
        }
        int paramSrcBandLength = 0;
        if (sourceBands != null) {
            paramSrcBandLength = sourceBands.length;
        }
        int paramDstBandLength = 0;
        if (destinationBands != null) {
            paramDstBandLength = destinationBands.length;
        }
        int virtualSrcBands = numSrcBands;
        if (paramSrcBandLength != 0) {
            virtualSrcBands = paramSrcBandLength;
        }
        int virtualDstBands = numDstBands;
        if (paramDstBandLength != 0) {
            virtualDstBands = paramDstBandLength;
        }
        if (virtualSrcBands != virtualDstBands) {
            throw new IllegalArgumentException("Number of source and destination bands differ!");
        }
        checkBandRange(sourceBands, numSrcBands, "Source band index out of bounds!");
        checkBandRange(destinationBands, numDstBands, "Destination band index out of bounds!");
    }

    /**
     * Gets the image to write into: the one given, or a new one of the right type.
     *
     * <p>It is what centralizes the choice between {@code setDestination} and
     * {@code setDestinationType}; see {@link ImageReadParam}.
     *
     * @param imageTypes the types the reader can deliver, the preferred first
     * @throws IIOException if the requested type is not among the ones the reader offers
     * @throws IllegalArgumentException if the types are null or empty, or if the size overflows
     */
    protected static BufferedImage getDestination(ImageReadParam param,
                                                  Iterator<ImageTypeSpecifier> imageTypes,
                                                  int width, int height) throws IIOException {
        if (imageTypes == null || !imageTypes.hasNext()) {
            throw new IllegalArgumentException("imageTypes null or empty!");
        }
        if ((long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("width*height > Integer.MAX_VALUE!");
        }
        ImageTypeSpecifier imageType = null;
        if (param != null) {
            BufferedImage dest = param.getDestination();
            if (dest != null) {
                return dest;
            }
            imageType = param.getDestinationType();
        }
        if (imageType == null) {
            imageType = imageTypes.next();
        } else {
            // The requested type has to be among the ones the reader can give: otherwise it would
            // be promising a conversion nobody is going to do.
            boolean foundIt = false;
            while (imageTypes.hasNext()) {
                ImageTypeSpecifier type = imageTypes.next();
                if (type.equals(imageType)) {
                    foundIt = true;
                }
            }
            if (!foundIt) {
                throw new IIOException("Destination type from ImageReadParam does not match!");
            }
        }
        Rectangle srcRegion = new Rectangle(0, 0, 0, 0);
        Rectangle destRegion = new Rectangle(0, 0, 0, 0);
        computeRegions(param, width, height, null, srcRegion, destRegion);
        int destWidth = destRegion.x + destRegion.width;
        int destHeight = destRegion.y + destRegion.height;
        if ((long) destWidth * destHeight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("destination width*height > Integer.MAX_VALUE!");
        }
        return imageType.createBufferedImage(destWidth, destHeight);
    }

    /** That every band of that array exists. */
    private static void checkBandRange(int[] bands, int numBands, String message) {
        if (bands == null) {
            return;
        }
        int i = 0;
        while (i < bands.length) {
            if (bands[i] >= numBands) {
                throw new IllegalArgumentException(message);
            }
            i = i + 1;
        }
    }

    /**
     * Filters some metadata by format and by nodes.
     *
     * <p>Shared by the two three-argument {@code getXxxMetadata}. Checking that the format is one
     * of the declared ones is what turns a misspelled name into an immediate error instead of an
     * empty tree.
     */
    private static IIOMetadata getMetadata(IIOMetadata metadata, String formatName,
                                           Set<String> nodeNames) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (nodeNames == null) {
            throw new IllegalArgumentException("nodeNames == null!");
        }
        if (metadata == null) {
            return null;
        }
        String[] formats = metadata.getMetadataFormatNames();
        boolean found = false;
        int i = 0;
        while (formats != null && i < formats.length) {
            if (formatName.equals(formats[i])) {
                found = true;
            }
            i = i + 1;
        }
        if (!found) {
            throw new IllegalArgumentException("Unsupported format name");
        }
        return metadata;
    }
}
