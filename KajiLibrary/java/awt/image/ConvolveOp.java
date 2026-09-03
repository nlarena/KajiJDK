package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Convolución: cada píxel de salida es una suma pesada de sus vecinos, con los pesos de un
 * {@link Kernel}.
 *
 * <p>Es la operación de la que salen casi todos los efectos que miran más de un píxel. Con un núcleo
 * de valores iguales que sumen 1, desenfoque; con uno que reste los vecinos al centro, realce; con
 * uno asimétrico, detección de bordes o relieve. El código es el mismo y cambia la tabla.
 *
 * <p>El **borde** es el problema real y por eso está declarado. Los píxeles del borde no tienen
 * todos sus vecinos, y hay dos respuestas posibles: {@link #EDGE_ZERO_FILL} los pone en cero, lo que
 * deja un marco oscuro; {@link #EDGE_NO_OP} los copia sin tocar, lo que deja un marco sin filtrar.
 * Ninguna es correcta —la información no está—, y elegir cuál mentira se prefiere es parte de la
 * operación.
 *
 * <p>El origen se lee entero antes de escribir el destino, así que **origen y destino pueden ser el
 * mismo**: cada salida depende de las entradas vecinas, y escribir sobre la entrada mientras se lee
 * contaminaría los píxeles siguientes.
 */
public class ConvolveOp implements BufferedImageOp, RasterOp {

    /** Los píxeles del borde salen en cero. */
    public static final int EDGE_ZERO_FILL = 0;

    /** Los píxeles del borde se copian sin filtrar. */
    public static final int EDGE_NO_OP = 1;

    private final Kernel kernel;
    private final int edgeHint;
    private final RenderingHints hints;

    /**
     * Con el núcleo, la condición de borde y las pistas.
     *
     * @throws NullPointerException si el núcleo es `null`
     */
    public ConvolveOp(Kernel kernel, int edgeCondition, RenderingHints hints) {
        this.kernel = kernel;
        this.edgeHint = edgeCondition;
        this.hints = hints;
    }

    /**
     * Con el núcleo y el borde en cero.
     *
     * @throws NullPointerException si el núcleo es `null`
     */
    public ConvolveOp(Kernel kernel) {
        this.kernel = kernel;
        this.edgeHint = EDGE_ZERO_FILL;
        this.hints = null;
    }

    /** Qué se hace con los píxeles del borde. */
    public int getEdgeCondition() {
        return this.edgeHint;
    }

    /** El núcleo. */
    public final Kernel getKernel() {
        return (Kernel) this.kernel.clone();
    }

    /**
     * Aplica la convolución a una imagen.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si el origen y el destino son el mismo objeto, si el origen
     *     tiene paleta, o si los tamaños no coinciden
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
        }
        ColorModel srcCM = src.getColorModel();
        if (srcCM instanceof IndexColorModel) {
            throw new IllegalArgumentException("ConvolveOp cannot be performed on an indexed "
                    + "image");
        }
        BufferedImage destino = dst;
        if (destino == null) {
            destino = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != destino.getWidth()
                || src.getHeight() != destino.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        // Se convoluciona con el alfa **premultiplicado**: sin premultiplicar, el color de un pixel
        // invisible pesaria lo mismo que el de uno opaco y sangraria sobre sus vecinos.
        BufferedImage origen = src;
        if (srcCM.hasAlpha() && !srcCM.isAlphaPremultiplied()) {
            ColorModel cm = srcCM;
            WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(),
                    src.getHeight());
            origen = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
            origen.setData(src.getRaster());
            origen.coerceData(true);
        }
        this.convolucionar(origen.getRaster(), destino.getRaster());
        if (destino.getColorModel().hasAlpha()
                && !destino.getColorModel().isAlphaPremultiplied()) {
            destino.coerceData(true);
            destino.coerceData(false);
        }
        return destino;
    }

    /**
     * Aplica la convolución a un ráster.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si el origen y el destino son el mismo objeto, o si los
     *     tamaños o la cantidad de bandas no coinciden
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        if (dst == src) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
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
        this.convolucionar(src, destino);
        return destino;
    }

    /** La cuenta, banda por banda. */
    private void convolucionar(Raster src, WritableRaster dst) {
        int w = src.getWidth();
        int h = src.getHeight();
        int bandas = Math.min(src.getNumBands(), dst.getNumBands());
        int kw = this.kernel.getWidth();
        int kh = this.kernel.getHeight();
        int kx = this.kernel.getXOrigin();
        int ky = this.kernel.getYOrigin();
        float[] pesos = this.kernel.getKernelData(null);
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = dst.getMinX();
        int dy = dst.getMinY();
        for (int b = 0; b < bandas; b++) {
            int max = (1 << dst.getSampleModel().getSampleSize(b)) - 1;
            int[] entrada = src.getSamples(sx, sy, w, h, b, (int[]) null);
            int[] salida = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean borde = x < kx || y < ky || x >= w - (kw - kx - 1)
                            || y >= h - (kh - ky - 1);
                    if (borde) {
                        if (this.edgeHint == EDGE_NO_OP) {
                            salida[y * w + x] = entrada[y * w + x];
                        } else {
                            salida[y * w + x] = 0;
                        }
                        continue;
                    }
                    float acum = 0.0f;
                    int k = 0;
                    for (int j = 0; j < kh; j++) {
                        int fy = y + j - ky;
                        for (int i = 0; i < kw; i++) {
                            int fx = x + i - kx;
                            acum = acum + pesos[k] * entrada[fy * w + fx];
                            k = k + 1;
                        }
                    }
                    int v = (int) (acum + 0.5f);
                    if (v < 0) {
                        v = 0;
                    } else if (v > max) {
                        v = max;
                    }
                    salida[y * w + x] = v;
                }
            }
            dst.setSamples(dx, dy, w, h, b, salida);
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
                        "ConvolveOp cannot be performed on an indexed image");
            }
        }
        int w = src.getWidth();
        int h = src.getHeight();
        WritableRaster wr = cm.createCompatibleWritableRaster(w, h);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /** Un ráster vacío del mismo tamaño y disposición. */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        return src.createCompatibleWritableRaster();
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
