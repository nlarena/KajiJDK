package java.lang.reflect;

// KajiLibrary's java.lang.reflect.Field — a reflective field. The VM allocates and populates Field
// objects natively (Class.getDeclaredFields), writing clazz/name/type/modifiers/slot directly; the
// getters read those back. get() reads a reference-typed field's value off a target object (a native).
public final class Field extends AccessibleObject implements Member {

    private Class<?> clazz;
    private String name;
    private Class<?> type;
    private int modifiers;
    private int slot;

    // Only the VM constructs Field objects (populating the fields from native code).
    private Field() {
    }

    public Class<?> getDeclaringClass() {
        return this.clazz;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getType() {
        return this.type;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    public boolean isSynthetic() {
        return (this.modifiers & 0x00001000) != 0;
    }

    // Reads this field's value from `obj`. In KajiLibrary this handles reference-typed fields
    // (primitive fields would be boxed — a runtime follow-up). Backed by the VM.
    public native Object get(Object obj);
}
