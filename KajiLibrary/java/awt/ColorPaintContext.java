package java.awt;

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * El contexto de pintado de un color plano.
 *
 * <p>Es el más simple posible y por eso vale la pena mirarlo: no invierte transformaciones, no mira
 * coordenadas y no calcula nada por píxel. Arma **un** ráster de un píxel del color pedido y para
 * cualquier rectángulo devuelve un hijo suyo estirado a ese tamaño, así que dos pedidos del mismo
 * tamaño no reservan memoria dos veces.
 *
 * <p>No es pública: es cómo está escrito {@link Color#createContext}.
 */
class ColorPaintContext implements PaintContext {

    private final int color;
    private final ColorModel model = ColorModel.getRGBdefault();
    private WritableRaster cache;

    /** Con el color ARGB que va a devolver siempre. */
    ColorPaintContext(int color) {
        this.color = color;
    }

    /** No hay nada que soltar más que el ráster guardado. */
    public void dispose() {
        this.cache = null;
    }

    /** Siempre ARGB de ocho bits por canal. */
    public ColorModel getColorModel() {
        return this.model;
    }

    /**
     * Un ráster del tamaño pedido, todo del mismo color.
     *
     * <p>Se guarda el último y se reusa mientras alcance: quien dibuja pide rectángulos del mismo
     * tamaño una y otra vez, y reservar uno por pedido sería tirar memoria a la basura.
     */
    public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster r = this.cache;
        if (r == null || r.getWidth() < w || r.getHeight() < h) {
            r = this.model.createCompatibleWritableRaster(w, h);
            int[] fila = new int[w];
            for (int i = 0; i < w; i++) {
                fila[i] = this.color;
            }
            for (int j = 0; j < h; j++) {
                r.setDataElements(0, j, w, 1, fila);
            }
            this.cache = r;
            return r;
        }
        return r.createWritableChild(0, 0, w, h, 0, 0, null);
    }
}
