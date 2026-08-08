package java.lang;

// KajiLibrary's java.lang.Class — the runtime mirror of a type. In KajiJDK the
// `Class<T>` object IS the heap mirror the VM keeps for each loaded class; `getClass()`
// simply hands back that reference. Generic in the type it describes (`String.class`
// has static type `Class<String>`).
public final class Class<T> {

    // Only the VM constructs Class mirrors (as the JDK does — its constructor is
    // private, called from native code). A private constructor blocks `new Class()`
    // and suppresses the synthesized public default.
    private Class() {}

    // Is `obj` an instance of this class? The VM's subtype check over the loaded class
    // hierarchy (the same `is_subtype` the verifier uses).
    public native boolean isInstance(Object obj);

    // The fully-qualified binary name of the type (`java.lang.String`), read from the
    // class metadata by the VM.
    public native String getName();
}
