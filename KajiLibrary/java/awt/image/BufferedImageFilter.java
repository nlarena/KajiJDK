package java.awt.image;

/**
 * El adaptador entre las dos formas de filtrar imágenes que tiene AWT.
 *
 * <p>{@link ImageFilter} trabaja de a tandas, sobre una imagen que va llegando;
 * {@link BufferedImageOp} trabaja sobre la imagen entera de una vez. Esta clase junta las dos: se
 * comporta como un filtro de tubería, guarda la imagen completa mientras llega, y recién al final
 * aplica la operación y entrega el resultado.
 *
 * <p>Ese "recién al final" es la parte que hay que tener presente. Una operación como una
 * convolución **necesita** los vecinos de cada píxel, así que no puede empezar hasta tener todo, y
 * por eso este filtro guarda la imagen entera en memoria: es lo que pide la operación que envuelve,
 * no un descuido.
 */
public class BufferedImageFilter extends ImageFilter implements Cloneable {

    private final BufferedImageOp bufferedImageOp;
    private ColorModel model;
    private int width;
    private int height;
    private byte[] bytePixels;
    private int[] intPixels;

    /**
     * Con la operación que va a aplicar.
     *
     * @throws NullPointerException si la operación es `null`
     */
    public BufferedImageFilter(BufferedImageOp op) {
        if (op == null) {
            throw new NullPointerException("Operation cannot be null");
        }
        this.bufferedImageOp = op;
    }

    /** La operación que aplica. */
    public BufferedImageOp getBufferedImageOp() {
        return this.bufferedImageOp;
    }

    /**
     * Anota el tamaño y reserva la imagen.
     *
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public void setDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            this.width = 0;
            this.height = 0;
            this.consumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
            return;
        }
        this.width = width;
        this.height = height;
    }

    /** Anota el modelo de color con el que van a venir los píxeles. */
    public void setColorModel(ColorModel model) {
        this.model = model;
    }

    /** Guarda una tanda de píxeles de un byte. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (this.width == 0 || this.height == 0) {
            return;
        }
        if (this.bytePixels == null && this.intPixels == null) {
            this.bytePixels = new byte[this.width * this.height];
            this.model = model;
        }
        if (this.bytePixels != null && this.model == model) {
            for (int cy = 0; cy < h; cy++) {
                System.arraycopy(pixels, off + cy * scansize, this.bytePixels,
                        (y + cy) * this.width + x, w);
            }
            return;
        }
        // Llego una tanda con otro modelo: no hay uno solo que describa a las dos, asi que se pasa
        // todo a ARGB, que es el unico comun.
        this.aRGB();
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                this.intPixels[(y + cy) * this.width + x + cx] =
                        model.getRGB(pixels[off + cy * scansize + cx] & 0xFF);
            }
        }
    }

    /** Guarda una tanda de píxeles de un `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (this.width == 0 || this.height == 0) {
            return;
        }
        if (this.intPixels == null) {
            this.aRGB();
        }
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                this.intPixels[(y + cy) * this.width + x + cx] =
                        model.getRGB(pixels[off + cy * scansize + cx]);
            }
        }
    }

    /** Pasa lo que se haya juntado a ARGB. */
    private void aRGB() {
        int[] nuevo = new int[this.width * this.height];
        if (this.bytePixels != null && this.model != null) {
            for (int i = 0; i < nuevo.length; i++) {
                nuevo[i] = this.model.getRGB(this.bytePixels[i] & 0xFF);
            }
        }
        this.bytePixels = null;
        this.intPixels = nuevo;
        this.model = ColorModel.getRGBdefault();
    }

    /**
     * Arma la imagen, le aplica la operación y entrega el resultado.
     *
     * <p>Con un estado de error o de aborto no se aplica nada: la imagen está incompleta y filtrar
     * una imagen a medias daría un resultado que no es el de nadie.
     */
    public void imageComplete(int status) {
        if (status == ImageConsumer.IMAGEERROR || status == ImageConsumer.IMAGEABORTED) {
            this.consumer.imageComplete(status);
            return;
        }
        if (this.width == 0 || this.height == 0) {
            this.consumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
            return;
        }
        BufferedImage entrada;
        if (this.intPixels != null) {
            entrada = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
            entrada.setRGB(0, 0, this.width, this.height, this.intPixels, 0, this.width);
        } else {
            this.aRGB();
            entrada = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
            entrada.setRGB(0, 0, this.width, this.height, this.intPixels, 0, this.width);
        }
        BufferedImage salida = this.bufferedImageOp.filter(entrada, null);
        int w = salida.getWidth();
        int h = salida.getHeight();
        int[] fila = new int[w];
        ColorModel rgb = ColorModel.getRGBdefault();
        this.consumer.setDimensions(w, h);
        this.consumer.setColorModel(rgb);
        this.consumer.setHints(ImageConsumer.TOPDOWNLEFTRIGHT
                | ImageConsumer.COMPLETESCANLINES | ImageConsumer.SINGLEPASS
                | ImageConsumer.SINGLEFRAME);
        for (int y = 0; y < h; y++) {
            salida.getRGB(0, y, w, 1, fila, 0, w);
            this.consumer.setPixels(0, y, w, 1, rgb, fila, 0, w);
        }
        this.consumer.imageComplete(status);
    }
}
