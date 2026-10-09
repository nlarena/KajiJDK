package javax.print.attribute;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

// The syntax class of the enumerated attributes: an enum predating the language's `enum`s.
//
// A subclass declares its values as `public static final` constants and gives the base class three
// tables by overriding: `getStringTable()` (the names), `getEnumValueTable()` (the constants
// themselves) and `getOffset()` (which integer the first one starts at). That is the whole
// mechanism; the base class only indexes.
//
// As the values are singletons, equality is Object's -- identity -- and that is why `EnumSyntax`
// does **not** override `equals`. That forces `clone()` to return `this` and deserialization to go
// through `readResolve()`: without that, a value travelling through a stream would come back as a
// different copy and `==` would stop working, which is how an enum is used.
public abstract class EnumSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = -2739521845085831642L;

    private int value;

    protected EnumSyntax(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    // The same object: cloning a singleton would stop it being one.
    public Object clone() {
        return this;
    }

    public int hashCode() {
        return this.value;
    }

    // The table's name if the value falls inside; otherwise, the bare integer. A subclass that
    // gives no table still prints something useful.
    public String toString() {
        int i = this.value - getOffset();
        String[] theTable = getStringTable();
        if (theTable != null && i >= 0 && i < theTable.length && theTable[i] != null) {
            return theTable[i];
        }
        return Integer.toString(this.value);
    }

    // The deserialization hook: it returns the constant corresponding to the integer read, not the
    // object just built. Without this, `==` against the constant would fail after a round trip
    // through a stream.
    //
    // The note said there is no serialization here to call it because KajiLibrary has no
    // ObjectInputStream; ObjectInputStream exists now, but its own note says it does not consult
    // `readResolve`, so it is still not called that way. The method is here and does what it says:
    // it is the integer -> constant translation, and it can be called directly.
    protected Object readResolve() throws ObjectStreamException {
        EnumSyntax[] theTable = getEnumValueTable();
        if (theTable == null) {
            throw new InvalidObjectException("Null enumeration value table for class "
                                             + getClass());
        }
        int theOffset = getOffset();
        int theIndex = this.value - theOffset;
        if (0 > theIndex || theIndex >= theTable.length) {
            throw new InvalidObjectException("Integer value = " + this.value
                                             + " not in valid range " + theOffset + ".."
                                             + (theOffset + theTable.length - 1)
                                             + "for class " + getClass());
        }
        EnumSyntax result = theTable[theIndex];
        if (result == null) {
            throw new InvalidObjectException("No enumeration value for integer value = "
                                             + this.value + "for class " + getClass());
        }
        return result;
    }

    // The three hooks. The default is "there is no table", which leaves toString() falling back to
    // the integer and readResolve() throwing InvalidObjectException -- which is right for a
    // subclass that did not declare itself as a real enum.
    protected String[] getStringTable() {
        return null;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return null;
    }

    // The integer of the tables' first entry. Zero unless the subclass says otherwise.
    protected int getOffset() {
        return 0;
    }
}
