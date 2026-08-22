package java.lang.invoke;

// Minimal java.lang.invoke.MethodType — the *shape* of a method (return + parameter
// types), carried around as its field-descriptor string. Pure Java state: it builds the
// descriptor from its `Class` arguments via `Class.descriptorString()` (a native). The
// factory arities here are the ones our tests exercise; the real JDK has a varargs form too.
public final class MethodType {
    private final String descriptor; // e.g. "(Ljava/lang/String;)Ljava/lang/String;"

    private MethodType(String descriptor) {
        this.descriptor = descriptor;
    }

    // The method descriptor string, e.g. "(Ljava/lang/String;)Ljava/lang/String;".
    public String descriptorString() {
        return this.descriptor;
    }

    public static MethodType methodType(Class<?> rtype) {
        return new MethodType("()" + rtype.descriptorString());
    }

    public static MethodType methodType(Class<?> rtype, Class<?> ptype0) {
        return new MethodType("(" + ptype0.descriptorString() + ")" + rtype.descriptorString());
    }

    public static MethodType methodType(Class<?> rtype, Class<?> ptype0, Class<?> ptype1) {
        return new MethodType(
            "(" + ptype0.descriptorString() + ptype1.descriptorString() + ")" + rtype.descriptorString());
    }
}
