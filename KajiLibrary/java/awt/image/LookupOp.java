package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Reemplaza cada valor de banda por el que dice una tabla.
 *
 * <p>Es la operación más general de las que trabajan píxel a píxel sin mirar a los vecinos: la tabla
 * puede describir cualquier función de un valor a otro. Invertir una imagen, aplicar una curva de
 * gama, posterizar, umbralizar: todas son la misma operación con distinta tabla.
 *
 * <p>Y es rápida justamente por eso: no hay cuenta que hacer por píxel, sólo un acceso a un arreglo.
 * La curva se calcula una vez al armar la tabla.
 *
 * <p>Como en {@link RescaleOp}, sobre una {@link BufferedImage} el **alfa no se toca** salvo que la
 * tabla tenga tantos arreglos como componentes tenga el modelo contando el alfa.
 */
public class LookupOp implements BufferedImageOp, RasterOp {

    private final LookupTable ltable;
    private final int numComponents;
    private final RenderingHints hints;

    /**
     * Con la tabla dada.
     *
     * @throws NullPointerException si la tabla es `null`
     */
    public LookupOp(LookupTable lookup, RenderingHints hints) {
        this.ltable = lookup;
        this.numComponents = this.ltable.getNumComponents();
        this.hints = hints;
    }

    /** La tabla. */
    public final LookupTable getTable() {
        return this.ltable;
    }

    /**
     * Aplica la operación a una imagen.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si el origen tiene paleta, si los tamaños no coinciden, o si
     *     la tabla no tiene 1 arreglo ni tantos como componentes
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        ColorModel srcCM = src.getColorModel();
        if (srcCM instanceof IndexColorModel) {
            throw new IllegalArgumentException("LookupOp cannot be "
                    + "performed on an indexed image");
        }
        int numColores = srcCM.getNumColorComponents();
        int numTodas = srcCM.getNumComponents();
        if (this.numComponents != 1 && this.numComponents != numColores
                && this.numComponents != numTodas) {
            throw new IllegalArgumentException("Number of arrays in the  lookup table ("
                    + this.numComponents + ") is not compatible with the  src image: " + src);
        }
        BufferedImage destino = dst;
        if (destino == null) {
            destino = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != destino.getWidth()
                || src.getHeight() != destino.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        BufferedImage origen = src;
        if (srcCM.isAlphaPremultiplied()) {
            ColorModel cm = srcCM;
            WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(),
                    src.getHeight());
            origen = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
            origen.setData(src.getRaster());
            origen.coerceData(false);
        }
        boolean tocarAlfa = this.numComponents == numTodas && srcCM.hasAlpha();
        int bandas = tocarAlfa ? numTodas : numColores;
        this.aplicar(origen.getRaster(), destino.getRaster(), bandas);
        if (!tocarAlfa && srcCM.hasAlpha() && destino.getColorModel().hasAlpha()) {
            int w = src.getWidth();
            int[] fila = new int[w];
            for (int y = 0; y < src.getHeight(); y++) {
                fila = origen.getRaster().getSamples(0, y, w, 1, numColores, fila);
                destino.getRaster().setSamples(0, y, w, 1, numColores, fila);
            }
        }
        if (destino.getColorModel().isAlphaPremultiplied()) {
            destino.coerceData(true);
        }
        return destino;
    }

    /**
     * Aplica la operación a un ráster.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si los tamaños o la cantidad de bandas no coinciden, o si la
     *     tabla no tiene 1 arreglo ni tantos como bandas
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        int numBands = src.getNumBands();
        if (this.numComponents != 1 && this.numComponents != numBands) {
            throw new IllegalArgumentException("Number of arrays in the  lookup table ("
                    + this.numComponents + ") is not compatible with the  src Raster: " + src);
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
                throw new IllegalArgumentException("Width or height of Rasters do not match");
            }
        }
        this.aplicar(src, destino, numBands);
        return destino;
    }

    /** Pasa las primeras `bandas` bandas por la tabla, píxel por píxel. */
    private void aplicar(Raster src, WritableRaster dst, int bandas) {
        int w = src.getWidth();
        int h = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = dst.getMinX();
        int dy = dst.getMinY();
        int[] pixel = new int[Math.max(src.getNumBands(), this.numComponents)];
        int[] salida = new int[pixel.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = src.getPixel(sx + x, sy + y, pixel);
                // Sólo se pasan por la tabla las bandas que corresponde; el resto se copia.
                int[] parcial = new int[bandas];
                System.arraycopy(pixel, 0, parcial, 0, bandas);
                int[] convertido = this.ltable.lookupPixel(parcial, null);
                System.arraycopy(pixel, 0, salida, 0, pixel.length);
                System.arraycopy(convertido, 0, salida, 0, bandas);
                dst.setPixel(dx + x, dy + y, salida);
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
                        "LookupOp cannot be performed on an indexed image");
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
