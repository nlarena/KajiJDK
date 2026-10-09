package javax.print.attribute;

import java.io.Serializable;

// The syntax class of the attributes that are a two-dimensional size (a sheet, a margin).
//
// Same trick as ResolutionSyntax: inside it is an integer in **micrometres**, and the INCH and MM
// constants are the conversion factor (25400 um = 1 inch, 1000 um = 1 mm). Keeping the integer and
// not the float is what lets two sizes built in different units be compared by exact equality.
//
// The difference from ResolutionSyntax is that here reading returns a `float`: a size in inches is
// almost never an integer.
public abstract class Size2DSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 5584439964938660530L;

    private int x;
    private int y;

    // The two factors to micrometres.
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

    // The integer variant does not round because it does not need to: the product is already exact.
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

    // The two numbers together: [x, y].
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

    // "8.5x11.0 in". With a null `unitsName` the suffix and the space are omitted.
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

    // The low 16 bits of each component, like ResolutionSyntax.
    public int hashCode() {
        return (this.x & 0x0000FFFF) | ((this.y & 0x0000FFFF) << 16);
    }

    // In micrometres, the internal unit: "215900x279400 um".
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
