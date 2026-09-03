package java.awt.image;

import java.util.Hashtable;

/**
 * Escala una imagen **repitiendo y salteando** píxeles.
 *
 * <p>Es el escalado más barato que hay: para cada píxel del destino se elige el píxel del origen que
 * le queda más cerca, y listo. Al ampliar salen bloques y al achicar se pierden detalles enteros —un
 * cable de un píxel de ancho puede desaparecer del todo—, pero no hace ni una multiplicación.
 *
 * <p>La correspondencia entre columnas y filas se calcula una sola vez, en dos tablas. Que estén
 * **redondeadas al centro** y no truncadas es lo que evita que la imagen se corra medio píxel:
 * `srccols[dx]` es el origen del centro del píxel de destino, no el de su borde izquierdo.
 *
 * <p>Con una de las dos medidas negativa se calcula a partir de la otra manteniendo la proporción,
 * y eso recién se puede hacer cuando se conoce el tamaño del origen: por eso pasa en
 * {@link #setDimensions} y no en el constructor.
 */
public class ReplicateScaleFilter extends ImageFilter {

    /** Ancho del origen. */
    protected int srcWidth;

    /** Alto del origen. */
    protected int srcHeight;

    /** Ancho del destino. */
    protected int destWidth;

    /** Alto del destino. */
    protected int destHeight;

    /** Para cada fila del destino, de qué fila del origen sale. */
    protected int[] srcrows;

    /** Para cada columna del destino, de qué columna del origen sale. */
    protected int[] srccols;

    /** El arreglo reusado para armar cada fila de salida. */
    protected Object outpixbuf;

    /**
     * Con el tamaño de destino.
     *
     * @throws IllegalArgumentException si alguna de las dos medidas es cero
     */
    public ReplicateScaleFilter(int width, int height) {
        if (width == 0 || height == 0) {
            throw new IllegalArgumentException("Width (" + width + ") and height (" + height
                    + ") must be non-zero");
        }
        this.destWidth = width;
        this.destHeight = height;
    }

    /** Reenvía las propiedades, dejando constancia del escalado. */
    public void setProperties(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = copiar(props);
        String key = "rescale";
        String val = this.destWidth + "x" + this.destHeight;
        Object o = p.get(key);
        if (o != null && o instanceof String) {
            val = ((String) o) + ", " + val;
        }
        p.put(key, val);
        super.setProperties(p);
    }

    /** Guarda el tamaño del origen y resuelve las medidas de destino que faltaban. */
    public void setDimensions(int w, int h) {
        this.srcWidth = w;
        this.srcHeight = h;
        if (this.destWidth < 0) {
            if (this.destHeight < 0) {
                this.destWidth = this.srcWidth;
                this.destHeight = this.srcHeight;
            } else {
                this.destWidth = this.srcWidth * this.destHeight / this.srcHeight;
                if (this.destWidth == 0) {
                    this.destWidth = 1;
                }
            }
        } else if (this.destHeight < 0) {
            this.destHeight = this.srcHeight * this.destWidth / this.srcWidth;
            if (this.destHeight == 0) {
                this.destHeight = 1;
            }
        }
        this.consumer.setDimensions(this.destWidth, this.destHeight);
    }

    /**
     * Arma las dos tablas de correspondencia.
     *
     * <p>La cuenta `(2*d*src + src) / (2*dest)` es el origen del **centro** del píxel de destino: el
     * `+ src` de arriba es medio píxel de destino llevado a unidades de origen.
     */
    private void calcularTablas() {
        this.srcrows = new int[this.destHeight + 1];
        for (int y = 0; y <= this.destHeight; y++) {
            this.srcrows[y] = (2 * y * this.srcHeight + this.srcHeight) / (2 * this.destHeight);
        }
        this.srccols = new int[this.destWidth + 1];
        for (int x = 0; x <= this.destWidth; x++) {
            this.srccols[x] = (2 * x * this.srcWidth + this.srcWidth) / (2 * this.destWidth);
        }
    }

    /** Reparte una tanda de píxeles de un byte a las filas de destino que le tocan. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (this.srcrows == null || this.srccols == null) {
            this.calcularTablas();
        }
        int dx1 = (2 * x * this.destWidth + this.srcWidth - 1) / (2 * this.srcWidth);
        int dy1 = (2 * y * this.destHeight + this.srcHeight - 1) / (2 * this.srcHeight);
        byte[] outpix;
        if (this.outpixbuf != null && this.outpixbuf instanceof byte[]) {
            outpix = (byte[]) this.outpixbuf;
        } else {
            outpix = new byte[this.destWidth];
            this.outpixbuf = outpix;
        }
        int sy;
        int sx;
        for (int dy = dy1; dy < this.destHeight && (sy = this.srcrows[dy]) < y + h; dy++) {
            int srcoff = off + scansize * (sy - y);
            int dx;
            for (dx = dx1; dx < this.destWidth && (sx = this.srccols[dx]) < x + w; dx++) {
                outpix[dx] = pixels[srcoff + sx - x];
            }
            if (dx > dx1) {
                this.consumer.setPixels(dx1, dy, dx - dx1, 1, model, outpix, dx1, this.destWidth);
            }
        }
    }

    /** Lo mismo para píxeles de un `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (this.srcrows == null || this.srccols == null) {
            this.calcularTablas();
        }
        int dx1 = (2 * x * this.destWidth + this.srcWidth - 1) / (2 * this.srcWidth);
        int dy1 = (2 * y * this.destHeight + this.srcHeight - 1) / (2 * this.srcHeight);
        int[] outpix;
        if (this.outpixbuf != null && this.outpixbuf instanceof int[]) {
            outpix = (int[]) this.outpixbuf;
        } else {
            outpix = new int[this.destWidth];
            this.outpixbuf = outpix;
        }
        int sy;
        int sx;
        for (int dy = dy1; dy < this.destHeight && (sy = this.srcrows[dy]) < y + h; dy++) {
            int srcoff = off + scansize * (sy - y);
            int dx;
            for (dx = dx1; dx < this.destWidth && (sx = this.srccols[dx]) < x + w; dx++) {
                outpix[dx] = pixels[srcoff + sx - x];
            }
            if (dx > dx1) {
                this.consumer.setPixels(dx1, dy, dx - dx1, 1, model, outpix, dx1, this.destWidth);
            }
        }
    }
}
