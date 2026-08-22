package java.lang;

import java.lang.constant.ClassDesc;

// KajiLibrary's java.lang.Enum — the common superclass every `enum` compiles to (§8.9).
// It keeps the state that makes an enum an enum: the constant's `name` and its `ordinal`,
// both set once by the constructor and never changed — which is exactly what lets `==`
// be the right comparison for enum constants.
//
// `valueOf(Class, String)` is deliberately absent: the real one reflects over the class's
// `$VALUES`, while our compiler synthesizes a self-contained `valueOf(String)` into each
// enum instead, so nothing here needs to find another class's constants.
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
