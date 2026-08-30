package java.lang.invoke;

import java.lang.reflect.Member;

// What a DIRECT method handle is made of, once cracked open: a reference kind, a declaring class,
// a name and a type. The nine `REF_*` constants are the JVMS 4.4.8 reference kinds, the same
// bytes `DirectMethodHandleDesc.Kind` wraps in the nominal world — this is the loaded twin of
// that enum.
//
// `reflectAs` is now declared, and the two defects that kept it out are both sidestepped rather
// than fixed. Its type variable is BOUNDED (`T extends Member`) and our compiler erases a bounded
// variable to `Object` instead of to its leftmost bound (#100), so it is written RAW, returning
// the bound — which is precisely the descriptor the JDK emits. Its second parameter is a nested
// type from another file, which cannot be spelled the Java way (#101/#208), so it is written with
// the type's binary name; see the note in `MethodHandles.java`.
//
// What is LOST by writing it raw is only compile-time precision: a caller of the JDK's version
// gets a `Method` back from `reflectAs(Method.class, lk)` without a cast, and a caller of this one
// has to cast. The class file is identical either way.
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

    // The inverse of `Lookup.unreflect*`: recover the `Method`, `Constructor` or `Field` the
    // handle was made from. The `Lookup` parameter is not decoration — cracking a handle open
    // hands back a member that the caller may not be allowed to touch, so the access check that
    // making the handle required has to be paid again here.
    <T extends Member> T reflectAs(Class<T> expected, MethodHandles$Lookup lookup);

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
