package java.awt.image;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * A {@link Raster} that can also be **written** to.
 *
 * <p>All the geometry —the translation of the sample model, the crops that share data, the edge
 * checks— is in the parent class already. Here the methods that write are added, which are the
 * exact mirror of the ones that read, plus two that have no mirror: {@link #setRect} and {@link
 * #setDataElements(int, int, Raster)}, which copy from another raster.
 *
 * <p>That writing lives in a subclass is not a detail of organisation. A method that receives a
 * `Raster` declares that it is only going to read it, and a read-only crop over writable data is an
 * honest view: the type says what whoever holds it can do, not what is underneath.
 *
 * <p>The difference between the two copying methods is at the edge. {@link #setRect} **clips**:
 * whatever falls outside is discarded silently, because copying an image against a corner is normal
 * and not an error. {@link #setDataElements(int, int, Raster)} **throws**, because it copies raw
 * pixels and there an overflow would be an arithmetic mistake, not a wanted clip.
 */
public class WritableRaster extends Raster {

    /**
     * A writable raster with a new buffer, of the size of the model, placed at `origin`.
     *
     * @throws RasterFormatException if the resulting size is empty
     */
    protected WritableRaster(SampleModel sampleModel, Point origin) {
        this(sampleModel, sampleModel.createDataBuffer(),
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * A writable raster over the given buffer, of the size of the model, placed at `origin`.
     *
     * @throws RasterFormatException if the resulting size is empty
     */
    protected WritableRaster(SampleModel sampleModel, DataBuffer dataBuffer, Point origin) {
        this(sampleModel, dataBuffer,
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * The general constructor: region, translation and parent given separately.
     *
     * @throws NullPointerException if any of the first four is missing
     * @throws RasterFormatException if the region is empty
     */
    protected WritableRaster(SampleModel sampleModel, DataBuffer dataBuffer, Rectangle aRegion,
            Point sampleModelTranslate, WritableRaster parent) {
        super(sampleModel, dataBuffer, aRegion, sampleModelTranslate, parent);
    }

    /**
     * The writable raster this one is a crop of, or `null`.
     *
     * <p>The `parent` field is inherited from {@link Raster}; here it is returned with its true
     * type, because a writable child is only built over a writable parent.
     */
    public WritableRaster getWritableParent() {
        return (WritableRaster) this.parent;
    }

    /**
     * The same raster moved to other coordinates, **over the same data**.
     *
     * @throws RasterFormatException if the new coordinates go past `int`
     */
    public WritableRaster createWritableTranslatedChild(int childMinX, int childMinY) {
        return this.createWritableChild(this.minX, this.minY, this.width, this.height, childMinX,
                childMinY, null);
    }

    /**
     * A writable crop over the **same data**, optionally with fewer bands.
     *
     * <p>Writing into the child changes the parent: there is no copy in between.
     *
     * @throws RasterFormatException if the rectangle asked for does not fall inside this one
     */
    public WritableRaster createWritableChild(int parentX, int parentY, int w, int h,
            int childMinX, int childMinY, int[] bandList) {
        if (parentX < this.minX) {
            throw new RasterFormatException("parentX lies outside raster");
        }
        if (parentY < this.minY) {
            throw new RasterFormatException("parentY lies outside raster");
        }
        if (parentX + w < parentX || parentX + w > this.minX + this.width) {
            throw new RasterFormatException("(parentX + width) is outside raster");
        }
        if (parentY + h < parentY || parentY + h > this.minY + this.height) {
            throw new RasterFormatException("(parentY + height) is outside raster");
        }
        SampleModel subSampleModel;
        if (bandList == null) {
            subSampleModel = this.sampleModel;
        } else {
            subSampleModel = this.sampleModel.createSubsetSampleModel(bandList);
        }
        int deltaX = childMinX - parentX;
        int deltaY = childMinY - parentY;
        return new WritableRaster(subSampleModel, this.dataBuffer,
                new Rectangle(childMinX, childMinY, w, h),
                new Point(this.sampleModelTranslateX + deltaX,
                        this.sampleModelTranslateY + deltaY),
                this);
    }

    /**
     * Checks that a point falls inside.
     *
     * @throws ArrayIndexOutOfBoundsException if it does not
     */
    private void checkPoint(int x, int y) {
        if (x < this.minX || y < this.minY || x >= this.minX + this.width
                || y >= this.minY + this.height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * Checks that a rectangle falls inside.
     *
     * @throws ArrayIndexOutOfBoundsException if it does not
     */
    private void checkRect(int x, int y, int w, int h) {
        if (x < this.minX || y < this.minY || x + w > this.minX + this.width
                || y + h > this.minY + this.height || x + w < x || y + h < y) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * Writes a raw pixel, without unpacking.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public void setDataElements(int x, int y, Object inData) {
        this.checkPoint(x, y);
        this.sampleModel.setDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, inData, this.dataBuffer);
    }

    /**
     * Copies another raster here, raw.
     *
     * <p>`(x, y)` is where the corner of the source raster goes. If something does not fit, it
     * **throws**: they are raw pixels and an overflow would be an arithmetic mistake.
     *
     * @throws ArrayIndexOutOfBoundsException if the source does not fit whole
     */
    public void setDataElements(int x, int y, Raster inRaster) {
        int dstOffX = x + inRaster.getMinX();
        int dstOffY = y + inRaster.getMinY();
        int w = inRaster.getWidth();
        int h = inRaster.getHeight();
        if (dstOffX < this.minX || dstOffY < this.minY
                || dstOffX + w > this.minX + this.width
                || dstOffY + h > this.minY + this.height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
        int srcOffX = inRaster.getMinX();
        int srcOffY = inRaster.getMinY();
        // Row by row and not all at once: the whole rectangle might not fit in memory, and one row
        // at a time the temporary array gets reused.
        Object tdata = null;
        for (int startY = 0; startY < h; startY++) {
            tdata = inRaster.getDataElements(srcOffX, srcOffY + startY, w, 1, tdata);
            this.setDataElements(dstOffX, dstOffY + startY, w, 1, tdata);
        }
    }

    /**
     * Writes the raw pixels of a rectangle.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public void setDataElements(int x, int y, int w, int h, Object inData) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, inData, this.dataBuffer);
    }

    /**
     * Copies another raster here, pixel by pixel, with the corner in the same place.
     *
     * <p>Whatever falls outside is discarded.
     */
    public void setRect(Raster srcRaster) {
        this.setRect(0, 0, srcRaster);
    }

    /**
     * Copies another raster here, shifted by `(dx, dy)`.
     *
     * <p><strong>It clips</strong>: whatever falls outside is discarded silently. It is the
     * opposite of {@link #setDataElements(int, int, Raster)}, and on purpose — pasting an image
     * against a corner is normal and not an error.
     *
     * <p>The copy goes through band values, not through raw elements, so source and destination can
     * have different layouts as long as the bands match.
     */
    public void setRect(int dx, int dy, Raster srcRaster) {
        int w = srcRaster.getWidth();
        int h = srcRaster.getHeight();
        int srcOffX = srcRaster.getMinX();
        int srcOffY = srcRaster.getMinY();
        int dstOffX = dx + srcOffX;
        int dstOffY = dy + srcOffY;
        if (dstOffX < this.minX) {
            int skipX = this.minX - dstOffX;
            w = w - skipX;
            srcOffX = srcOffX + skipX;
            dstOffX = this.minX;
        }
        if (dstOffY < this.minY) {
            int skipY = this.minY - dstOffY;
            h = h - skipY;
            srcOffY = srcOffY + skipY;
            dstOffY = this.minY;
        }
        if (dstOffX + w > this.minX + this.width) {
            w = this.minX + this.width - dstOffX;
        }
        if (dstOffY + h > this.minY + this.height) {
            h = this.minY + this.height - dstOffY;
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        // The integer types go through int and the floating ones through their own type: converting
        // a float to int to copy it would lose the decimal part in an operation that should lose
        // nothing.
        int type = srcRaster.getSampleModel().getDataType();
        if (type == DataBuffer.TYPE_FLOAT) {
            float[] fData = null;
            for (int startY = 0; startY < h; startY++) {
                fData = srcRaster.getPixels(srcOffX, srcOffY + startY, w, 1, fData);
                this.setPixels(dstOffX, dstOffY + startY, w, 1, fData);
            }
            return;
        }
        if (type == DataBuffer.TYPE_DOUBLE) {
            double[] dData = null;
            for (int startY = 0; startY < h; startY++) {
                dData = srcRaster.getPixels(srcOffX, srcOffY + startY, w, 1, dData);
                this.setPixels(dstOffX, dstOffY + startY, w, 1, dData);
            }
            return;
        }
        int[] iData = null;
        for (int startY = 0; startY < h; startY++) {
            iData = srcRaster.getPixels(srcOffX, srcOffY + startY, w, 1, iData);
            this.setPixels(dstOffX, dstOffY + startY, w, 1, iData);
        }
    }

    /**
     * Writes every band of a pixel.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public void setPixel(int x, int y, int[] iArray) {
        this.checkPoint(x, y);
        this.sampleModel.setPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, iArray, this.dataBuffer);
    }

    /**
     * Like the previous one, from `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public void setPixel(int x, int y, float[] fArray) {
        this.checkPoint(x, y);
        this.sampleModel.setPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, fArray, this.dataBuffer);
    }

    /**
     * Like the previous one, from `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public void setPixel(int x, int y, double[] dArray) {
        this.checkPoint(x, y);
        this.sampleModel.setPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, dArray, this.dataBuffer);
    }

    /**
     * Writes the pixels of a rectangle.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public void setPixels(int x, int y, int w, int h, int[] iArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, iArray, this.dataBuffer);
    }

    /**
     * Like the previous one, from `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public void setPixels(int x, int y, int w, int h, float[] fArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, fArray, this.dataBuffer);
    }

    /**
     * Like the previous one, from `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public void setPixels(int x, int y, int w, int h, double[] dArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, dArray, this.dataBuffer);
    }

    /**
     * Writes one band of a pixel.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public void setSample(int x, int y, int b, int s) {
        this.checkPoint(x, y);
        this.sampleModel.setSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, s, this.dataBuffer);
    }

    /**
     * Like the previous one, from `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public void setSample(int x, int y, int b, float s) {
        this.checkPoint(x, y);
        this.sampleModel.setSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, s, this.dataBuffer);
    }

    /**
     * Like the previous one, from `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public void setSample(int x, int y, int b, double s) {
        this.checkPoint(x, y);
        this.sampleModel.setSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, s, this.dataBuffer);
    }

    /**
     * Writes one band over a rectangle.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public void setSamples(int x, int y, int w, int h, int b, int[] iArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, iArray, this.dataBuffer);
    }

    /**
     * Like the previous one, from `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public void setSamples(int x, int y, int w, int h, int b, float[] fArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, fArray, this.dataBuffer);
    }

    /**
     * Like the previous one, from `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public void setSamples(int x, int y, int w, int h, int b, double[] dArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, dArray, this.dataBuffer);
    }
}
