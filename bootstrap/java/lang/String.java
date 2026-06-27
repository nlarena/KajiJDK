package java.lang;

// Minimal java.lang.String — just the type. Its text isn't a Java field here; the VM
// lays the UTF-8 bytes inline in the object (see interpreter::strings), materialised
// by `ldc` and read back by the native println.
public final class String {
    public native int length();

    public native char charAt(int index);

    public native boolean equals(Object o);

    public native int hashCode();
}
