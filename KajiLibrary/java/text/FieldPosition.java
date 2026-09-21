package java.text;

// KajiLibrary's java.text.FieldPosition — asks a formatter "and where did you put the X?".
//
// Formatting produces one flat string, but a caller often needs to know which SLICE of it is the
// integer part, or the exponent, so it can be aligned in a column or styled. So `format` takes one
// of these, and writes back the begin/end offsets of the requested field.
//
// MISSING, and it is a forced choice: the two constructors taking `java.text.Format.Field` and
// `getFieldAttribute()`. Not that they cannot be written --they were written and they worked-- but
// that with them declared, NO compilation unit of java.text that declares a subclass of
// java.text.Format.Field compiles any more: its `super(name)` stops resolving. Since every formatter
// names FieldPosition in its signature, the choice is between these three members and the whole
// java.text.NumberFormat.Field / java.text.DateFormat.Field / java.text.MessageFormat.Field classes.
//
// The Fields were chosen. Three missing members are a legal subset; a FieldPosition built with an
// attribute that NO formatter can then fill in --because without those classes there is nothing to
// name the field with-- is not: the caller would get begin == end == 0 and would read it as "the
// field came out empty".
//
// This header used to name the cause as well: that declaring them puts `java/text/Format$Field` into
// FieldPosition.class's constant pool. That is NOT the cause. It was ablated in `scratchpad/zz325/`:
// with `Pos` rewritten so that it does not name `Fmt.Field` at all --the only difference-- the error
// is identical, so the constant pool does not take part. The symptom is real and reproduced; the
// cause is still unidentified, most likely the circularity (Format names FieldPosition and
// FieldPosition names Format) on top of the nesting. The three members come back as soon as it is
// found and fixed.
public class FieldPosition {

    private Format.Field attribute;
    private final int field;
    private int beginIndex;
    private int endIndex;

    /** The field sought, named by its **attribute** instead of by an integer. */
    public FieldPosition(Format.Field attribute) {
        this(attribute, -1);
    }

    /** The one above, with the equivalent integer for the old formatters. */
    public FieldPosition(Format.Field attribute, int fieldID) {
        this.attribute = attribute;
        this.field = fieldID;
        this.beginIndex = 0;
        this.endIndex = 0;
    }

    /** The attribute sought, or `null` if it was built with an integer. */
    public Format.Field getFieldAttribute() {
        return this.attribute;
    }

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
