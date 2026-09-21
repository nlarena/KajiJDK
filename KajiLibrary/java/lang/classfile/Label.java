package java.lang.classfile;

// A position inside a method's body, treated as an identity and not as a number. It is what makes
// inserting code possible without recomputing jumps: the targets are named, and the offset is only
// resolved when writing.
//
// It declares no member on purpose -- neither does the JDK. A label is only compared by identity and
// is resolved against the `CodeModel` or the `CodeBuilder` that created it.
public interface Label {
}
