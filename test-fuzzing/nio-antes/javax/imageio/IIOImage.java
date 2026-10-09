package javax.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.List;
import javax.imageio.metadata.IIOMetadata;

/**
 * KajiLibrary's javax.imageio.IIOImage -- an image with its thumbnails and its metadata.
 *
 * <p>What is read or written in one go: the pixels, the previews the format carries embedded, and
 * the associated information.
 *
 * <h2>Image or raster, never both</h2>
 *
 * <p>It is the part that defines the class. It carries <b>either</b> a {@link RenderedImage}
 * <b>or</b> a {@link Raster}, and {@link #hasRaster} says which. Setting one sets the other to
 * null.
 *
 * <p>The difference is that an image knows how to interpret its pixels --it has a colour model--
 * and a raster is raw numbers. The raster exists for formats whose data <b>are not colours</b>: a
 * medical image in Hounsfield units, a satellite band in reflectance. Forcing a colour model there
 * would be making things up.
 *
 * <p>Asking for the one that is not there does not fail: it returns null.
 *
 * <h2>Thumbnails are not copied</h2>
 *
 * <p>The list is kept by reference, and {@link #getThumbnails} returns it as is. Modifying it
 * afterwards changes what the image holds. It is what the JDK does.
 */
public class IIOImage {

    /** The pixels as an image, or null if there is a raster. */
    protected RenderedImage image;

    /** The pixels as raw numbers, or null if there is an image. */
    protected Raster raster;

    /** The previews, or null. */
    protected List<? extends BufferedImage> thumbnails = null;

    /** The associated information, or null. */
    protected IIOMetadata metadata;

    /**
     * With an image.
     *
     * @param thumbnails the previews, or null; not copied
     * @param metadata the associated information, or null
     * @throws IllegalArgumentException if the image is null
     */
    public IIOImage(RenderedImage image, List<? extends BufferedImage> thumbnails,
                    IIOMetadata metadata) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        this.image = image;
        this.raster = null;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    /**
     * With a raster. See the class note about when that fits.
     *
     * @throws IllegalArgumentException if the raster is null
     */
    public IIOImage(Raster raster, List<? extends BufferedImage> thumbnails,
                    IIOMetadata metadata) {
        if (raster == null) {
            throw new IllegalArgumentException("raster == null!");
        }
        this.raster = raster;
        this.image = null;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    /** The image, or null if what it holds is a raster. */
    public RenderedImage getRenderedImage() {
        return this.image;
    }

    /** Sets it, and drops the raster. */
    public void setRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        this.image = image;
        this.raster = null;
    }

    /** Which of the two it carries. See the class note. */
    public boolean hasRaster() {
        return this.raster != null;
    }

    /** The raster, or null if what it holds is an image. */
    public Raster getRaster() {
        return this.raster;
    }

    /** Sets it, and drops the image. */
    public void setRaster(Raster raster) {
        if (raster == null) {
            throw new IllegalArgumentException("raster == null!");
        }
        this.raster = raster;
        this.image = null;
    }

    /** How many previews. */
    public int getNumThumbnails() {
        if (this.thumbnails == null) {
            return 0;
        }
        return this.thumbnails.size();
    }

    /**
     * A preview.
     *
     * @throws IndexOutOfBoundsException if it does not exist, or if there are none
     */
    public BufferedImage getThumbnail(int index) {
        if (this.thumbnails == null) {
            throw new IndexOutOfBoundsException("No thumbnails available!");
        }
        return this.thumbnails.get(index);
    }

    /** The previews, not copied. See the class note. */
    public List<? extends BufferedImage> getThumbnails() {
        return this.thumbnails;
    }

    /** Replaces them; does not copy either. */
    public void setThumbnails(List<? extends BufferedImage> thumbnails) {
        this.thumbnails = thumbnails;
    }

    /** The associated information, or null. */
    public IIOMetadata getMetadata() {
        return this.metadata;
    }

    /** Replaces it. */
    public void setMetadata(IIOMetadata metadata) {
        this.metadata = metadata;
    }
}
