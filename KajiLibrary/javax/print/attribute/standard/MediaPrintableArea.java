package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * El rectangulo de la hoja donde de verdad se puede imprimir.
 *
 * <p>Casi ninguna impresora llega al borde del papel: hay un margen fisico que impone el mecanismo.
 * Este atributo dice donde empieza ese rectangulo --{@code x}, {@code y} desde la esquina superior
 * izquierda-- y cuanto mide. Pedirlo mas chico que el fisico es legitimo y es como se piden
 * margenes; pedirlo mas grande no lo agranda.
 *
 * <p>No extiende {@link javax.print.attribute.Size2DSyntax Size2DSyntax} aunque se le parezca,
 * porque son cuatro numeros y no dos. Repite la misma idea igual: guarda todo en micrometros
 * enteros, que es lo que hace que las mismas medidas expresadas en pulgadas y en milimetros den
 * iguales. La unidad con la que se construyo <b>no</b> se guarda --{@code toString()} siempre
 * imprime en milimetros y {@code equals()} compara micrometros--, asi que
 * {@code new MediaPrintableArea(1, 1, 1, 1, INCH)} es igual a la misma area declarada en MM.
 *
 * <p>Ancho y alto tienen que ser estrictamente positivos: un area de impresion vacia no describe
 * nada. El origen si puede ser cero.
 */
public final class MediaPrintableArea
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -1597171464050795793L;

    /** Micrometros por pulgada. */
    public static final int INCH = 25400;

    /** Micrometros por milimetro. */
    public static final int MM = 1000;

    private int x;
    private int y;
    private int w;
    private int h;

    public MediaPrintableArea(float x, float y, float w, float h, int units) {
        if (x < 0.0f || y < 0.0f || w <= 0.0f || h <= 0.0f || units < 1) {
            throw new IllegalArgumentException("0 or negative value argument");
        }
        // El medio micrometro es para redondear al mas cercano y no truncar.
        this.x = (int) (x * units + 0.5f);
        this.y = (int) (y * units + 0.5f);
        this.w = (int) (w * units + 0.5f);
        this.h = (int) (h * units + 0.5f);
    }

    /** La variante entera no redondea porque el producto ya es exacto. */
    public MediaPrintableArea(int x, int y, int w, int h, int units) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || units < 1) {
            throw new IllegalArgumentException("0 or negative value argument");
        }
        this.x = x * units;
        this.y = y * units;
        this.w = w * units;
        this.h = h * units;
    }

    /** Los cuatro numeros juntos: {@code [x, y, ancho, alto]}. No es {@code [x0, y0, x1, y1]}. */
    public float[] getPrintableArea(int units) {
        return new float[] {getX(units), getY(units), getWidth(units), getHeight(units)};
    }

    public float getX(int units) {
        return convertFromMicrometers(this.x, units);
    }

    public float getY(int units) {
        return convertFromMicrometers(this.y, units);
    }

    public float getWidth(int units) {
        return convertFromMicrometers(this.w, units);
    }

    public float getHeight(int units) {
        return convertFromMicrometers(this.h, units);
    }

    public boolean equals(Object object) {
        if (!(object instanceof MediaPrintableArea)) {
            return false;
        }
        MediaPrintableArea other = (MediaPrintableArea) object;
        return this.x == other.x && this.y == other.y
            && this.w == other.w && this.h == other.h;
    }

    public final Class<? extends Attribute> getCategory() {
        return MediaPrintableArea.class;
    }

    public final String getName() {
        return "media-printable-area";
    }

    /** {@code "(x,y)->(ancho,alto)"} con el sufijo pegado; {@code unitsName} null lo omite. */
    public String toString(int units, String unitsName) {
        if (unitsName == null) {
            unitsName = "";
        }
        float[] vals = getPrintableArea(units);
        return "(" + vals[0] + "," + vals[1] + ")->(" + vals[2] + "," + vals[3] + ")" + unitsName;
    }

    public String toString() {
        return toString(MM, "mm");
    }

    // Los cuatro componentes con pesos primos distintos, para que permutar x e y cambie el hash.
    public int hashCode() {
        return this.x + 37 * this.y + 43 * this.w + 47 * this.h;
    }

    private static float convertFromMicrometers(int um, int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        return ((float) um) / ((float) units);
    }
}
