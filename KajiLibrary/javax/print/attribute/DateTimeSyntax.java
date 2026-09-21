package javax.print.attribute;

import java.io.Serializable;
import java.util.Date;

// The syntax class of the attributes whose value is an instant.
//
// `java.util.Date` is mutable, and this class wraps it **halfway**: it copies on the way out but
// not on the way in. It is the JDK's behaviour and it is replicated as it is because it is
// observable, but it is as well to face it because it looks like an oversight, and it is one:
//
//     Date d = new Date(1000);
//     X x = new X(d);          // THE SAME Date is kept, not a copy
//     d.setTime(2000);         // ...so this changes the value of the already built attribute
//
// `getValue()` does return a copy, so the hole goes one way only: whoever built the attribute can
// change it from behind, whoever only reads it cannot. The half that is there protects the
// attribute from its readers; the one missing is the one that would protect it from its creator.
//
// Closing it by copying in the constructor too was considered and discarded: it would change the
// behaviour against the JDK in a case a program can notice, and this package is a reimplementation,
// not a correction.
public abstract class DateTimeSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = -1400819079791208582L;

    private Date value;

    // The reference is kept, not a copy. See the header.
    protected DateTimeSyntax(Date value) {
        if (value == null) {
            throw new NullPointerException("value is null");
        }
        this.value = value;
    }

    // Here there is a copy: a new Date with the same instant. The JDK writes exactly this --not a
    // `clone()`--, so our java.util.Date does not need to expose clone.
    public Date getValue() {
        return new Date(this.value.getTime());
    }

    public boolean equals(Object object) {
        if (!(object instanceof DateTimeSyntax)) {
            return false;
        }
        return this.value.equals(((DateTimeSyntax) object).value);
    }

    public int hashCode() {
        return this.value.hashCode();
    }

    public String toString() {
        return this.value.toString();
    }
}
