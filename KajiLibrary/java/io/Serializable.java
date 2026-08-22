package java.io;

// Marker interface: a class implements Serializable to declare that its instances
// may be flattened to a byte stream. It has no methods — the type itself is the
// signal. KajiLibrary carries it so the concurrency and collection types can name
// it in their `implements` clause exactly as the JDK does.
public interface Serializable {
}
