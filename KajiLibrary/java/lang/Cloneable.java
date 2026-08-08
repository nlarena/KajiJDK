package java.lang;

// KajiLibrary's java.lang.Cloneable — a **marker** interface (no methods): implementing it
// tells Object.clone() that field-by-field copies of this class are legal (otherwise clone
// throws CloneNotSupportedException).
public interface Cloneable {
}
