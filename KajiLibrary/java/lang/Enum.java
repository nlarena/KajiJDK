package java.lang;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Field;

// KajiLibrary's java.lang.Enum — the common superclass every `enum` compiles to (§8.9).
// It keeps the state that makes an enum an enum: the constant's `name` and its `ordinal`,
// both set once by the constructor and never changed — which is exactly what lets `==`
// be the right comparison for enum constants.
//
// `valueOf(Class, String)` is here because the compiler needs it: every enum's synthesized
// `valueOf(String)` is one line, `(E) Enum.valueOf(E.class, name)`, exactly as the JDK does it.
// Without this method NO enum compiles at all.
public abstract class Enum {

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
    public static Enum valueOf(Class<?> enumType, String name) {
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
                    return (Enum) value;
                }
            }
            i = i + 1;
        }
        throw new IllegalArgumentException("No enum constant " + name);
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
