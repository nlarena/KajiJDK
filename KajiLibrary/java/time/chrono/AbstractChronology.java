package java.time.chrono;

// KajiLibrary's java.time.chrono.AbstractChronology — the base class for Chronology implementations,
// supplying the identity/order plumbing (compare and equals by id, string form = id) so concrete
// chronologies only implement their calendar rules. A KajiLibrary subset of the JDK class.
public abstract class AbstractChronology implements Chronology {

    protected AbstractChronology() {
    }

    public int compareTo(Chronology other) {
        return this.getId().compareTo(other.getId());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractChronology) {
            return this.compareTo((AbstractChronology) obj) == 0;
        }
        return false;
    }

    public int hashCode() {
        return this.getClass().hashCode() ^ this.getId().hashCode();
    }

    public String toString() {
        return this.getId();
    }
}
