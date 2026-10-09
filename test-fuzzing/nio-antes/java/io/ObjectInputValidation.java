package java.io;

// KajiLibrary's java.io.ObjectInputValidation — a callback for checking an object AFTER the
// whole graph has been read. It exists because readObject runs while the graph is still
// half-built: an object's references may point at instances whose own fields are not filled
// in yet, so any invariant that spans several objects cannot be checked there. Registering a
// validation defers the check to the point where the graph is complete.
public interface ObjectInputValidation {

    void validateObject() throws InvalidObjectException;
}
