package java.awt;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D$Double;

import jdk.internal.awt.FuenteBitmap;

/**
 * Las metricas de la unica fuente de esta VM; ver {@link FuenteBitmap}.
 *
 * <p>{@link FontMetrics} define casi todo en terminos de {@link #charsWidth} y {@link #getWidths},
 * y las dos se definen en circulo entre si a proposito: la subclase tiene que romperlo con la medida
 * real, y esta es la unica que hay. Los dos metodos de aca son los que convierten el circulo en una
 * tabla.
 *
 * <p>Toda {@link Font} da las mismas metricas, sea cual sea su nombre o tamano, porque toda
 * {@code Font} se dibuja con la misma cara. Es la sustitucion de la que habla {@link FuenteBitmap},
 * y lo importante es que sea <strong>la misma</strong> en las dos puntas: lo que esto mide es lo que
 * el rasterizador pinta.
 */
class KajiFontMetrics extends FontMetrics {

    private static final long serialVersionUID = 1L;

    KajiFontMetrics(Font font) {
        super(font);
    }

    public int getAscent() {
        return FuenteBitmap.ASCENDENTE;
    }

    public int getDescent() {
        return FuenteBitmap.DESCENDENTE;
    }

    public int getLeading() {
        return FuenteBitmap.ENTRELINEA;
    }

    public int getMaxAdvance() {
        return FuenteBitmap.AVANCE_MAX;
    }

    /** La suma de los avances. Es la primitiva: {@code stringWidth} y {@code charWidth} salen de aca. */
    public int charsWidth(char[] data, int off, int len) {
        int total = 0;
        for (int i = 0; i < len; i++) {
            total = total + FuenteBitmap.avance(data[off + i]);
        }
        return total;
    }

    /** Los avances de los 256 primeros caracteres; fuera de ASCII, el de {@code ?}. */
    public int[] getWidths() {
        int[] anchos = new int[256];
        for (int c = 0; c < 256; c++) {
            anchos[c] = FuenteBitmap.avance((char) c);
        }
        return anchos;
    }

    /**
     * La caja de una cadena, desde la linea de base.
     *
     * <p>Sobrescrita porque la de {@link FontMetrics} delega en {@link Font#getStringBounds}, que
     * necesita el motor tipografico que esta VM no trae. La caja es el ancho medido por
     * {@link #stringWidth} y el alto del renglon, con el origen en la linea de base — de ahi la
     * {@code y} negativa: {@code -ascenso}.
     */
    public Rectangle2D getStringBounds(String str, Graphics context) {
        return new Rectangle2D$Double(0, -getAscent(), stringWidth(str), getHeight());
    }

    public Rectangle2D getStringBounds(String str, int beginIndex, int limit, Graphics context) {
        return getStringBounds(str.substring(beginIndex, limit), context);
    }
}
