package java.awt.image;

/**
 * Un filtro que trabaja **color por color**, sin mirar a los vecinos.
 *
 * <p>La subclase sólo escribe {@link #filterRGB}: una función de un color a otro. Todo lo demás
 * —desempaquetar el píxel, convertirlo a ARGB, volver a empaquetarlo— lo pone esta clase.
 *
 * <p>Y hay un atajo que vale la pena entender, porque es la razón de ser de casi toda la clase. Si
 * la imagen viene con paleta y la función no depende de la posición, no hace falta filtrar los
 * píxeles: alcanza con filtrar **la paleta**, que son 256 colores, y dejar los píxeles como están.
 * Una imagen de un millón de píxeles se filtra con 256 llamadas.
 *
 * <p>Eso es lo que declara {@link #canFilterIndexColorModel}, y la subclase tiene que ponerlo en
 * `true` sólo si su función ignora de verdad las coordenadas. Si las usa y activa el atajo, todos
 * los píxeles del mismo índice reciben el color que le tocó a las coordenadas con las que se filtró
 * la paleta, y el resultado no se parece a nada.
 */
public abstract class RGBImageFilter extends ImageFilter {

    /** El modelo de color que anunció el productor. */
    protected ColorModel origmodel;

    /** El modelo con el que se lo reemplaza. */
    protected ColorModel newmodel;

    /**
     * Si alcanza con filtrar la paleta en vez de los píxeles.
     *
     * <p>Sólo puede ser `true` si {@link #filterRGB} ignora las coordenadas.
     */
    protected boolean canFilterIndexColorModel;

    /** Para las subclases. */
    protected RGBImageFilter() {
    }

    /**
     * La función de color, que es todo lo que la subclase tiene que escribir.
     *
     * @param x la coordenada, o -1 si el color viene de una paleta
     * @param y lo mismo
     * @param rgb el color de entrada, en ARGB
     * @return el color de salida, en ARGB
     */
    public abstract int filterRGB(int x, int y, int rgb);

    /**
     * Anuncia el modelo de color, aplicando el atajo de la paleta si corresponde.
     *
     * <p>Cuando el atajo se activa, lo que llega al consumidor es una paleta **ya filtrada** y los
     * píxeles sin tocar.
     */
    public void setColorModel(ColorModel model) {
        if (this.canFilterIndexColorModel && model instanceof IndexColorModel) {
            ColorModel newcm = this.filterIndexColorModel((IndexColorModel) model);
            this.substituteColorModel(model, newcm);
            this.consumer.setColorModel(newcm);
        } else {
            this.consumer.setColorModel(ColorModel.getRGBdefault());
        }
    }

    /** Anota que un modelo se reemplaza por otro. */
    public void substituteColorModel(ColorModel oldcm, ColorModel newcm) {
        this.origmodel = oldcm;
        this.newmodel = newcm;
    }

    /**
     * La misma paleta con todos sus colores pasados por {@link #filterRGB}.
     *
     * <p>Las coordenadas que se le pasan son -1: un color de paleta no está en ningún lado en
     * particular, y pasarle un punto cualquiera sería mentirle a la función.
     */
    public IndexColorModel filterIndexColorModel(IndexColorModel icm) {
        int mapsize = icm.getMapSize();
        byte[] r = new byte[mapsize];
        byte[] g = new byte[mapsize];
        byte[] b = new byte[mapsize];
        byte[] a = new byte[mapsize];
        icm.getReds(r);
        icm.getGreens(g);
        icm.getBlues(b);
        icm.getAlphas(a);
        int trans = icm.getTransparentPixel();
        boolean needalpha = false;
        for (int i = 0; i < mapsize; i++) {
            int rgb = this.filterRGB(-1, -1, icm.getRGB(i));
            a[i] = (byte) (rgb >> 24);
            if (a[i] != ((byte) 0xFF) && i != trans) {
                needalpha = true;
            }
            r[i] = (byte) (rgb >> 16);
            g[i] = (byte) (rgb >> 8);
            b[i] = (byte) rgb;
        }
        if (needalpha) {
            return new IndexColorModel(icm.getPixelSize(), mapsize, r, g, b, a);
        }
        return new IndexColorModel(icm.getPixelSize(), mapsize, r, g, b, trans);
    }

    /** Pasa una tanda de píxeles por la función, uno por uno. */
    public void filterRGBPixels(int x, int y, int w, int h, int[] pixels, int off, int scansize) {
        int index = off;
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                pixels[index] = this.filterRGB(x + cx, y + cy, pixels[index]);
                index = index + 1;
            }
            index = index + scansize - w;
        }
        this.consumer.setPixels(x, y, w, h, ColorModel.getRGBdefault(), pixels, off, scansize);
    }

    /**
     * Reenvía una tanda de píxeles de un byte.
     *
     * <p>Si el modelo es el que se reemplazó, los píxeles pasan **sin tocar**: la paleta ya se
     * filtró y filtrarlos de nuevo aplicaría la función dos veces.
     */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (model == this.origmodel) {
            this.consumer.setPixels(x, y, w, h, this.newmodel, pixels, off, scansize);
            return;
        }
        int[] filtered = new int[w];
        int index = off;
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                filtered[cx] = model.getRGB(pixels[index + cx] & 0xFF);
            }
            this.filterRGBPixels(x, y + cy, w, 1, filtered, 0, w);
            index = index + scansize;
        }
    }

    /** Lo mismo para píxeles de un `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (model == this.origmodel) {
            this.consumer.setPixels(x, y, w, h, this.newmodel, pixels, off, scansize);
            return;
        }
        int[] filtered = new int[w];
        int index = off;
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                filtered[cx] = model.getRGB(pixels[index + cx]);
            }
            this.filterRGBPixels(x, y + cy, w, 1, filtered, 0, w);
            index = index + scansize;
        }
    }
}
