package java.text;

// KajiLibrary's java.text.FieldPosition — asks a formatter "and where did you put the X?".
//
// Formatting produces one flat string, but a caller often needs to know which SLICE of it is the
// integer part, or the exponent, so it can be aligned in a column or styled. So `format` takes one
// of these, and writes back the begin/end offsets of the requested field.
//
// A KajiLibrary subset: the two constructors taking `Format.Field` and `getFieldAttribute()` are
// omitted — `Format.Field` is a nested type, and a nested type does not resolve (finding #101).
// The int-field constructor is the older API and needs nothing nested.
public class FieldPosition {

    private final int field;
    private int beginIndex;
    private int endIndex;

    public FieldPosition(int field) {
        this.field = field;
        this.beginIndex = 0;
        this.endIndex = 0;
    }

    public int getField() {
        return this.field;
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public void setBeginIndex(int bi) {
        this.beginIndex = bi;
    }

    public void setEndIndex(int ei) {
        this.endIndex = ei;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FieldPosition) {
            FieldPosition other = (FieldPosition) obj;
            return this.field == other.field
                    && this.beginIndex == other.beginIndex
                    && this.endIndex == other.endIndex;
        }
        return false;
    }

    public int hashCode() {
        return (this.field << 24) | (this.beginIndex << 16) | this.endIndex;
    }

    public String toString() {
        return "java.text.FieldPosition[field=" + Integer.toString(this.field)
                + ",beginIndex=" + Integer.toString(this.beginIndex)
                + ",endIndex=" + Integer.toString(this.endIndex) + "]";
    }
}
