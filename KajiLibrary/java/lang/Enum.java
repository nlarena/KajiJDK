package java.lang;

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
}
