package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * La parte común de todos los contextos de pintado que se calculan punto por punto.
 *
 * <p>Hace el trabajo que no cambia entre un degradé y una textura: invertir la transformación,
 * recorrer el rectángulo pedido, llevar cada píxel de coordenadas de dispositivo a coordenadas de
 * usuario y armar el ráster. Lo único que cada pintura pone es {@link #colorDe}.
 *
 * <p>El píxel se muestrea en su **centro** —de ahí el medio píxel que se suma— y no en su esquina.
 * Muestrear en la esquina corre el degradé medio píxel, que se nota como una costura cuando dos
 * figuras pintadas con el mismo degradé se tocan.
 *
 * <p>No es pública: es un detalle de cómo están escritas las pinturas de este paquete.
 */
abstract class RasterPaintContext implements PaintContext {

    private final AffineTransform inverse;
    private final ColorModel model = ColorModel.getRGBdefault();

    /**
     * Con la transformación de usuario a dispositivo, que se invierte una sola vez.
     *
     * @throws NoninvertibleTransformException si la transformación aplasta el plano
     */
    RasterPaintContext(AffineTransform xform) throws NoninvertibleTransformException {
        this.inverse = xform.createInverse();
    }

    /** No hay nada que soltar: el ráster se arma en cada pedido. */
    public void dispose() {
    }

    /** Siempre ARGB de ocho bits por canal. */
    public ColorModel getColorModel() {
        return this.model;
    }

    /** Los píxeles de ese rectángulo del dispositivo. */
    public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster r = this.model.createCompatibleWritableRaster(w, h);
        double[] p = new double[2];
        int[] fila = new int[w];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                p[0] = x + i + 0.5;
                p[1] = y + j + 0.5;
                this.inverse.transform(p, 0, p, 0, 1);
                fila[i] = this.colorDe(p[0], p[1]);
            }
            r.setDataElements(0, j, w, 1, fila);
        }
        return r;
    }

    /** El color ARGB que le toca a ese punto en coordenadas de usuario. */
    abstract int colorDe(double ux, double uy);
}
