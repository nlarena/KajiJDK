package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Multiplica y suma: `destino = origen * escala + corrimiento`, banda por banda.
 *
 * <p>Es la operación de brillo y contraste. La escala abre o cierra el rango —contraste— y el
 * corrimiento lo mueve entero —brillo—. Con una sola constante se aplica a todas las bandas; con
 * varias, una por banda.
 *
 * <p>Trabaja sobre los valores **tal como están guardados**, no sobre 0..1, así que una escala de 2
 * sobre una banda de ocho bits lleva 100 a 200 y 200 a 255 recortado. Ese recorte es la parte que se
 * ve: subir el brillo aplasta las luces contra el techo y esa información no vuelve.
 *
 * <p>Sobre una {@link BufferedImage} el **alfa no se toca** salvo que se den tantas constantes como
 * componentes tenga el modelo, contando el alfa. Es lo razonable: subirle el brillo a una imagen no
 * debería volverla opaca. Sobre un {@link Raster} no hay modelo de color que consultar y todas las
 * bandas son iguales, así que se escalan todas.
 */
public class RescaleOp implements BufferedImageOp, RasterOp {

    private final float[] scaleFactors;
    private final float[] offsets;
    private final int length;
    private final RenderingHints hints;

    /**
     * Con una constante por banda.
     *
     * @throws IllegalArgumentException si los dos arreglos no miden lo mismo
     */
    public RescaleOp(float[] scaleFactors, float[] offsets, RenderingHints hints) {
        this.length = scaleFactors.length;
        if (this.length != offsets.length) {
            throw new IllegalArgumentException("Number of scaling factors does not equal the "
                    + "number of offsets");
        }
        this.scaleFactors = new float[this.length];
        this.offsets = new float[this.length];
        for (int i = 0; i < this.length; i++) {
            this.scaleFactors[i] = scaleFactors[i];
            this.offsets[i] = offsets[i];
        }
        this.hints = hints;
    }

    /** Con la misma constante para todas las bandas. */
    public RescaleOp(float scaleFactor, float offset, RenderingHints hints) {
        this.length = 1;
        this.scaleFactors = new float[1];
        this.offsets = new float[1];
        this.scaleFactors[0] = scaleFactor;
        this.offsets[0] = offset;
        this.hints = hints;
    }

    /**
     * Las escalas.
     *
     * @param scaleFactors dónde escribirlas, o `null` para que se cree el arreglo
     * @throws IllegalArgumentException si el arreglo dado es más corto
     */
    public final float[] getScaleFactors(float[] scaleFactors) {
        float[] out = scaleFactors;
        if (out == null) {
            out = new float[this.length];
        }
        System.arraycopy(this.scaleFactors, 0, out, 0, Math.min(this.length, out.length));
        return out;
    }

    /**
     * Los corrimientos.
     *
     * @param offsets dónde escribirlos, o `null` para que se cree el arreglo
     */
    public final float[] getOffsets(float[] offsets) {
        float[] out = offsets;
        if (out == null) {
            out = new float[this.length];
        }
        System.arraycopy(this.offsets, 0, out, 0, Math.min(this.length, out.length));
        return out;
    }

    /** Cuántas constantes hay. */
    public final int getNumFactors() {
        return this.length;
    }

    /** La constante que le toca a esa banda. */
    private float escala(int b) {
        return this.length == 1 ? this.scaleFactors[0] : this.scaleFactors[b];
    }

    /** El corrimiento que le toca a esa banda. */
    private float corrimiento(int b) {
        return this.length == 1 ? this.offsets[0] : this.offsets[b];
    }

    /**
     * Aplica la operación a una imagen.
     *
     * <p>Con alfa premultiplicado el origen se lleva primero a no premultiplicado: escalar un color
     * que ya está multiplicado por su alfa daría un resultado que depende de la transparencia, que
     * no es lo que se pide.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si el origen tiene paleta, si los tamaños no coinciden, o si
     *     la cantidad de constantes no es 1 ni la cantidad de componentes
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        ColorModel srcCM = src.getColorModel();
        if (srcCM instanceof IndexColorModel) {
            throw new IllegalArgumentException(
                    "Rescaling cannot be performed on an indexed image");
        }
        int numColores = srcCM.getNumColorComponents();
        int numTodas = srcCM.getNumComponents();
        if (this.length != 1 && this.length != numColores && this.length != numTodas) {
            throw new IllegalArgumentException("Number of scaling constants does not equal the "
                    + "number of of color or color/alpha components");
        }
        BufferedImage destino = dst;
        if (destino == null) {
            destino = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != destino.getWidth()
                || src.getHeight() != destino.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        // Se trabaja siempre sin premultiplicar y se vuelve a premultiplicar al final si el destino
        // lo pide: son dos conversiones de mas en el peor caso, y la unica forma de que la cuenta
        // signifique lo mismo en las cuatro combinaciones de origen y destino.
        BufferedImage origen = src;
        if (srcCM.isAlphaPremultiplied()) {
            origen = this.copiaSinPremultiplicar(src);
        }
        boolean tocarAlfa = this.length == numTodas && srcCM.hasAlpha();
        int bandas = tocarAlfa ? numTodas : numColores;
        this.escalarRaster(origen.getRaster(), destino.getRaster(), bandas);
        if (!tocarAlfa && srcCM.hasAlpha() && destino.getColorModel().hasAlpha()) {
            this.copiarAlfa(origen.getRaster(), destino.getRaster(), numColores);
        }
        if (destino.getColorModel().isAlphaPremultiplied()) {
            destino.coerceData(true);
        }
        return destino;
    }

    /** Una copia de la imagen con el color sin premultiplicar. */
    private BufferedImage copiaSinPremultiplicar(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(), src.getHeight());
        BufferedImage copia = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
        copia.setData(src.getRaster());
        copia.coerceData(false);
        return copia;
    }

    /** Copia la banda de alfa sin tocarla. */
    private void copiarAlfa(Raster src, WritableRaster dst, int aIdx) {
        int w = Math.min(src.getWidth(), dst.getWidth());
        int h = Math.min(src.getHeight(), dst.getHeight());
        int[] fila = new int[w];
        for (int y = 0; y < h; y++) {
            fila = src.getSamples(src.getMinX(), src.getMinY() + y, w, 1, aIdx, fila);
            dst.setSamples(dst.getMinX(), dst.getMinY() + y, w, 1, aIdx, fila);
        }
    }

    /**
     * Aplica la operación a un ráster.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si los tamaños o la cantidad de bandas no coinciden, o si la
     *     cantidad de constantes no es 1 ni la cantidad de bandas
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        int numBands = src.getNumBands();
        if (this.length != 1 && this.length != numBands) {
            throw new IllegalArgumentException("Number of rasterBands (" + numBands
                    + ") does not match number of scale factors (" + this.length + ")");
        }
        WritableRaster destino = dst;
        if (destino == null) {
            destino = this.createCompatibleDestRaster(src);
        } else {
            if (src.getNumBands() != destino.getNumBands()) {
                throw new IllegalArgumentException("Number of src bands (" + src.getNumBands()
                        + ") does not match number of dst bands (" + destino.getNumBands() + ")");
            }
            if (src.getWidth() != destino.getWidth() || src.getHeight() != destino.getHeight()) {
                throw new IllegalArgumentException(
                        "Width or height of Rasters do not match");
            }
        }
        this.escalarRaster(src, destino, numBands);
        return destino;
    }

    /** El mayor valor que admite esa banda del ráster. */
    private static int maximo(Raster r, int b) {
        return (1 << r.getSampleModel().getSampleSize(b)) - 1;
    }

    /** Aplica la cuenta a las primeras `bandas` bandas. */
    private void escalarRaster(Raster src, WritableRaster dst, int bandas) {
        int w = src.getWidth();
        int h = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = dst.getMinX();
        int dy = dst.getMinY();
        int tipo = src.getSampleModel().getDataType();
        boolean flotante = tipo == DataBuffer.TYPE_FLOAT || tipo == DataBuffer.TYPE_DOUBLE;
        if (flotante) {
            double[] fila = new double[w];
            for (int b = 0; b < bandas; b++) {
                double esc = this.escala(b);
                double corr = this.corrimiento(b);
                for (int y = 0; y < h; y++) {
                    fila = src.getSamples(sx, sy + y, w, 1, b, fila);
                    for (int i = 0; i < w; i++) {
                        fila[i] = fila[i] * esc + corr;
                    }
                    dst.setSamples(dx, dy + y, w, 1, b, fila);
                }
            }
            return;
        }
        int[] fila = new int[w];
        for (int b = 0; b < bandas; b++) {
            float esc = this.escala(b);
            float corr = this.corrimiento(b);
            int max = maximo(dst, b);
            for (int y = 0; y < h; y++) {
                fila = src.getSamples(sx, sy + y, w, 1, b, fila);
                for (int i = 0; i < w; i++) {
                    int v = (int) (fila[i] * esc + corr + 0.5f);
                    if (v < 0) {
                        v = 0;
                    } else if (v > max) {
                        v = max;
                    }
                    fila[i] = v;
                }
                dst.setSamples(dx, dy + y, w, 1, b, fila);
            }
        }
    }

    /**
     * Una imagen vacía del tamaño y formato que corresponde.
     *
     * @throws IllegalArgumentException si el origen tiene paleta y no se da otro modelo de color
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel cm = destCM;
        if (cm == null) {
            cm = src.getColorModel();
            if (cm instanceof IndexColorModel) {
                throw new IllegalArgumentException(
                        "Rescaling cannot be performed on an indexed image");
            }
        }
        int w = src.getWidth();
        int h = src.getHeight();
        WritableRaster wr = cm.createCompatibleWritableRaster(w, h);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /** Un ráster vacío del mismo tamaño y disposición. */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        return src.createCompatibleWritableRaster(src.getWidth(), src.getHeight());
    }

    /** El mismo rectángulo: esta operación no mueve nada de lugar. */
    public final Rectangle2D getBounds2D(BufferedImage src) {
        return this.getBounds2D(src.getRaster());
    }

    /** El mismo rectángulo. */
    public final Rectangle2D getBounds2D(Raster src) {
        return src.getBounds();
    }

    /** El mismo punto. */
    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        Point2D out = dstPt;
        if (out == null) {
            out = new java.awt.geom.Point2D.Float();
        }
        out.setLocation(srcPt.getX(), srcPt.getY());
        return out;
    }

    /** Las pistas de dibujo, o `null` si no hay. */
    public final RenderingHints getRenderingHints() {
        return this.hints;
    }
}
