package java.awt.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 * An image with the pixels **in memory**, readable and writable.
 *
 * <p>It is where the three pieces of the package come together: a {@link WritableRaster} that has
 * the pixels, a {@link ColorModel} that says what colour they are, and the {@link Image} interface
 * that makes it drawable anywhere. Everything else in `java.awt.image` exists to produce one of
 * these or to transform it.
 *
 * <p>The `TYPE_*` constants are shortcuts for the usual combinations of colour model and layout,
 * and choosing well matters more than it seems: `TYPE_INT_RGB` stores four bytes per pixel for
 * three components and draws very fast; `TYPE_3BYTE_BGR` stores three and is slower. `TYPE_CUSTOM`
 * is not a format but the answer "this is none of the others".
 *
 * <p>The `_PRE` family stores the colour already multiplied by the alpha. It is not a decorative
 * variant: compositing premultiplied images is adding, and without premultiplying there is one
 * multiplication per pixel and per operation. It is paid when creating it and saved on every draw.
 *
 * <p>It is a {@link RenderedImage} of **a single tile**, and from that come almost all of its
 * answers about tiles: one across, one down, the (0,0) one, of the size of the image.
 *
 * <p><strong>It can be drawn on.</strong> This note used to say that {@link #createGraphics} and
 * {@link #getGraphics} threw `UnsupportedOperationException` for want of a rasteriser; both return
 * one now —see their own notes for what it does and what it only records— and everything else of
 * the class works as well: reading and writing pixels, cropping, copying, converting and filtering.
 */
public class BufferedImage extends Image implements WritableRenderedImage, Transparency {

    /** None of the named formats. */
    public static final int TYPE_CUSTOM = 0;

    /** One `int` per pixel, with no alpha: 8 bits of padding and 8 per component. */
    public static final int TYPE_INT_RGB = 1;

    /** One `int` per pixel, with alpha. */
    public static final int TYPE_INT_ARGB = 2;

    /** Like the previous one, with the colour already multiplied by the alpha. */
    public static final int TYPE_INT_ARGB_PRE = 3;

    /** One `int` per pixel with the components the other way round: blue in the high bits. */
    public static final int TYPE_INT_BGR = 4;

    /** Three bytes per pixel, in blue, green, red order. */
    public static final int TYPE_3BYTE_BGR = 5;

    /** Four bytes per pixel, in alpha, blue, green, red order. */
    public static final int TYPE_4BYTE_ABGR = 6;

    /** Like the previous one, with the colour already multiplied by the alpha. */
    public static final int TYPE_4BYTE_ABGR_PRE = 7;

    /** Sixteen bits per pixel split 5-6-5. */
    public static final int TYPE_USHORT_565_RGB = 8;

    /** Fifteen bits per pixel split 5-5-5. */
    public static final int TYPE_USHORT_555_RGB = 9;

    /** One byte per pixel, in grey. */
    public static final int TYPE_BYTE_GRAY = 10;

    /** Sixteen bits per pixel, in grey. */
    public static final int TYPE_USHORT_GRAY = 11;

    /** One, two or four bits per pixel, with a palette. */
    public static final int TYPE_BYTE_BINARY = 12;

    /** One byte per pixel, with a palette of up to 256 colours. */
    public static final int TYPE_BYTE_INDEXED = 13;

    private int imageType = TYPE_CUSTOM;
    private ColorModel colorModel;
    private final WritableRaster raster;
    private Hashtable<String, Object> properties;
    private ImageProducer source;

    /**
     * An image of one of the named formats.
     *
     * @throws IllegalArgumentException if the type is not one of the fourteen, if it is
     *     {@link #TYPE_CUSTOM}, or if the size is empty
     */
    public BufferedImage(int width, int height, int imageType) {
        ColorModel cm;
        WritableRaster wr;
        if (imageType == TYPE_INT_RGB) {
            cm = new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_INT_ARGB) {
            cm = ColorModel.getRGBdefault();
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_INT_ARGB_PRE) {
            cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 32,
                    0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000, true, DataBuffer.TYPE_INT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_INT_BGR) {
            cm = new DirectColorModel(24, 0x000000FF, 0x0000FF00, 0x00FF0000);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_3BYTE_BGR) {
            int[] nBits = { 8, 8, 8 };
            int[] bOffs = { 2, 1, 0 };
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, false,
                    false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, width * 3, 3,
                    bOffs, null);
        } else if (imageType == TYPE_4BYTE_ABGR || imageType == TYPE_4BYTE_ABGR_PRE) {
            int[] nBits = { 8, 8, 8, 8 };
            int[] bOffs = { 3, 2, 1, 0 };
            boolean pre = imageType == TYPE_4BYTE_ABGR_PRE;
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, true,
                    pre, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, width * 4, 4,
                    bOffs, null);
        } else if (imageType == TYPE_USHORT_565_RGB) {
            cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 16, 0xF800,
                    0x07E0, 0x001F, 0, false, DataBuffer.TYPE_USHORT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_USHORT_555_RGB) {
            cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 15, 0x7C00,
                    0x03E0, 0x001F, 0, false, DataBuffer.TYPE_USHORT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_BYTE_GRAY) {
            int[] nBits = { 8 };
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), nBits, false,
                    true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_USHORT_GRAY) {
            int[] nBits = { 16 };
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), nBits, false,
                    true, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_BYTE_BINARY) {
            byte[] arr = { (byte) 0, (byte) 0xFF };
            cm = new IndexColorModel(1, 2, arr, arr, arr);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_BYTE_INDEXED) {
            // A cube of 6x6x6 colours, which is 216, and the rest a ramp of greys: it is the
            // palette of 256 any image looks reasonable with, without having to work one out for
            // it.
            int[] cmap = new int[256];
            int i = 0;
            for (int r = 0; r < 256; r = r + 51) {
                for (int g = 0; g < 256; g = g + 51) {
                    for (int b = 0; b < 256; b = b + 51) {
                        cmap[i] = (r << 16) | (g << 8) | b;
                        i = i + 1;
                    }
                }
            }
            int grayIncr = 256 / (256 - i);
            int gray = grayIncr * 3;
            while (i < 256) {
                cmap[i] = (gray << 16) | (gray << 8) | gray;
                gray = gray + grayIncr;
                i = i + 1;
            }
            cm = new IndexColorModel(8, 256, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else {
            throw new IllegalArgumentException("Unknown image type " + imageType);
        }
        this.colorModel = cm;
        this.raster = wr;
        this.imageType = imageType;
    }

    /**
     * An image with a palette.
     *
     * @throws IllegalArgumentException if the type is neither {@link #TYPE_BYTE_BINARY} nor {@link
     *     #TYPE_BYTE_INDEXED}, if the palette has premultiplied alpha, or if it has more than 16
     *     entries for the binary type
     */
    public BufferedImage(int width, int height, int imageType, IndexColorModel cm) {
        if (cm.hasAlpha() && cm.isAlphaPremultiplied()) {
            throw new IllegalArgumentException("This image types do not have premultiplied alpha.");
        }
        WritableRaster wr;
        if (imageType == TYPE_BYTE_BINARY) {
            int bits;
            int mapSize = cm.getMapSize();
            if (mapSize <= 2) {
                bits = 1;
            } else if (mapSize <= 4) {
                bits = 2;
            } else if (mapSize <= 16) {
                bits = 4;
            } else {
                throw new IllegalArgumentException("Color map for TYPE_BYTE_BINARY must have "
                        + "no more than 16 entries");
            }
            wr = Raster.createPackedRaster(DataBuffer.TYPE_BYTE, width, height, 1, bits, null);
        } else if (imageType == TYPE_BYTE_INDEXED) {
            wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 1, null);
        } else {
            throw new IllegalArgumentException("Invalid image type (" + imageType + ").  Image "
                    + "type must be either TYPE_BYTE_BINARY or  TYPE_BYTE_INDEXED");
        }
        if (!cm.isCompatibleRaster(wr)) {
            throw new IllegalArgumentException("Incompatible image type and IndexColorModel");
        }
        this.colorModel = cm;
        this.raster = wr;
        this.imageType = imageType;
    }

    /**
     * An image over a given raster and colour model.
     *
     * <p>It is the general constructor and the only one that can give an image of {@link
     * #TYPE_CUSTOM}. The type is **deduced** by looking at the model and the layout: if they match
     * one of the named formats, that one is declared.
     *
     * @throws IllegalArgumentException if the raster does not suit the colour model, or if its
     *     corner is not at the origin
     * @throws RasterFormatException if the raster does not have enough bands for the model
     * @throws NullPointerException if the model or the raster is missing
     */
    public BufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
            Hashtable<?, ?> properties) {
        if (!cm.isCompatibleRaster(raster)) {
            throw new IllegalArgumentException("Raster " + raster
                    + " is incompatible with ColorModel " + cm);
        }
        if (raster.getMinX() != 0 || raster.getMinY() != 0) {
            throw new IllegalArgumentException("Raster " + raster
                    + " has minX or minY not equal to zero: " + raster.getMinX() + " "
                    + raster.getMinY());
        }
        this.colorModel = cm;
        this.raster = raster;
        if (properties != null && !properties.isEmpty()) {
            this.properties = new Hashtable<String, Object>();
            java.util.Enumeration<?> e = properties.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                if (key instanceof String) {
                    this.properties.put((String) key, properties.get(key));
                }
            }
        }
        // The premultiplication state of the raster has to match the one the model declares: if it
        // did not, every colour read would be undoing a sum that was never done.
        this.coerceData(isRasterPremultiplied);
        this.imageType = this.inferType();
    }

    /**
     * Which of the named formats describes this model and this raster.
     *
     * @return the type, or {@link #TYPE_CUSTOM} if it is none of them
     */
    private int inferType() {
        ColorModel cm = this.colorModel;
        SampleModel sm = this.raster.getSampleModel();
        ColorSpace cs = cm.getColorSpace();
        boolean srgb = cs == ColorSpace.getInstance(ColorSpace.CS_sRGB);
        if (cm instanceof DirectColorModel && sm instanceof SinglePixelPackedSampleModel) {
            DirectColorModel dcm = (DirectColorModel) cm;
            if (srgb && dcm.getRedMask() == 0x00FF0000 && dcm.getGreenMask() == 0x0000FF00
                    && dcm.getBlueMask() == 0x000000FF) {
                if (!dcm.hasAlpha()) {
                    return TYPE_INT_RGB;
                }
                if (dcm.getAlphaMask() == 0xFF000000) {
                    return dcm.isAlphaPremultiplied() ? TYPE_INT_ARGB_PRE : TYPE_INT_ARGB;
                }
            }
            if (srgb && !dcm.hasAlpha() && dcm.getRedMask() == 0x000000FF
                    && dcm.getGreenMask() == 0x0000FF00 && dcm.getBlueMask() == 0x00FF0000) {
                return TYPE_INT_BGR;
            }
            if (srgb && !dcm.hasAlpha() && dcm.getTransferType() == DataBuffer.TYPE_USHORT) {
                if (dcm.getRedMask() == 0xF800 && dcm.getGreenMask() == 0x07E0
                        && dcm.getBlueMask() == 0x001F) {
                    return TYPE_USHORT_565_RGB;
                }
                if (dcm.getRedMask() == 0x7C00 && dcm.getGreenMask() == 0x03E0
                        && dcm.getBlueMask() == 0x001F) {
                    return TYPE_USHORT_555_RGB;
                }
            }
            return TYPE_CUSTOM;
        }
        if (cm instanceof IndexColorModel && sm.getNumBands() == 1) {
            if (sm instanceof MultiPixelPackedSampleModel) {
                int bits = ((MultiPixelPackedSampleModel) sm).getPixelBitStride();
                if (bits == 1 || bits == 2 || bits == 4) {
                    return TYPE_BYTE_BINARY;
                }
                return TYPE_CUSTOM;
            }
            if (cm.getPixelSize() == 8 && sm.getTransferType() == DataBuffer.TYPE_BYTE) {
                return TYPE_BYTE_INDEXED;
            }
            return TYPE_CUSTOM;
        }
        if (cm instanceof ComponentColorModel && sm instanceof ComponentSampleModel) {
            ComponentSampleModel csm = (ComponentSampleModel) sm;
            int[] offs = csm.getBandOffsets();
            if (cs.getType() == ColorSpace.TYPE_GRAY && srgbGris(cs) && !cm.hasAlpha()
                    && offs.length == 1 && csm.getPixelStride() == 1) {
                if (sm.getTransferType() == DataBuffer.TYPE_BYTE && cm.getComponentSize(0) == 8) {
                    return TYPE_BYTE_GRAY;
                }
                if (sm.getTransferType() == DataBuffer.TYPE_USHORT
                        && cm.getComponentSize(0) == 16) {
                    return TYPE_USHORT_GRAY;
                }
                return TYPE_CUSTOM;
            }
            if (!srgb || sm.getTransferType() != DataBuffer.TYPE_BYTE) {
                return TYPE_CUSTOM;
            }
            if (!cm.hasAlpha() && offs.length == 3 && csm.getPixelStride() == 3
                    && offs[0] == 2 && offs[1] == 1 && offs[2] == 0) {
                return TYPE_3BYTE_BGR;
            }
            if (cm.hasAlpha() && offs.length == 4 && csm.getPixelStride() == 4
                    && offs[0] == 3 && offs[1] == 2 && offs[2] == 1 && offs[3] == 0) {
                return cm.isAlphaPremultiplied() ? TYPE_4BYTE_ABGR_PRE : TYPE_4BYTE_ABGR;
            }
        }
        return TYPE_CUSTOM;
    }

    /** Whether that space is the default grey one. */
    private static boolean srgbGris(ColorSpace cs) {
        return cs == ColorSpace.getInstance(ColorSpace.CS_GRAY);
    }

    /** The format, or {@link #TYPE_CUSTOM}. */
    public int getType() {
        return this.imageType;
    }

    /** The colour model. */
    public ColorModel getColorModel() {
        return this.colorModel;
    }

    /**
     * The raster with the pixels.
     *
     * <p>It is the real one, not a copy: writing to it changes the image.
     */
    public WritableRaster getRaster() {
        return this.raster;
    }

    /**
     * The alpha channel as a one-band raster, or `null` if there is none.
     *
     * <p>It shares the data with the image.
     */
    public WritableRaster getAlphaRaster() {
        return this.colorModel.getAlphaRaster(this.raster);
    }

    /** The colour of a pixel, in ARGB with eight bits per channel. */
    public int getRGB(int x, int y) {
        return this.colorModel.getRGB(this.raster.getDataElements(x, y, null));
    }

    /**
     * The colours of a rectangle, in ARGB.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside or the array is not long
     *     enough
     */
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset,
            int scansize) {
        int[] out = rgbArray;
        if (out == null) {
            out = new int[offset + h * scansize];
        }
        Object data = null;
        int yoff = offset;
        for (int y = startY; y < startY + h; y++) {
            int off = yoff;
            for (int x = startX; x < startX + w; x++) {
                data = this.raster.getDataElements(x, y, data);
                out[off] = this.colorModel.getRGB(data);
                off = off + 1;
            }
            yoff = yoff + scansize;
        }
        return out;
    }

    /**
     * Sets the colour of a pixel, given in ARGB.
     *
     * <p>The colour is converted to the format of the image, and if the format cannot represent it
     * the closest one is stored: writing and reading back does not always return the same thing.
     */
    public void setRGB(int x, int y, int rgb) {
        this.raster.setDataElements(x, y, this.colorModel.getDataElements(rgb, null));
    }

    /**
     * Sets the colours of a rectangle, given in ARGB.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside or the array is not long
     *     enough
     */
    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset,
            int scansize) {
        Object pixel = null;
        int yoff = offset;
        for (int y = startY; y < startY + h; y++) {
            int off = yoff;
            for (int x = startX; x < startX + w; x++) {
                pixel = this.colorModel.getDataElements(rgbArray[off], pixel);
                this.raster.setDataElements(x, y, pixel);
                off = off + 1;
            }
            yoff = yoff + scansize;
        }
    }

    /** Width, in pixels. */
    public int getWidth() {
        return this.raster.getWidth();
    }

    /** Height, in pixels. */
    public int getHeight() {
        return this.raster.getHeight();
    }

    /**
     * Width, in pixels.
     *
     * <p>The observer is not used: the pixels are there already.
     */
    public int getWidth(ImageObserver observer) {
        return this.raster.getWidth();
    }

    /** Height, in pixels; the observer is not used. */
    public int getHeight(ImageObserver observer) {
        return this.raster.getHeight();
    }

    /** A producer that delivers these pixels. */
    public ImageProducer getSource() {
        if (this.source == null) {
            this.source = new BufferedImageSource(this);
        }
        return this.source;
    }

    /**
     * A property of the image.
     *
     * @return the value, or {@link Image#UndefinedProperty} if it is not defined
     * @throws NullPointerException if the name is `null`
     */
    public Object getProperty(String name, ImageObserver observer) {
        return this.getProperty(name);
    }

    /**
     * A property of the image.
     *
     * @return the value, or {@link Image#UndefinedProperty} if it is not defined
     * @throws NullPointerException if the name is `null`
     */
    public Object getProperty(String name) {
        if (name == null) {
            throw new NullPointerException("null property name is not allowed");
        }
        if (this.properties == null) {
            return Image.UndefinedProperty;
        }
        Object o = this.properties.get(name);
        if (o == null) {
            return Image.UndefinedProperty;
        }
        return o;
    }

    /** The names of the properties, or `null` if there are none. */
    public String[] getPropertyNames() {
        if (this.properties == null || this.properties.isEmpty()) {
            return null;
        }
        Set<String> keys = this.properties.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    /**
     * A context to draw over this image.
     *
     * <p><strong>It works.</strong> It returns this library's rasteriser: lines by Bresenham, fills
     * by scanline, arcs, polygons, rectangular clipping, translation, area copying and drawing of
     * other {@code BufferedImage}s. The only thing it declines is {@code drawString}, which needs
     * the outlines of the glyphs — a subsystem apart that is not there yet.
     *
     * <p>It is not the same as {@link #createGraphics}: that one promises a {@link Graphics2D},
     * with affine transforms, strokes, compositing and rendering hints, and that is one layer more
     * on top of this.
     */
    public Graphics getGraphics() {
        return new KajiGraphics(this);
    }

    /**
     * A context to draw over this image.
     *
     * <p><strong>It works.</strong> It returns the same object as {@link #getGraphics}, which is a
     * full {@link Graphics2D}: affine transforms —translation, rotation, scaling, shearing—, {@code
     * draw} and {@code fill} of any {@link java.awt.Shape}, stroke width, clipping by shape and
     * drawing of images with a transform.
     *
     * <p>Three things are stored and reported but <strong>not applied</strong>, and it is worth
     * knowing: a {@link java.awt.Paint} that is not a uniform colour, a {@link java.awt.Composite}
     * with partial alpha, and the rendering hints. All three ask for per-pixel evaluation against
     * the destination, which is a mechanism this tier does not have. What is honoured is that
     * {@code getPaint}, {@code getComposite} and {@code getRenderingHint} return what was set,
     * because there is code that saves them and restores them.
     *
     * <p>And {@code drawString} goes on declining: it needs the outlines of the glyphs.
     */
    public Graphics2D createGraphics() {
        return new KajiGraphics(this);
    }

    /**
     * A crop that shares the pixels with this one.
     *
     * <p>It copies nothing: writing into the crop changes the original image.
     *
     * @throws RasterFormatException if the rectangle does not fall inside
     */
    public BufferedImage getSubimage(int x, int y, int w, int h) {
        return new BufferedImage(this.colorModel, this.raster.createWritableChild(x, y, w, h, 0, 0,
                null), this.colorModel.isAlphaPremultiplied(), this.properties);
    }

    /** Whether the stored colour is already multiplied by the alpha. */
    public boolean isAlphaPremultiplied() {
        return this.colorModel.isAlphaPremultiplied();
    }

    /**
     * Premultiplies the pixels by their alpha, or undoes it, **in place**.
     *
     * <p>The operation loses information in one direction: premultiplying a pixel of alpha zero
     * takes it to black, and undoing it afterwards does not bring it back.
     */
    public void coerceData(boolean isAlphaPremultiplied) {
        if (this.colorModel.hasAlpha()
                && this.colorModel.isAlphaPremultiplied() != isAlphaPremultiplied) {
            this.colorModel = this.colorModel.coerceData(this.raster, isAlphaPremultiplied);
        }
    }

    public String toString() {
        return "BufferedImage@" + Integer.toHexString(this.hashCode()) + ": type = "
                + this.imageType + " " + this.colorModel + " " + this.raster;
    }

    /** The images this one is computed from: none. */
    public Vector<RenderedImage> getSources() {
        return null;
    }

    /** How the pixels are laid out. */
    public SampleModel getSampleModel() {
        return this.raster.getSampleModel();
    }

    /** Always 0: the image starts at the origin. */
    public int getMinX() {
        return this.raster.getMinX();
    }

    /** Always 0: the image starts at the origin. */
    public int getMinY() {
        return this.raster.getMinY();
    }

    /** Always 1: a single tile. */
    public int getNumXTiles() {
        return 1;
    }

    /** Always 1: a single tile. */
    public int getNumYTiles() {
        return 1;
    }

    /** Always 0: the only tile is the (0,0) one. */
    public int getMinTileX() {
        return 0;
    }

    /** Always 0: the only tile is the (0,0) one. */
    public int getMinTileY() {
        return 0;
    }

    /** The single tile measures what the image does. */
    public int getTileWidth() {
        return this.getWidth();
    }

    /** The single tile measures what the image does. */
    public int getTileHeight() {
        return this.getHeight();
    }

    /** Always 0: the tile grid starts at the origin. */
    public int getTileGridXOffset() {
        return this.raster.getSampleModelTranslateX();
    }

    /** Always 0: the tile grid starts at the origin. */
    public int getTileGridYOffset() {
        return this.raster.getSampleModelTranslateY();
    }

    /**
     * The single tile.
     *
     * @throws ArrayIndexOutOfBoundsException if the indices are not (0,0)
     */
    public Raster getTile(int tileX, int tileY) {
        if (tileX == 0 && tileY == 0) {
            return this.raster;
        }
        throw new ArrayIndexOutOfBoundsException("BufferedImages only have one tile with index 0,0");
    }

    /**
     * A **copy** of the whole image.
     *
     * <p>Unlike {@link #getRaster}, this copies: the result shares no data.
     */
    public Raster getData() {
        int width = this.raster.getWidth();
        int height = this.raster.getHeight();
        int startX = this.raster.getMinX();
        int startY = this.raster.getMinY();
        WritableRaster wr = Raster.createWritableRaster(this.raster.getSampleModel(),
                new Point(this.raster.getSampleModelTranslateX(),
                        this.raster.getSampleModelTranslateY()));
        Object tdata = null;
        for (int i = startY; i < startY + height; i++) {
            tdata = this.raster.getDataElements(startX, i, width, 1, tdata);
            wr.setDataElements(startX, i, width, 1, tdata);
        }
        return wr;
    }

    /**
     * A copy of a region of the image.
     *
     * @throws NullPointerException if the rectangle is `null`
     */
    public Raster getData(Rectangle rect) {
        SampleModel sm = this.raster.getSampleModel();
        SampleModel nsm = sm.createCompatibleSampleModel(rect.width, rect.height);
        WritableRaster wr = Raster.createWritableRaster(nsm, rect.getLocation());
        int width = rect.width;
        int height = rect.height;
        int startX = rect.x;
        int startY = rect.y;
        Object tdata = null;
        for (int i = startY; i < startY + height; i++) {
            tdata = this.raster.getDataElements(startX, i, width, 1, tdata);
            wr.setDataElements(startX, i, width, 1, tdata);
        }
        return wr;
    }

    /**
     * Copies the image into the given raster, or into a new one if it is `null`.
     *
     * <p>Only the part where the two overlap is copied.
     */
    public WritableRaster copyData(WritableRaster outRaster) {
        WritableRaster out = outRaster;
        if (out == null) {
            return (WritableRaster) this.getData();
        }
        int width = out.getWidth();
        int height = out.getHeight();
        int startX = out.getMinX();
        int startY = out.getMinY();
        Object tdata = null;
        for (int i = startY; i < startY + height; i++) {
            tdata = this.raster.getDataElements(startX, i, width, 1, tdata);
            out.setDataElements(startX, i, width, 1, tdata);
        }
        return out;
    }

    /**
     * Writes a raster into the image.
     *
     * <p>Only the part that falls inside is written; the rest is discarded.
     */
    public void setData(Raster r) {
        int width = r.getWidth();
        int height = r.getHeight();
        int startX = r.getMinX();
        int startY = r.getMinY();
        int[] tdata = null;
        Rectangle rclip = new Rectangle(startX, startY, width, height);
        Rectangle bclip = new Rectangle(0, 0, this.raster.getWidth(), this.raster.getHeight());
        Rectangle intersect = rclip.intersection(bclip);
        if (intersect.isEmpty()) {
            return;
        }
        width = intersect.width;
        height = intersect.height;
        startX = intersect.x;
        startY = intersect.y;
        for (int i = startY; i < startY + height; i++) {
            tdata = r.getPixels(startX, i, width, 1, tdata);
            this.raster.setPixels(startX, i, width, 1, tdata);
        }
    }

    /**
     * Adds a tile observer.
     *
     * <p>It does nothing: the single tile of an image in memory is always available for writing, so
     * there is no transition to report.
     */
    public void addTileObserver(TileObserver to) {
    }

    /** Removes that observer; it does nothing, for the same reason. */
    public void removeTileObserver(TileObserver to) {
    }

    /**
     * Whether that tile is taken for writing.
     *
     * <p>Always `true` for the (0,0) one: in an image in memory the tile is always writable.
     *
     * @throws IllegalArgumentException if the indices are not (0,0)
     */
    public boolean isTileWritable(int tileX, int tileY) {
        if (tileX == 0 && tileY == 0) {
            return true;
        }
        throw new IllegalArgumentException("Only 1 tile in image");
    }

    /** The index of the single tile. */
    public Point[] getWritableTileIndices() {
        Point[] p = new Point[1];
        p[0] = new Point(0, 0);
        return p;
    }

    /** Always `true`: the single tile is always writable. */
    public boolean hasTileWriters() {
        return true;
    }

    /**
     * The single tile, for writing.
     *
     * @throws IllegalArgumentException if the indices are not (0,0)
     */
    public WritableRaster getWritableTile(int tileX, int tileY) {
        if (tileX == 0 && tileY == 0) {
            return this.raster;
        }
        throw new IllegalArgumentException("Only 1 tile in image");
    }

    /**
     * Gives the single tile back.
     *
     * <p>It does nothing more than check the indices: there is no count of loans to keep.
     *
     * @throws IllegalArgumentException if the indices are not (0,0)
     */
    public void releaseWritableTile(int tileX, int tileY) {
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException("Only 1 tile in image");
        }
    }

    /** The transparency of the colour model. */
    public int getTransparency() {
        return this.colorModel.getTransparency();
    }
}
