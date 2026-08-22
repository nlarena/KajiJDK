package java.lang.reflect;

// KajiLibrary's java.lang.reflect.Method — a reflective method. A KajiLibrary subset: the object is
// meant to be created and populated by the VM (Class.getDeclaredMethods, a runtime follow-up); the
// getters read the stored metadata and invoke() is a native. Generic type/annotation accessors and
// the checked-exception `throws` on invoke() are omitted.
public final class Method extends AccessibleObject implements Member {

    private Class<?> clazz;
    private String name;
    private Class<?> returnType;
    private Class<?>[] parameterTypes;
    private int modifiers;
    private int slot;

    private Method() {
    }

    public Class<?> getDeclaringClass() {
        return this.clazz;
    }

    public String getName() {
        return this.name;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    public Class<?> getReturnType() {
        return this.returnType;
    }

    public Class<?>[] getParameterTypes() {
        return this.parameterTypes;
    }

    public int getParameterCount() {
        if (this.parameterTypes == null) {
            return 0;
        }
        return this.parameterTypes.length;
    }

    public boolean isSynthetic() {
        return (this.modifiers & 0x00001000) != 0;
    }

    public boolean isVarArgs() {
        return (this.modifiers & 0x00000080) != 0;
    }

    public boolean isBridge() {
        return (this.modifiers & 0x00000040) != 0;
    }

    // Invokes this method on `obj` with `args`. Backed by the VM (a runtime follow-up).
    public native Object invoke(Object obj, Object[] args);
}
