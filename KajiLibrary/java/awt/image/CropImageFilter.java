package java.awt.image;

import java.awt.Rectangle;
import java.util.Hashtable;

/**
 * Un filtro que deja pasar sólo un rectángulo de la imagen.
 *
 * <p>Corre el origen: el ángulo del recorte pasa a ser el (0,0) de lo que sale. Las tandas que caen
 * enteras afuera se descartan y las que caen a medias se recortan, sin copiar nada — se le pasa al
 * consumidor un desplazamiento distinto dentro del mismo arreglo.
 */
public class CropImageFilter extends ImageFilter {

    private final int cropX;
    private final int cropY;
    private final int cropW;
    private final int cropH;

    /** Con el rectángulo a recortar. */
    public CropImageFilter(int x, int y, int w, int h) {
        this.cropX = x;
        this.cropY = y;
        this.cropW = w;
        this.cropH = h;
    }

    /** Reenvía las propiedades, agregando el rectángulo recortado. */
    public void setProperties(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = copiar(props);
        p.put("croprect", new Rectangle(this.cropX, this.cropY, this.cropW, this.cropH));
        super.setProperties(p);
    }

    /** Anuncia el tamaño del recorte, no el de la imagen original. */
    public void setDimensions(int w, int h) {
        this.consumer.setDimensions(this.cropW, this.cropH);
    }

    /** Una suma que no da la vuelta: se satura en vez de desbordar. */
    private static int sumaSinDesborde(int x, int w) {
        int x2 = x + w;
        if (x > 0 && w > 0 && x2 < 0) {
            return Integer.MAX_VALUE;
        }
        return x2;
    }

    /** Deja pasar la parte de la tanda que cae en el recorte, con las coordenadas corridas. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        int x1 = x;
        if (x1 < this.cropX) {
            x1 = this.cropX;
        }
        int x2 = sumaSinDesborde(x, w);
        if (x2 > this.cropX + this.cropW) {
            x2 = this.cropX + this.cropW;
        }
        int y1 = y;
        if (y1 < this.cropY) {
            y1 = this.cropY;
        }
        int y2 = sumaSinDesborde(y, h);
        if (y2 > this.cropY + this.cropH) {
            y2 = this.cropY + this.cropH;
        }
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        this.consumer.setPixels(x1 - this.cropX, y1 - this.cropY, x2 - x1, y2 - y1, model, pixels,
                off + (y1 - y) * scansize + (x1 - x), scansize);
    }

    /** Lo mismo para píxeles de un `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        int x1 = x;
        if (x1 < this.cropX) {
            x1 = this.cropX;
        }
        int x2 = sumaSinDesborde(x, w);
        if (x2 > this.cropX + this.cropW) {
            x2 = this.cropX + this.cropW;
        }
        int y1 = y;
        if (y1 < this.cropY) {
            y1 = this.cropY;
        }
        int y2 = sumaSinDesborde(y, h);
        if (y2 > this.cropY + this.cropH) {
            y2 = this.cropY + this.cropH;
        }
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        this.consumer.setPixels(x1 - this.cropX, y1 - this.cropY, x2 - x1, y2 - y1, model, pixels,
                off + (y1 - y) * scansize + (x1 - x), scansize);
    }
}
