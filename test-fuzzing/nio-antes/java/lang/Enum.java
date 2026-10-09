package java.lang;

import java.io.Serializable;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Field;
import java.util.Optional;

// KajiLibrary's java.lang.Enum — the common superclass every `enum` compiles to (§8.9).
// It keeps the state that makes an enum an enum: the constant's `name` and its `ordinal`,
// both set once by the constructor and never changed — which is exactly what lets `==`
// be the right comparison for enum constants.
//
// It is **generic** (`Enum<E extends Enum<E>>`), matching what the compiler already emits for every
// enum's supertype (`extends Enum<Self>`, with a `Signature`); that self-bound is what types
// `compareTo` and `getDeclaringClass` to the enum's own type.
//
// `valueOf(Class, String)` is here because the compiler needs it: every enum's synthesized
// `valueOf(String)` is one line, `(E) Enum.valueOf(E.class, name)`, exactly as the JDK does it.
// Without this method NO enum compiles at all.
//
// It does NOT implement `Constable` (the JDK does): that override returns an `EnumDesc`, and the
// JDK's `EnumDesc` is a `DynamicConstantDesc`, while ours is a plain descriptor the switch-condy
// mechanism reads by field name — making it a `ConstantDesc` would tangle those two. `describeConstable`
// is still here as a plain method (same signature, same result), just not the interface override.
public abstract class Enum<E extends Enum<E>> implements Comparable<E>, Serializable {

    private final String name;

    private final int ordinal;

    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    // The constant's name, exactly as declared.
    public final String name() {
        return this.name;
    }

    // Its position in the declaration, from zero — the index into the synthetic
    // `$SwitchMap` array a `switch` over an enum is lowered to.
    public final int ordinal() {
        return this.ordinal;
    }

    public String toString() {
        return this.name;
    }

    // Enum equality is identity: two constants are equal iff they are the same object. `final`
    // because an enum must not be allowed to redefine it (there is exactly one instance per name).
    public final boolean equals(Object other) {
        return this == other;
    }

    // The identity hash, `final` for the same reason as {@link #equals}.
    public final int hashCode() {
        return super.hashCode();
    }

    // Order by declaration position, within the same enum type (which the type system guarantees:
    // the parameter is `E`, this enum's own type).
    public final int compareTo(E o) {
        return this.ordinal - o.ordinal();
    }

    /**
     * The {@code Class} of this constant's enum type.
     *
     * <p>Not simply {@code getClass()}: a constant with a body (`RED { ... }`) is an anonymous
     * subclass, so its {@code getClass()} is that subclass, and the enum type is its superclass.
     */
    public final Class<E> getDeclaringClass() {
        Class<?> clazz = getClass();
        Class<?> zuper = clazz.getSuperclass();
        return (Class<E>) (zuper == Enum.class ? clazz : zuper);
    }

    /**
     * The constant of {@code enumType} named {@code name}.
     *
     * <p>Every enum's synthesized {@code valueOf(String)} is a call to this one, so nothing about
     * enums works without it — a missing {@code Enum.valueOf} is why no enum in the library
     * compiled at all.
     *
     * <p>It finds the constant by REFLECTION, over the declared static fields, which is how the
     * JDK does it too (there over a cached name-to-constant map built from {@code $VALUES}). A
     * lookup walks the fields once; there is no cache here because caching needs a map keyed by
     * {@code Class}, and this class is loaded far too early for that.
     *
     * @throws IllegalArgumentException if the type has no constant of that name
     */
    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
        if (enumType == null || name == null) {
            throw new NullPointerException();
        }
        // Object[] and not Field[]: a call returning an array of a cross-package reference type is
        // emitted with an `Object[]` descriptor and then fails to resolve (finding #102). Widening
        // the local is legal Java -- arrays are covariant -- and it is what the call site already
        // says it returns.
        Object[] declared = enumType.getDeclaredFields();
        int i = 0;
        while (i < declared.length) {
            Field field = (Field) declared[i];
            if (field.getName().equals(name)) {
                Object value = field.get(null);
                if (value instanceof Enum) {
                    return (T) value;
                }
            }
            i = i + 1;
        }
        throw new IllegalArgumentException("No enum constant " + name);
    }

    /**
     * A nominal descriptor for this constant, if its enum type has one. Same result as the JDK's
     * {@code Constable} override (this class does not implement {@code Constable}; see the header).
     */
    public final Optional<EnumDesc> describeConstable() {
        Optional<ClassDesc> type = this.getDeclaringClass().describeConstable();
        if (type.isPresent()) {
            return Optional.of(EnumDesc.of(type.get(), this.name()));
        }
        return Optional.empty();
    }

    // A *nominal* descriptor for one enum constant: which enum type, and which constant of it.
    // Nominal is the point — it names the constant without holding a reference to it, so it can
    // live in the constant pool as a `condy` argument, which is how a `switch` over an enum
    // lowers its case labels.
    //
    // Both fields are read back by name by the interpreter when it matches a label against the
    // selector, so `constantType` and `constantName` must keep these exact names.
    public static final class EnumDesc {

        private final ClassDesc constantType;

        private final String constantName;

        private EnumDesc(ClassDesc constantType, String constantName) {
            this.constantType = constantType;
            this.constantName = constantName;
        }

        // The only way to build one — the bootstrap the compiler emits calls exactly this.
        public static EnumDesc of(ClassDesc constantType, String constantName) {
            return new EnumDesc(constantType, constantName);
        }

        public ClassDesc constantType() {
            return this.constantType;
        }

        public String constantName() {
            return this.constantName;
        }
    }
}
