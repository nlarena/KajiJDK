package java.lang;

// Minimal java.lang.Record — the implicit superclass every record extends. It carries no
// behaviour: `equals`/`hashCode`/`toString` are abstract here and javac generates each
// record's bodies as `invokedynamic` call sites bootstrapped by ObjectMethods.
//
// What this class *does* provide is the missing rung in the hierarchy. Without it the
// subtype walk from a record stops at an unloadable superclass and never reaches Object,
// so the verifier rejects passing a record where an Object is expected — which is every
// `record.equals(other)` call.
public abstract class Record {
    protected Record() {
    }
}
