package java.lang;

import java.lang.constant.ClassDesc;

// Minimal java.lang.Enum — the implicit superclass of every enum.
//
// Until now enums ran *without* this class: `Color.<init>` calls
// `Enum.<init>(String, int)`, which the VM no-opped because the superclass was
// unresolvable (the same escalón the exception hierarchy uses). Identity comparison
// worked, so `==` over constants was already right. What was missing is the state: each
// constant's `name` and `ordinal`.
//
// The nested `EnumDesc` is the reason this file exists now. A `switch` over enum patterns
// compiles its case labels to *dynamic constants* built by `Enum$EnumDesc.of`, so that
// class has to exist under exactly that binary name for the condy to resolve.
public abstract class Enum {
    private final String name;
    private final int ordinal;

    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public final String name() {
        return name;
    }

    public final int ordinal() {
        return ordinal;
    }

    // A *nominal* description of one enum constant: which enum type, and which constant
    // of it. Nominal is the point — it names the constant without loading the class, so a
    // case label can be a compile-time constant.
    public static final class EnumDesc {
        private final ClassDesc constantType;
        private final String constantName;

        private EnumDesc(ClassDesc constantType, String constantName) {
            this.constantType = constantType;
            this.constantName = constantName;
        }

        // What the condy behind a `case Color.RED ->` label calls.
        public static EnumDesc of(ClassDesc constantType, String constantName) {
            return new EnumDesc(constantType, constantName);
        }

        public ClassDesc constantType() {
            return constantType;
        }

        public String constantName() {
            return constantName;
        }
    }
}
