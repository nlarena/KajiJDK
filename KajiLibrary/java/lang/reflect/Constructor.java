package java.lang.reflect;

// KajiLibrary's java.lang.reflect.Constructor — a reflective constructor. A KajiLibrary subset (as
// Method): populated by the VM, getters read the metadata, newInstance() is a native.
public final class Constructor<T> extends AccessibleObject implements Member {

    private Class<T> clazz;
    private Class<?>[] parameterTypes;
    private int modifiers;
    private int slot;

    private Constructor() {
    }

    public Class<T> getDeclaringClass() {
        return this.clazz;
    }

    public String getName() {
        return this.clazz.getName();
    }

    public int getModifiers() {
        return this.modifiers;
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

    // Creates a new instance by invoking this constructor with `args`. Backed by the VM.
    public native T newInstance(Object[] args);
}
