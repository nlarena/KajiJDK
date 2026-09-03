package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Mueve los píxeles: rota, escala, inclina o traslada una imagen con una transformación afín.
 *
 * <p>Es la única operación del paquete que cambia **dónde** está cada píxel, y de ahí que sea la
 * única cuyo {@link #getPoint2D} hace algo interesante.
 *
 * <p>El recorrido va al revés de lo que uno esperaría. No se toma cada píxel del origen y se calcula
 * dónde cae —eso dejaría agujeros al ampliar y colisiones al achicar—, sino que se toma cada píxel
 * del **destino** y se calcula de dónde viene, invirtiendo la transformación. Por eso el constructor
 * exige que la transformación sea invertible: sin inversa no hay de dónde leer.
 *
 * <p>Ese punto de origen casi nunca cae justo en un píxel, y ahí entra la interpolación.
 * {@link #TYPE_NEAREST_NEIGHBOR} agarra el más cercano y es rápido y con escalones;
 * {@link #TYPE_BILINEAR} promedia los cuatro que lo rodean; {@link #TYPE_BICUBIC} usa dieciséis y
 * una curva que conserva mejor los bordes, a costa de poder pasarse del rango y necesitar recorte.
 */
public class AffineTransformOp implements BufferedImageOp, RasterOp {

    /** El píxel más cercano. */
    public static final int TYPE_NEAREST_NEIGHBOR = 1;

    /** Promedio pesado de los cuatro vecinos. */
    public static final int TYPE_BILINEAR = 2;

    /** Curva cúbica sobre dieciséis vecinos. */
    public static final int TYPE_BICUBIC = 3;

    private final AffineTransform xform;
    private final int interpolationType;
    private final RenderingHints hints;

    /**
     * Con el tipo de interpolación dado.
     *
     * @throws ImagingOpException si la transformación no es invertible
     * @throws IllegalArgumentException si el tipo de interpolación no es uno de los tres
     */
    public AffineTransformOp(AffineTransform xform, int interpolationType) {
        this.validar(xform);
        if (interpolationType != TYPE_NEAREST_NEIGHBOR && interpolationType != TYPE_BILINEAR
                && interpolationType != TYPE_BICUBIC) {
            throw new IllegalArgumentException("Unknown interpolation type: " + interpolationType);
        }
        this.xform = (AffineTransform) xform.clone();
        this.interpolationType = interpolationType;
        this.hints = null;
    }

    /**
     * Con el tipo de interpolación tomado de las pistas.
     *
     * <p>Sin pistas, o sin la pista de interpolación, se usa el vecino más cercano: es lo más rápido
     * y lo que corresponde cuando nadie pidió calidad.
     *
     * @throws ImagingOpException si la transformación no es invertible
     */
    public AffineTransformOp(AffineTransform xform, RenderingHints hints) {
        this.validar(xform);
        this.xform = (AffineTransform) xform.clone();
        this.hints = hints;
        int tipo = TYPE_NEAREST_NEIGHBOR;
        if (hints != null) {
            Object value = hints.get(RenderingHints.KEY_INTERPOLATION);
            if (value == null) {
                Object calidad = hints.get(RenderingHints.KEY_RENDERING);
                if (calidad == RenderingHints.VALUE_RENDER_QUALITY) {
                    tipo = TYPE_BILINEAR;
                }
            } else if (value == RenderingHints.VALUE_INTERPOLATION_BILINEAR) {
                tipo = TYPE_BILINEAR;
            } else if (value == RenderingHints.VALUE_INTERPOLATION_BICUBIC) {
                tipo = TYPE_BICUBIC;
            }
        }
        this.interpolationType = tipo;
    }

    /**
     * Comprueba que la transformación se pueda invertir.
     *
     * @throws ImagingOpException si el determinante es cero o casi
     */
    private void validar(AffineTransform xform) {
        double det = xform.getDeterminant();
        if (Math.abs(det) <= Double.MIN_VALUE) {
            throw new ImagingOpException("Unable to invert transform " + xform);
        }
    }

    /** El tipo de interpolación. */
    public final int getInterpolationType() {
        return this.interpolationType;
    }

    /** La transformación. */
    public final AffineTransform getTransform() {
        return (AffineTransform) this.xform.clone();
    }

    /**
     * Aplica la transformación a una imagen.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si el origen y el destino son el mismo objeto
     * @throws ImagingOpException si la transformación no se puede invertir
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (src == null) {
            throw new NullPointerException("src image is null");
        }
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
        }
        BufferedImage destino = dst;
        if (destino == null) {
            destino = this.createCompatibleDestImage(src, src.getColorModel());
        }
        // Se transforma en ARGB y no en el formato de la imagen: interpolar exige promediar
        // colores, y promediar indices de paleta o campos de bits no significa nada.
        int sw = src.getWidth();
        int sh = src.getHeight();
        int dw = destino.getWidth();
        int dh = destino.getHeight();
        int[] origen = new int[sw * sh];
        src.getRGB(0, 0, sw, sh, origen, 0, sw);
        int[] salida = new int[dw * dh];
        AffineTransform inv;
        try {
            inv = this.xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new ImagingOpException("Unable to invert transform " + this.xform);
        }
        double[] punto = new double[2];
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                punto[0] = x + 0.5;
                punto[1] = y + 0.5;
                inv.transform(punto, 0, punto, 0, 1);
                salida[y * dw + x] = this.muestrear(origen, sw, sh, punto[0] - 0.5,
                        punto[1] - 0.5);
            }
        }
        destino.setRGB(0, 0, dw, dh, salida, 0, dw);
        return destino;
    }

    /** El color ARGB que corresponde a esa coordenada continua del origen. */
    private int muestrear(int[] src, int w, int h, double x, double y) {
        if (this.interpolationType == TYPE_NEAREST_NEIGHBOR) {
            int ix = (int) Math.floor(x + 0.5);
            int iy = (int) Math.floor(y + 0.5);
            return leer(src, w, h, ix, iy);
        }
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        if (this.interpolationType == TYPE_BILINEAR) {
            int[] canales = new int[4];
            for (int c = 0; c < 4; c++) {
                double a = canal(leer(src, w, h, x0, y0), c);
                double b = canal(leer(src, w, h, x0 + 1, y0), c);
                double d = canal(leer(src, w, h, x0, y0 + 1), c);
                double e = canal(leer(src, w, h, x0 + 1, y0 + 1), c);
                double arriba = a + (b - a) * fx;
                double abajo = d + (e - d) * fx;
                canales[c] = recortar(arriba + (abajo - arriba) * fy);
            }
            return armar(canales);
        }
        int[] canales = new int[4];
        for (int c = 0; c < 4; c++) {
            double[] filas = new double[4];
            for (int j = 0; j < 4; j++) {
                double[] v = new double[4];
                for (int i = 0; i < 4; i++) {
                    v[i] = canal(leer(src, w, h, x0 - 1 + i, y0 - 1 + j), c);
                }
                filas[j] = cubica(v, fx);
            }
            canales[c] = recortar(cubica(filas, fy));
        }
        return armar(canales);
    }

    /**
     * La curva cúbica de Catmull-Rom sobre cuatro valores.
     *
     * <p>Pasa exactamente por los dos del medio y usa los de los costados sólo para la pendiente,
     * que es lo que le da los bordes más limpios que la bilineal — y también lo que le permite
     * pasarse del rango y necesitar recorte.
     */
    private static double cubica(double[] v, double t) {
        double a = v[3] - v[2] - v[0] + v[1];
        double b = v[0] - v[1] - a;
        double c = v[2] - v[0];
        double d = v[1];
        return a * t * t * t + b * t * t + c * t + d;
    }

    /** El píxel de esa posición, o transparente si cae afuera. */
    private static int leer(int[] src, int w, int h, int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return 0;
        }
        return src[y * w + x];
    }

    /** El canal `c` de un ARGB: 0 alfa, 1 rojo, 2 verde, 3 azul. */
    private static int canal(int argb, int c) {
        return (argb >> (24 - c * 8)) & 0xFF;
    }

    /** Un valor llevado a 0..255. */
    private static int recortar(double v) {
        int i = (int) (v + 0.5);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }

    /** Los cuatro canales de vuelta en un ARGB. */
    private static int armar(int[] canales) {
        return (canales[0] << 24) | (canales[1] << 16) | (canales[2] << 8) | canales[3];
    }

    /**
     * Aplica la transformación a un ráster.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si el origen y el destino son el mismo objeto, o si no tienen
     *     la misma cantidad de bandas
     * @throws ImagingOpException si la transformación no se puede invertir
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        if (src == null) {
            throw new NullPointerException("src image is null");
        }
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
        }
        WritableRaster destino = dst;
        if (destino == null) {
            destino = this.createCompatibleDestRaster(src);
        } else if (src.getNumBands() != destino.getNumBands()) {
            throw new IllegalArgumentException("Number of src bands (" + src.getNumBands()
                    + ") does not match number of dst bands (" + destino.getNumBands() + ")");
        }
        AffineTransform inv;
        try {
            inv = this.xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new ImagingOpException("Unable to invert transform " + this.xform);
        }
        int sw = src.getWidth();
        int sh = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dw = destino.getWidth();
        int dh = destino.getHeight();
        int bandas = src.getNumBands();
        int[] pixel = new int[bandas];
        double[] punto = new double[2];
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                punto[0] = x + 0.5;
                punto[1] = y + 0.5;
                inv.transform(punto, 0, punto, 0, 1);
                int ix = (int) Math.floor(punto[0]);
                int iy = (int) Math.floor(punto[1]);
                if (ix < 0 || iy < 0 || ix >= sw || iy >= sh) {
                    for (int b = 0; b < bandas; b++) {
                        pixel[b] = 0;
                    }
                } else {
                    pixel = src.getPixel(sx + ix, sy + iy, pixel);
                }
                destino.setPixel(destino.getMinX() + x, destino.getMinY() + y, pixel);
            }
        }
        return destino;
    }

    /** El rectángulo que va a ocupar el resultado. */
    public final Rectangle2D getBounds2D(BufferedImage src) {
        return this.getBounds2D(src.getRaster());
    }

    /** El rectángulo que va a ocupar el resultado. */
    public final Rectangle2D getBounds2D(Raster src) {
        int w = src.getWidth();
        int h = src.getHeight();
        double[] esquinas = new double[8];
        esquinas[0] = 0;
        esquinas[1] = 0;
        esquinas[2] = w;
        esquinas[3] = 0;
        esquinas[4] = w;
        esquinas[5] = h;
        esquinas[6] = 0;
        esquinas[7] = h;
        this.xform.transform(esquinas, 0, esquinas, 0, 4);
        double minX = esquinas[0];
        double maxX = esquinas[0];
        double minY = esquinas[1];
        double maxY = esquinas[1];
        for (int i = 2; i < 8; i = i + 2) {
            minX = Math.min(minX, esquinas[i]);
            maxX = Math.max(maxX, esquinas[i]);
            minY = Math.min(minY, esquinas[i + 1]);
            maxY = Math.max(maxY, esquinas[i + 1]);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Una imagen vacía del tamaño que va a ocupar el resultado.
     *
     * <p>El tamaño sale de {@link #getBounds2D} y no del origen: rotar una imagen cuadrada necesita
     * un destino más grande.
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel cm = destCM;
        Rectangle2D r = this.getBounds2D(src.getRaster());
        int w = (int) Math.ceil(r.getWidth());
        int h = (int) Math.ceil(r.getHeight());
        if (cm == null) {
            cm = src.getColorModel();
            if (cm instanceof IndexColorModel && src.getType() != BufferedImage.TYPE_BYTE_BINARY) {
                cm = ColorModel.getRGBdefault();
            }
        }
        WritableRaster wr = cm.createCompatibleWritableRaster(w, h);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /** Un ráster vacío del tamaño que va a ocupar el resultado. */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        Rectangle2D r = this.getBounds2D(src);
        return src.createCompatibleWritableRaster((int) r.getX(), (int) r.getY(),
                (int) Math.ceil(r.getWidth()), (int) Math.ceil(r.getHeight()));
    }

    /** A dónde va a parar ese punto. */
    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return this.xform.transform(srcPt, dstPt);
    }

    /** Las pistas de dibujo, o `null` si no hay. */
    public final RenderingHints getRenderingHints() {
        return this.hints;
    }
}
