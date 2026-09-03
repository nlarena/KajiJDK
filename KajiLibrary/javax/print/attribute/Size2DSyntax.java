package javax.print.attribute;

import java.io.Serializable;

// La clase de sintaxis de los atributos que son un tamano bidimensional (una hoja, un margen).
//
// Mismo truco que ResolutionSyntax: adentro es un entero en **micrometros**, y las constantes INCH
// y MM son el factor de conversion (25400 um = 1 pulgada, 1000 um = 1 mm). Guardar el entero y no
// el float es lo que hace que dos tamanos construidos en unidades distintas se puedan comparar por
// igualdad exacta.
//
// La diferencia con ResolutionSyntax es que aca la lectura devuelve `float`: un tamano en
// pulgadas casi nunca es entero.
public abstract class Size2DSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 5584439964938660530L;

    private int x;
    private int y;

    // Los dos factores a micrometros.
    public static final int INCH = 25400;
    public static final int MM = 1000;

    protected Size2DSyntax(float x, float y, int units) {
        if (x < 0.0f) {
            throw new IllegalArgumentException("x < 0");
        }
        if (y < 0.0f) {
            throw new IllegalArgumentException("y < 0");
        }
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        this.x = (int) (x * units + 0.5f);
        this.y = (int) (y * units + 0.5f);
    }

    // La variante entera no redondea porque no hace falta: el producto ya es exacto.
    protected Size2DSyntax(int x, int y, int units) {
        if (x < 0) {
            throw new IllegalArgumentException("x < 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y < 0");
        }
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        this.x = x * units;
        this.y = y * units;
    }

    private static float convertFromMicrometers(int um, int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        return ((float) um) / ((float) units);
    }

    // Los dos numeros juntos: [x, y].
    public float[] getSize(int units) {
        float[] result = new float[2];
        result[0] = getX(units);
        result[1] = getY(units);
        return result;
    }

    public float getX(int units) {
        return convertFromMicrometers(this.x, units);
    }

    public float getY(int units) {
        return convertFromMicrometers(this.y, units);
    }

    // "8.5x11.0 in". Con `unitsName` null se omite el sufijo y el espacio.
    public String toString(int units, String unitsName) {
        StringBuilder result = new StringBuilder();
        result.append(getX(units));
        result.append('x');
        result.append(getY(units));
        if (unitsName != null) {
            result.append(' ');
            result.append(unitsName);
        }
        return result.toString();
    }

    public boolean equals(Object object) {
        if (!(object instanceof Size2DSyntax)) {
            return false;
        }
        Size2DSyntax other = (Size2DSyntax) object;
        return this.x == other.x && this.y == other.y;
    }

    // Los 16 bits bajos de cada componente, igual que ResolutionSyntax.
    public int hashCode() {
        return (this.x & 0x0000FFFF) | ((this.y & 0x0000FFFF) << 16);
    }

    // En micrometros, la unidad interna: "215900x279400 um".
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.x);
        result.append('x');
        result.append(this.y);
        result.append(" um");
        return result.toString();
    }

    protected int getXMicrometers() {
        return this.x;
    }

    protected int getYMicrometers() {
        return this.y;
    }
}
