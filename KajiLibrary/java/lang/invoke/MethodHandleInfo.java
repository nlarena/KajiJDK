package java.lang.invoke;

// What a DIRECT method handle is made of, once cracked open: a reference kind, a declaring class,
// a name and a type. The nine `REF_*` constants are the JVMS 4.4.8 reference kinds, the same
// bytes `DirectMethodHandleDesc.Kind` wraps in the nominal world — this is the loaded twin of
// that enum.
//
// OMITTED (subset): `reflectAs(Class<T>, MethodHandles.Lookup)`. Its type variable is BOUNDED
// (`T extends Member`), which our compiler erases to `Object` instead of to the bound (#100), and
// its second parameter is a NESTED type from another file, which erases to `Object` too (#101).
// Two separate defects on one signature, and no source spelling avoids either.
public interface MethodHandleInfo {

    public static final int REF_getField = 1;
    public static final int REF_getStatic = 2;
    public static final int REF_putField = 3;
    public static final int REF_putStatic = 4;
    public static final int REF_invokeVirtual = 5;
    public static final int REF_invokeStatic = 6;
    public static final int REF_invokeSpecial = 7;
    public static final int REF_newInvokeSpecial = 8;
    public static final int REF_invokeInterface = 9;

    int getReferenceKind();

    Class<?> getDeclaringClass();

    String getName();

    MethodType getMethodType();

    int getModifiers();

    // Reads the `ACC_VARARGS` flag off the modifiers — the one piece of information a descriptor
    // cannot carry, since `int...` and `int[]` erase to the same thing.
    default boolean isVarArgs() {
        return (getModifiers() & 0x0080) != 0;
    }

    public static String referenceKindToString(int referenceKind) {
        String name = "invalid";
        if (referenceKind == REF_getField) {
            name = "getField";
        } else if (referenceKind == REF_getStatic) {
            name = "getStatic";
        } else if (referenceKind == REF_putField) {
            name = "putField";
        } else if (referenceKind == REF_putStatic) {
            name = "putStatic";
        } else if (referenceKind == REF_invokeVirtual) {
            name = "invokeVirtual";
        } else if (referenceKind == REF_invokeStatic) {
            name = "invokeStatic";
        } else if (referenceKind == REF_invokeSpecial) {
            name = "invokeSpecial";
        } else if (referenceKind == REF_newInvokeSpecial) {
            name = "newInvokeSpecial";
        } else if (referenceKind == REF_invokeInterface) {
            name = "invokeInterface";
        }
        return name;
    }

    public static String toString(int kind, Class<?> defc, String name, MethodType type) {
        return referenceKindToString(kind) + " " + defc.getName() + "." + name + ":" + type.toString();
    }
}
