package java.lang;

// KajiLibrary's java.lang.Record — the common superclass every `record` compiles to (§8.10).
// It carries no state and declares no members of its own: a record's components, accessors,
// `equals`/`hashCode`/`toString` are all synthesized into the record class itself by the
// compiler. What this class exists for is to be a *marker* the VM can test against — that is
// what makes `instanceof Record` (and the class-file `ACC_RECORD` machinery) meaningful.
//
// Abstract with a protected constructor: a record subclass calls it via `invokespecial`, and
// nothing else can instantiate it directly, exactly like the real one.
public abstract class Record {

    protected Record() {
    }
}
