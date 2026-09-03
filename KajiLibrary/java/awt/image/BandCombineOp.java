package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Combina las bandas de un ráster con una matriz: cada banda de salida es una suma pesada de las de
 * entrada.
 *
 * <p>Es lo que hace posible pasar de RGB a gris con los pesos correctos, intercambiar canales,
 * separar luminancia y crominancia, o cualquier mezcla lineal de bandas en una sola pasada.
 *
 * <p>La matriz tiene una fila por banda de salida. Si además tiene **una columna de más**, esa
 * última se multiplica por un 1 implícito y funciona como término constante: es el truco de
 * coordenadas homogéneas, y permite escribir un corrimiento —brillo— dentro de la misma matriz.
 *
 * <p>Es la única de las operaciones de este paquete que **no** trabaja sobre imágenes. Mezclar
 * bandas es una operación sobre números sin interpretar: la banda 0 de una imagen es roja y la de
 * otra es cian, y una matriz que las combine no sabría qué está produciendo. Por eso implementa
 * {@link RasterOp} y no {@link BufferedImageOp}.
 */
public class BandCombineOp implements RasterOp {

    private final float[][] matrix;
    private final int nrows;
    private final int ncols;
    private final RenderingHints hints;

    /**
     * Con la matriz dada.
     *
     * @throws NullPointerException si la matriz es `null`
     */
    public BandCombineOp(float[][] matrix, RenderingHints hints) {
        this.nrows = matrix.length;
        this.ncols = matrix[0].length;
        this.matrix = new float[this.nrows][];
        for (int i = 0; i < this.nrows; i++) {
            this.matrix[i] = new float[this.ncols];
            System.arraycopy(matrix[i], 0, this.matrix[i], 0,
                    Math.min(this.ncols, matrix[i].length));
        }
        this.hints = hints;
    }

    /** Una copia de la matriz. */
    public final float[][] getMatrix() {
        float[][] out = new float[this.nrows][];
        for (int i = 0; i < this.nrows; i++) {
            out[i] = new float[this.ncols];
            System.arraycopy(this.matrix[i], 0, out[i], 0, this.ncols);
        }
        return out;
    }

    /**
     * Aplica la matriz.
     *
     * @param dst el destino, o `null` para que se cree
     * @throws IllegalArgumentException si la matriz no tiene tantas columnas como bandas de entrada
     *     —o una más—, o si el destino no tiene tantas bandas como filas la matriz
     */
    public WritableRaster filter(Raster src, WritableRaster dst) {
        int nBands = src.getNumBands();
        if (this.ncols != nBands && this.ncols != nBands + 1) {
            throw new IllegalArgumentException("Number of columns in the  matrix (" + this.ncols
                    + ") must be equal to the number of bands ([+1]) in src (" + nBands + ").");
        }
        WritableRaster destino = dst;
        if (destino == null) {
            destino = this.createCompatibleDestRaster(src);
        } else if (this.nrows != destino.getNumBands()) {
            throw new IllegalArgumentException("Number of rows in the  matrix (" + this.nrows
                    + ") must be equal to the number of bands ([+1]) in dst (" + nBands + ").");
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = destino.getMinX();
        int dy = destino.getMinY();
        // La columna de mas se multiplica por un 1 implicito: es el termino constante.
        boolean constante = this.ncols == nBands + 1;
        int[] entrada = new int[nBands];
        int[] salida = new int[this.nrows];
        int[] maximos = new int[this.nrows];
        for (int b = 0; b < this.nrows; b++) {
            maximos[b] = (1 << destino.getSampleModel().getSampleSize(b)) - 1;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                entrada = src.getPixel(sx + x, sy + y, entrada);
                for (int b = 0; b < this.nrows; b++) {
                    float acum = 0.0f;
                    for (int c = 0; c < nBands; c++) {
                        acum = acum + this.matrix[b][c] * entrada[c];
                    }
                    if (constante) {
                        acum = acum + this.matrix[b][nBands];
                    }
                    int v = (int) (acum + 0.5f);
                    if (v < 0) {
                        v = 0;
                    } else if (v > maximos[b]) {
                        v = maximos[b];
                    }
                    salida[b] = v;
                }
                destino.setPixel(dx + x, dy + y, salida);
            }
        }
        return destino;
    }

    /**
     * Un ráster vacío con tantas bandas como filas tenga la matriz.
     *
     * @throws IllegalArgumentException si la matriz no encaja con las bandas del origen
     */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        int nBands = src.getNumBands();
        if (this.ncols != nBands && this.ncols != nBands + 1) {
            throw new IllegalArgumentException("Number of columns in the  matrix (" + this.ncols
                    + ") must be equal to the number of bands ([+1]) in src (" + nBands + ").");
        }
        if (this.nrows == nBands) {
            return src.createCompatibleWritableRaster();
        }
        SampleModel sm = src.getSampleModel();
        int[] bandas = new int[this.nrows];
        for (int i = 0; i < this.nrows; i++) {
            // Un destino con mas bandas que el origen repite la ultima disposicion: no hay de donde
            // sacar la de una banda que en el origen no existe.
            bandas[i] = Math.min(i, sm.getNumBands() - 1);
        }
        SampleModel nsm = sm.createSubsetSampleModel(bandas);
        return Raster.createWritableRaster(nsm, nsm.createDataBuffer(),
                new java.awt.Point(src.getMinX(), src.getMinY()));
    }

    /** El mismo rectángulo: esta operación no mueve nada de lugar. */
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
