package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.math.BigInteger;

/**
 * A colour model where the pixel is not a colour but a **palette number**.
 *
 * <p>It is the indirection that makes a GIF possible: instead of storing three bytes per pixel an
 * eight-bit index is stored, and the real colours live in a table of 256 entries. An image like
 * that takes a third of the room, and changing its palette recolours it whole without touching a
 * single pixel.
 *
 * <p>The price is that **converting to this model loses information**. A colour that is not in the
 * palette is replaced by the closest one, and that is done by {@link #getDataElements(int, Object)}
 * searching the whole table. It is the only expensive operation of the class, and it is expensive
 * on purpose: choosing the index badly shows.
 *
 * <p>The transparency is **deduced** from the palette and not declared. If no entry has alpha, the
 * model is opaque; if some has alpha zero and none has a value in between, it is by mask —the pixel
 * is there or it is not—; if there are values in between, it is translucent. That changes how many
 * components the model says it has, and that is why it is worked out before finishing building it.
 *
 * <p>A palette can have **holes**: entries that correspond to no valid colour, marked in the {@link
 * BigInteger} of {@link #getValidPixels}. A hole is not the same as a transparent colour: the
 * transparent one is a colour that can be used, the hole is never chosen.
 */
public class IndexColorModel extends ColorModel {

    private static final int[] opaqueBits = { 8, 8, 8 };
    private static final int[] alphaBits = { 8, 8, 8, 8 };

    private int[] rgb;
    private int map_size;
    private int transparent_index = -1;
    private boolean allgrayopaque;
    private BigInteger validBits;

    /**
     * With three arrays of components and no alpha.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 16 or if `size` is not
     *     positive
     * @throws NullPointerException if any of the arrays is missing
     * @throws ArrayIndexOutOfBoundsException if some array is shorter than `size`
     */
    public IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(size, r, g, b, null);
    }

    /**
     * Like the previous one, with one entry marked as transparent.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 16 or if `size` is not
     *     positive
     */
    public IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b, int trans) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(size, r, g, b, null);
        this.setTransparentPixel(trans);
    }

    /**
     * With four arrays of components.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 16 or if `size` is not
     *     positive
     */
    public IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b, byte[] a) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(size, r, g, b, a);
    }

    /**
     * With the palette packed into an array of bytes, three or four per entry.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 16, if `size` is not
     *     positive, or if the array is not long enough
     */
    public IndexColorModel(int bits, int size, byte[] cmap, int start, boolean hasalpha) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(bits, size, cmap, start, hasalpha, -1);
    }

    /**
     * Like the previous one, with one entry marked as transparent.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 16 or if `size` is not
     *     positive
     */
    public IndexColorModel(int bits, int size, byte[] cmap, int start, boolean hasalpha,
            int trans) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(bits, size, cmap, start, hasalpha, trans);
    }

    /**
     * With the palette in an array of ARGB and the transfer type given.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 16, if `size` is not
     *     positive, or if the type is neither `byte` nor `ushort`
     */
    public IndexColorModel(int bits, int size, int[] cmap, int start, boolean hasalpha, int trans,
            int transferType) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, transferType);
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException("transferType must be one of "
                    + "DataBuffer.TYPE_BYTE or DataBuffer.TYPE_USHORT");
        }
        this.setRGBs(size, cmap, start, hasalpha);
        this.setTransparentPixel(trans);
    }

    /**
     * With the palette in ARGB and a map of which entries are valid.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 16, if `size` is not
     *     positive, or if the type is neither `byte` nor `ushort`
     */
    public IndexColorModel(int bits, int size, int[] cmap, int start, int transferType,
            BigInteger validBits) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, transferType);
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException("transferType must be one of "
                    + "DataBuffer.TYPE_BYTE or DataBuffer.TYPE_USHORT");
        }
        if (validBits != null) {
            // A palette full of valid entries is the same as having no map, and not having one
            // saves a BigInteger query per pixel in everything that follows.
            boolean full = true;
            for (int i = 0; i < size; i++) {
                if (!validBits.testBit(i)) {
                    full = false;
                    break;
                }
            }
            if (!full) {
                this.validBits = validBits;
            }
        }
        this.setRGBs(size, cmap, start, true);
    }

    /** How much to reserve for the table, with room to spare for indices out of range. */
    private static int calcRealMapSize(int bits, int size) {
        int newSize = Math.max(1 << bits, size);
        return Math.max(newSize, 256);
    }

    /** Builds the table from three or four arrays of components. */
    private void setRGBs(int size, byte[] r, byte[] g, byte[] b, byte[] a) {
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        this.map_size = size;
        this.rgb = new int[calcRealMapSize(this.pixel_bits, size)];
        int alpha = 0xFF;
        int trans = Transparency.OPAQUE;
        boolean allgray = true;
        int transparentIndex = -1;
        for (int i = 0; i < size; i++) {
            int rc = r[i] & 0xFF;
            int gc = g[i] & 0xFF;
            int bc = b[i] & 0xFF;
            allgray = allgray && rc == gc && gc == bc;
            if (a != null) {
                alpha = a[i] & 0xFF;
                if (alpha != 0xFF) {
                    if (alpha == 0x00) {
                        if (trans == Transparency.OPAQUE) {
                            trans = Transparency.BITMASK;
                        }
                        if (transparentIndex < 0) {
                            transparentIndex = i;
                        }
                    } else {
                        trans = Transparency.TRANSLUCENT;
                    }
                    allgray = false;
                }
            }
            this.rgb[i] = (alpha << 24) | (rc << 16) | (gc << 8) | bc;
        }
        this.allgrayopaque = allgray;
        this.setTransparency(trans);
        this.setTransparentPixel(transparentIndex);
    }

    /** Builds the table from an array of bytes with three or four per entry. */
    private void setRGBs(int bits, int size, byte[] cmap, int start, boolean hasalpha, int trans) {
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        this.map_size = size;
        this.rgb = new int[calcRealMapSize(bits, size)];
        int j = start;
        int alpha = 0xFF;
        int transparency = Transparency.OPAQUE;
        boolean allgray = true;
        int transparentIndex = -1;
        for (int i = 0; i < size; i++) {
            int rc = cmap[j] & 0xFF;
            j = j + 1;
            int gc = cmap[j] & 0xFF;
            j = j + 1;
            int bc = cmap[j] & 0xFF;
            j = j + 1;
            allgray = allgray && rc == gc && gc == bc;
            if (hasalpha) {
                alpha = cmap[j] & 0xFF;
                j = j + 1;
                if (alpha != 0xFF) {
                    if (alpha == 0x00) {
                        if (transparency == Transparency.OPAQUE) {
                            transparency = Transparency.BITMASK;
                        }
                        if (transparentIndex < 0) {
                            transparentIndex = i;
                        }
                    } else {
                        transparency = Transparency.TRANSLUCENT;
                    }
                    allgray = false;
                }
            }
            this.rgb[i] = (alpha << 24) | (rc << 16) | (gc << 8) | bc;
        }
        this.allgrayopaque = allgray;
        this.setTransparency(transparency);
        this.setTransparentPixel(trans >= 0 ? trans : transparentIndex);
    }

    /** Builds the table from an array of ARGB. */
    private void setRGBs(int size, int[] cmap, int start, boolean hasalpha) {
        this.map_size = size;
        this.rgb = new int[calcRealMapSize(this.pixel_bits, size)];
        int transparency = Transparency.OPAQUE;
        boolean allgray = true;
        BigInteger validBitsLocal = this.validBits;
        for (int i = 0; i < size; i++) {
            int cmapValue = cmap[i + start];
            this.rgb[i] = cmapValue;
            if (validBitsLocal != null && !validBitsLocal.testBit(i)) {
                continue;
            }
            int rc = (cmapValue >> 16) & 0xFF;
            int gc = (cmapValue >> 8) & 0xFF;
            int bc = cmapValue & 0xFF;
            allgray = allgray && rc == gc && gc == bc;
            if (hasalpha) {
                int alpha = cmapValue >>> 24;
                if (alpha != 0xFF) {
                    if (alpha == 0x00) {
                        if (transparency == Transparency.OPAQUE) {
                            transparency = Transparency.BITMASK;
                        }
                        if (this.transparent_index < 0) {
                            this.transparent_index = i;
                        }
                    } else {
                        transparency = Transparency.TRANSLUCENT;
                    }
                    allgray = false;
                }
            } else {
                this.rgb[i] = cmapValue | 0xFF000000;
            }
        }
        this.allgrayopaque = allgray;
        this.setTransparency(transparency);
    }

    /**
     * Sets the deduced transparency and adjusts how many components the model has.
     *
     * <p>An opaque indexed model has three components and one with alpha, four. That changes what
     * `getNumComponents` and `getComponentSize` return, so it has to be adjusted here and not in
     * the constructor: only now is it known.
     */
    private void setTransparency(int transparency) {
        if (this.transparency != transparency) {
            this.transparency = transparency;
            if (transparency == Transparency.OPAQUE) {
                this.supportsAlpha = false;
                this.numComponents = 3;
                this.nBits = opaqueBits;
            } else {
                this.supportsAlpha = true;
                this.numComponents = 4;
                this.nBits = alphaBits;
            }
        }
    }

    /** Marks an entry as transparent, by setting its alpha to zero. */
    private void setTransparentPixel(int trans) {
        if (trans < 0 || trans >= this.map_size) {
            return;
        }
        this.rgb[trans] = this.rgb[trans] & 0x00FFFFFF;
        this.transparent_index = trans;
        this.allgrayopaque = false;
        if (this.transparency == Transparency.OPAQUE) {
            this.setTransparency(Transparency.BITMASK);
        }
    }

    /** `OPAQUE`, `BITMASK` or `TRANSLUCENT`, deduced from the palette. */
    public int getTransparency() {
        return this.transparency;
    }

    /** Eight bits per component; four components if there is alpha. */
    public int[] getComponentSize() {
        if (this.nBits == null) {
            return null;
        }
        return this.nBits.clone();
    }

    /** How many entries the palette has. */
    public final int getMapSize() {
        return this.map_size;
    }

    /** The entry marked as transparent, or -1 if there is none. */
    public final int getTransparentPixel() {
        return this.transparent_index;
    }

    /** Copies the reds of the palette. */
    public final void getReds(byte[] r) {
        for (int i = 0; i < this.map_size; i++) {
            r[i] = (byte) (this.rgb[i] >> 16);
        }
    }

    /** Copies the greens of the palette. */
    public final void getGreens(byte[] g) {
        for (int i = 0; i < this.map_size; i++) {
            g[i] = (byte) (this.rgb[i] >> 8);
        }
    }

    /** Copies the blues of the palette. */
    public final void getBlues(byte[] b) {
        for (int i = 0; i < this.map_size; i++) {
            b[i] = (byte) this.rgb[i];
        }
    }

    /** Copies the alphas of the palette. */
    public final void getAlphas(byte[] a) {
        for (int i = 0; i < this.map_size; i++) {
            a[i] = (byte) (this.rgb[i] >> 24);
        }
    }

    /** Copies the whole palette as ARGB. */
    public final void getRGBs(int[] rgb) {
        System.arraycopy(this.rgb, 0, rgb, 0, this.map_size);
    }

    /** The red of that entry of the palette. */
    public final int getRed(int pixel) {
        return (this.rgb[pixel] >> 16) & 0xFF;
    }

    /** The green of that entry of the palette. */
    public final int getGreen(int pixel) {
        return (this.rgb[pixel] >> 8) & 0xFF;
    }

    /** The blue of that entry of the palette. */
    public final int getBlue(int pixel) {
        return this.rgb[pixel] & 0xFF;
    }

    /** The alpha of that entry of the palette. */
    public final int getAlpha(int pixel) {
        return (this.rgb[pixel] >> 24) & 0xFF;
    }

    /** The ARGB of that entry of the palette. */
    public final int getRGB(int pixel) {
        return this.rgb[pixel];
    }

    /**
     * The palette index that best represents that colour.
     *
     * <p>First an exact match is looked for and, if there is none, the closest entry by squared
     * Euclidean distance over the four components. On a tie the smaller index wins, so that the
     * result does not depend on the order things are walked in.
     *
     * <p>It is the expensive operation of the class, and the one that actually loses information:
     * the colour that goes in is almost never the one that comes out.
     *
     * @throws UnsupportedOperationException if the transfer type is neither `byte` nor `ushort`
     */
    public synchronized Object getDataElements(int rgb, Object pixel) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int alpha = rgb >>> 24;
        int pix = 0;
        boolean found = false;
        for (int i = 0; i < this.map_size; i++) {
            if (this.isValidEntry(i) && this.rgb[i] == rgb) {
                pix = i;
                found = true;
                break;
            }
        }
        if (!found) {
            // A fully transparent colour goes to the transparent entry if there is one: any other
            // would add a colour that is not seen but that comes back when compositing.
            if (alpha == 0 && this.transparent_index >= 0) {
                pix = this.transparent_index;
            } else {
                int best = Integer.MAX_VALUE;
                for (int i = 0; i < this.map_size; i++) {
                    if (!this.isValidEntry(i)) {
                        continue;
                    }
                    int c = this.rgb[i];
                    int da = (c >>> 24) - alpha;
                    int dr = ((c >> 16) & 0xFF) - red;
                    int dg = ((c >> 8) & 0xFF) - green;
                    int db = (c & 0xFF) - blue;
                    int error = da * da + dr * dr + dg * dg + db * db;
                    if (error < best) {
                        best = error;
                        pix = i;
                    }
                }
            }
        }
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] out = pixel == null ? new byte[1] : (byte[]) pixel;
            out[0] = (byte) pix;
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] out = pixel == null ? new short[1] : (short[]) pixel;
            out[0] = (short) pix;
            return out;
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /** Whether that entry of the palette corresponds to a real colour. */
    private boolean isValidEntry(int pixel) {
        return this.validBits == null || this.validBits.testBit(pixel);
    }

    /** The components of the colour of that entry. */
    public int[] getComponents(int pixel, int[] components, int offset) {
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        out[offset] = this.getRed(pixel);
        out[offset + 1] = this.getGreen(pixel);
        out[offset + 2] = this.getBlue(pixel);
        if (this.supportsAlpha && out.length - offset > 3) {
            out[offset + 3] = this.getAlpha(pixel);
        }
        return out;
    }

    /**
     * The components of the colour of that raw pixel.
     *
     * @throws UnsupportedOperationException if the transfer type is neither `byte` nor `ushort`
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        int pix;
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            pix = ((byte[]) pixel)[0] & 0xFF;
        } else if (this.transferType == DataBuffer.TYPE_USHORT) {
            pix = ((short[]) pixel)[0] & 0xFFFF;
        } else if (this.transferType == DataBuffer.TYPE_INT) {
            pix = ((int[]) pixel)[0];
        } else {
            throw new UnsupportedOperationException(
                    "This method has not been implemented for transferType " + this.transferType);
        }
        return this.getComponents(pix, components, offset);
    }

    /**
     * The palette index closest to that colour given by components.
     *
     * @throws IllegalArgumentException if the array does not carry every component
     */
    public int getDataElement(int[] components, int offset) {
        int rgb = (components[offset] << 16) | (components[offset + 1] << 8)
                | components[offset + 2];
        if (this.supportsAlpha) {
            rgb = rgb | (components[offset + 3] << 24);
        } else {
            rgb = rgb | 0xFF000000;
        }
        Object inData = this.getDataElements(rgb, null);
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            return ((byte[]) inData)[0] & 0xFF;
        }
        return ((short[]) inData)[0] & 0xFFFF;
    }

    /**
     * Like the previous one, in an array of the transfer type.
     *
     * @throws UnsupportedOperationException if the transfer type is neither `byte` nor `ushort`
     */
    public Object getDataElements(int[] components, int offset, Object pixel) {
        int rgb = (components[offset] << 16) | (components[offset + 1] << 8)
                | components[offset + 2];
        if (this.supportsAlpha) {
            rgb = rgb | (components[offset + 3] << 24);
        } else {
            rgb = rgb | 0xFF000000;
        }
        return this.getDataElements(rgb, pixel);
    }

    /**
     * A raster that stores one index per pixel.
     *
     * <p>With small palettes what comes out is a packed raster of several pixels per element: an
     * image of two colours takes one bit per pixel and not one byte.
     *
     * @throws IllegalArgumentException if the size is empty
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        if (this.pixel_bits == 1 || this.pixel_bits == 2 || this.pixel_bits == 4) {
            return Raster.createPackedRaster(DataBuffer.TYPE_BYTE, w, h, 1, this.pixel_bits, null);
        }
        if (this.pixel_bits <= 8) {
            return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, 1, null);
        }
        if (this.pixel_bits <= 16) {
            return Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, w, h, 1, null);
        }
        throw new UnsupportedOperationException("This method is not supported for pixel bits > 16.");
    }

    /** Whether that raster has a single band of the type and width that corresponds. */
    public boolean isCompatibleRaster(Raster raster) {
        int size = raster.getSampleModel().getSampleSize(0);
        return raster.getTransferType() == this.transferType && raster.getNumBands() == 1
                && (1 << size) >= this.map_size;
    }

    /**
     * A one-band sample model.
     *
     * @throws IllegalArgumentException if the size is empty
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        if (this.pixel_bits == 1 || this.pixel_bits == 2 || this.pixel_bits == 4) {
            return new MultiPixelPackedSampleModel(this.transferType, w, h, this.pixel_bits);
        }
        int[] bandOffsets = new int[1];
        bandOffsets[0] = 0;
        return new ComponentSampleModel(this.transferType, w, h, 1, w, bandOffsets);
    }

    /** Whether that sample model stores one index per pixel. */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (!(sm instanceof ComponentSampleModel) && !(sm instanceof MultiPixelPackedSampleModel)) {
            return false;
        }
        if (sm.getTransferType() != this.transferType) {
            return false;
        }
        return sm.getNumBands() == 1;
    }

    /** Whether every entry of the palette is valid. */
    public boolean isValid() {
        return this.validBits == null;
    }

    /** Whether that entry of the palette is valid. */
    public boolean isValid(int pixel) {
        if (pixel < 0 || pixel >= this.map_size) {
            return false;
        }
        return this.isValidEntry(pixel);
    }

    /**
     * Which entries of the palette are valid, or `null` if they all are.
     *
     * <p>Bit `i` is on if entry `i` corresponds to a real colour.
     */
    public BigInteger getValidPixels() {
        if (this.validBits == null) {
            return this.getAllValid();
        }
        return this.validBits;
    }

    /** A map with every entry switched on. */
    private BigInteger getAllValid() {
        int numbytes = (this.map_size + 7) / 8;
        byte[] valid = new byte[numbytes];
        java.util.Arrays.fill(valid, (byte) 0xFF);
        valid[0] = (byte) (0xFF >>> (numbytes * 8 - this.map_size));
        return new BigInteger(1, valid);
    }

    /**
     * Undoes the indirection: the same image with the colour in each pixel.
     *
     * <p>It is the inverse operation of indexing, and it loses nothing — the palette had the colour
     * of each index already.
     *
     * @param forceARGB `true` for the result to have alpha even if the palette is opaque
     */
    public BufferedImage convertToIntDiscrete(Raster raster, boolean forceARGB) {
        ColorModel cm;
        if (forceARGB || this.transparency == Transparency.TRANSLUCENT) {
            cm = ColorModel.getRGBdefault();
        } else if (this.transparency == Transparency.BITMASK) {
            cm = new DirectColorModel(25, 0xFF0000, 0x00FF00, 0x0000FF, 0x1000000);
        } else {
            cm = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
        }
        int w = raster.getWidth();
        int h = raster.getHeight();
        WritableRaster discreteRaster = cm.createCompatibleWritableRaster(w, h);
        int rX = raster.getMinX();
        int rY = raster.getMinY();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                row[x] = this.rgb[raster.getSample(rX + x, rY + y, 0)];
            }
            discreteRaster.setDataElements(0, y, w, 1, row);
        }
        return new BufferedImage(cm, discreteRaster, false, null);
    }

    public String toString() {
        return "IndexColorModel: #pixelBits = " + this.pixel_bits + " numComponents = "
                + this.numComponents + " color space = " + this.getColorSpace()
                + " transparency = " + this.transparency + " transIndex   = "
                + this.transparent_index + " has alpha = " + this.supportsAlpha
                + " isAlphaPre = " + this.isAlphaPremultiplied;
    }
}
