package javax.print.attribute;

import java.io.Serializable;

// The syntax class of the attributes whose value is an integer.
//
// It is not an Attribute: it is the "value" half that a concrete subclass combines with the
// "category" half by implementing Attribute. That is why it is abstract and its constructors are
// protected -- nobody outside builds a loose IntegerSyntax.
//
// Careful with `equals`: it compares by `instanceof IntegerSyntax`, not by exact class, so two
// attributes of **different categories** with the same integer come out equal. It is what the JDK
// does and it is replicated as it is; the sets do not get confused because they index by category,
// not by value.
public abstract class IntegerSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 3644574816328081943L;

    private int value;

    protected IntegerSyntax(int value) {
        this.value = value;
    }

    // With a range: both ends are inclusive.
    protected IntegerSyntax(int value, int lowerBound, int upperBound) {
        if (value < lowerBound || value > upperBound) {
            throw new IllegalArgumentException("Value " + value + " not in range " + lowerBound
                                               + ".." + upperBound);
        }
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public boolean equals(Object object) {
        if (!(object instanceof IntegerSyntax)) {
            return false;
        }
        return this.value == ((IntegerSyntax) object).value;
    }

    public int hashCode() {
        return this.value;
    }

    public String toString() {
        return "" + this.value;
    }
}
