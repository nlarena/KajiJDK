package javax.swing;

import java.awt.Image;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

/**
 * It turns an image into greys and raises its brightness: it is how a disabled icon looks.
 *
 * <h2>Two steps, not one</h2>
 *
 * <p>First it goes to grey, and afterwards it lightens or darkens. Grey alone would not be
 * enough: an icon in greys with the same contrast as the original still reads as active. What
 * says "this cannot be touched" is that it is also <em>washed out</em>, and the percentage
 * takes care of that.
 *
 * <h2>The conversion to grey is not the average</h2>
 *
 * <p>It is {@code 0.30 R + 0.59 G + 0.11 B}. The three numbers are not arbitrary: the eye is
 * much more sensitive to green than to blue, so averaging the three equally would give a grey
 * that looks wrong -- the greens come out too dark and the blues too light --.
 *
 * <p>Transparency is kept as it is: the three colour channels are filtered and the alpha passes
 * untouched. An icon with smoothed edges goes on having them.
 */
public class GrayFilter extends RGBImageFilter {

    private boolean brighter;
    private int percent;

    /**
     * That image's disabled version.
     *
     * <p>Lightening by 50%, which is what Swing uses for the icons of disabled buttons.
     */
    public static Image createDisabledImage(Image i) {
        GrayFilter filter = new GrayFilter(true, 50);
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        return java.awt.Toolkit.getDefaultToolkit().createImage(prod);
    }

    /**
     * A filter that lightens or darkens by that percentage.
     *
     * @param b true to lighten, false to darken
     * @param p by how much, from 0 to 100
     */
    public GrayFilter(boolean b, int p) {
        brighter = b;
        percent = p;
        // The filter does not look at a pixel's neighbours, so it can be applied to the colour
                // table instead of to each pixel: in an indexed image that is one pass of 256
                // instead of one of a million.
        canFilterIndexColorModel = true;
    }

    /**
     * That pixel's colour, already in grey and lightened.
     *
     * <p>See the class note: the weights are not equal, and the alpha passes untouched.
     */
    public int filterRGB(int x, int y, int rgb) {
        int gray = (int) ((0.30 * ((rgb >> 16) & 0xff)
                + 0.59 * ((rgb >> 8) & 0xff)
                + 0.11 * (rgb & 0xff)) / 3);
        if (brighter) {
            gray = (255 - ((255 - gray) * (100 - percent) / 100));
        } else {
            gray = (gray * (100 - percent) / 100);
        }
        if (gray < 0) {
            gray = 0;
        }
        if (gray > 255) {
            gray = 255;
        }
        return (rgb & 0xff000000) | (gray << 16) | (gray << 8) | (gray << 0);
    }
}
