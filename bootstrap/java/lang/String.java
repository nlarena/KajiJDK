package java.lang;

// Minimal java.lang.String — just the type. Its text isn't a Java field here; the VM
// lays the UTF-8 bytes inline in the object (see interpreter::strings), materialised
// by `ldc` and read back by the native println.
public final class String {
    public native int length();

    public native char charAt(int index);

    public native boolean equals(Object o);

    public native int hashCode();

    // The text of any object: `null` becomes "null", a String is itself, anything else
    // answers with its own `toString()`. Declared native, but the VM intercepts it before
    // the native bridge — it is *not* a leaf operation: calling `toString()` is a virtual
    // call back into user bytecode, which a native cannot make.
    //
    // `javac` emits this ahead of every string concatenation that splices an object, so
    // the concatenation call site itself only ever sees Strings.
    public static native String valueOf(Object o);
}
