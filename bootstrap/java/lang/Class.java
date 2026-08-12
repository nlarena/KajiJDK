package java.lang;

// Minimal java.lang.Class — the runtime representation of a class. In our VM the
// `Class<…>` object IS the heap mirror; getClass() just hands back that reference.
public final class Class<T> {
    // isInstance(obj): is obj an instance of this class? Native — it's the subtype
    // check the VM does (reusing is_subtype over the class hierarchy).
    public native boolean isInstance(Object obj);

    // descriptorString(): this class's field descriptor — "Ljava/lang/String;" for a
    // class, "[I" for an array, "I" for the primitive int. Native: the VM reads the class
    // name off this mirror. (Used by java.lang.invoke.MethodType to build a method
    // descriptor from Class args.)
    public native String descriptorString();

    // getPrimitiveClass(name): the Class mirror of a primitive type ("int" → the int class).
    // Native — the VM mints a header-only mirror per primitive. `int.class` compiles to
    // `getstatic Integer.TYPE`, and Integer.TYPE is this call; there's no other way to name a
    // primitive's Class, which is why it can't be written in ordinary Java.
    static native Class<?> getPrimitiveClass(String name);

    // getName(): the JDK-format name of the class this mirror represents — dotted binary name
    // for classes/interfaces ("java.lang.String"), descriptor form with dots for arrays
    // ("[I", "[Ljava.lang.String;"). Native: the VM reads the class name off this mirror.
    public native String getName();

    // getSimpleName(): the source-level simple name — the segment after the last '.' (and '$'
    // for nested classes); arrays get the component's simple name + "[]". Native — our String
    // has no lastIndexOf yet, so the VM slices the name it already holds.
    public native String getSimpleName();
}
