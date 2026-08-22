package java.lang;

import java.lang.reflect.Field;

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

    // The Java language modifiers (public/final/abstract/…) of this class, as a bitmask
    // to decode with java.lang.reflect.Modifier. Read from the class's access_flags by the VM.
    public native int getModifiers();

    // The direct superclass, or null for Object and for interfaces. The VM returns the
    // superclass's Class mirror.
    public native Class<? super T> getSuperclass();

    // Whether this type is an interface (the ACC_INTERFACE flag), read by the VM.
    public native boolean isInterface();

    // Whether an instance of `cls` can be assigned to a reference of this type — i.e. `cls`
    // is this type or a subtype of it. The VM's subtype check over the loaded hierarchy.
    public native boolean isAssignableFrom(Class<?> cls);

    // The fields declared by this class (not inherited ones). The VM allocates one
    // java.lang.reflect.Field per field, populated from the class metadata.
    public native Field[] getDeclaredFields();
}
