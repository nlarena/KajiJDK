package java.text;

// KajiLibrary's java.text.ParsePosition — a cursor handed INTO a parse and updated by it.
//
// It exists because parsing a format is incremental: `MessageFormat` parses a number, then a
// literal, then a date, each continuing where the last stopped. A return value cannot carry both
// the parsed object and the new position, so the position travels in a mutable argument.
//
// `errorIndex` is the other half of the contract: on failure the parser returns null AND records
// WHERE it gave up, which is what lets a caller report a useful message instead of "bad input".
public class ParsePosition {

    private int index;
    private int errorIndex;

    public ParsePosition(int index) {
        this.index = index;
        this.errorIndex = -1;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getErrorIndex() {
        return this.errorIndex;
    }

    public void setErrorIndex(int ei) {
        this.errorIndex = ei;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ParsePosition) {
            ParsePosition other = (ParsePosition) obj;
            return this.index == other.index && this.errorIndex == other.errorIndex;
        }
        return false;
    }

    public int hashCode() {
        return (this.errorIndex << 16) | this.index;
    }

    public String toString() {
        return "java.text.ParsePosition[index=" + Integer.toString(this.index)
                + ",errorIndex=" + Integer.toString(this.errorIndex) + "]";
    }
}
