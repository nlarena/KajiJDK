package javax.imageio.plugins.bmp;

import java.util.Locale;
import javax.imageio.ImageWriteParam;

/**
 * KajiLibrary's javax.imageio.plugins.bmp.BMPImageWriteParam -- BMP's own parameters.
 *
 * <p>It adds a single thing to {@link ImageWriteParam}: {@link #setTopDown}.
 *
 * <h2>BMP stores the rows upside down</h2>
 *
 * <p>A classic BMP stores the image <b>from bottom to top</b>: the first row of the file is the
 * last one on screen. It is a convention inherited from the device-independent bitmaps of Windows
 * and OS/2, and it is still the normal one.
 *
 * <p>The format also allows the natural order, marked with a negative height in the header. It is
 * faster to draw and quite a bit less compatible: some old readers do not understand it.
 *
 * <p>That is why the default is <b>false</b> --bottom to top--, which is the interoperable one.
 * Setting it to true is an optimization to decide on consciously.
 *
 * <p>The six compression types the JDK version declares are the format's: {@code BI_RGB}
 * uncompressed, the two {@code BI_RLE}, {@code BI_BITFIELDS}, and the two that wrap a whole other
 * format inside the BMP.
 */
public class BMPImageWriteParam extends ImageWriteParam {

    /** Whether the rows go in natural order. See the class note. */
    private boolean topDown = false;

    /**
     * @param locale which locale to give the texts in, or null
     */
    public BMPImageWriteParam(Locale locale) {
        super(locale);
        // BMP compresses, and with several methods: it has to be declared so that the
        // setCompressionXxx methods of the base class do not reject everything.
        this.canWriteCompressed = true;
        this.compressionTypes = new String[] {
            "BI_RGB", "BI_RLE8", "BI_RLE4", "BI_BITFIELDS", "BI_JPEG", "BI_PNG",
        };
    }

    /** Without a locale. */
    public BMPImageWriteParam() {
        this(null);
    }

    /** Whether to write the rows in natural order. See the class note. */
    public void setTopDown(boolean topDown) {
        this.topDown = topDown;
    }

    /** Whether they go in natural order. */
    public boolean isTopDown() {
        return this.topDown;
    }
}
