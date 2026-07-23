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
// Generic in E (self-referential, `E extends Enum<E>`) because that is the shape javac
// expects: compiling `enum Foo` produces `Foo extends Enum<Foo>`. A non-generic Enum makes
// javac reject the extends clause ("type Enum does not take parameters") — which only bites
// enums compiled against *this* class (i.e. those inside bootstrap/, like Thread.State).
public abstract class Enum<E extends Enum<E>> {
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

    // Every enum javac compiles gets a `static Foo valueOf(String)` that delegates here.
    // A stub on purpose: real lookup needs reflection over the enum's constants, and our
    // code never calls it — the constants are built directly in each enum's <clinit>, not
    // through valueOf. It exists only so the generated delegation links.
    public static <T extends Enum<T>> T valueOf(Class<T> enumClass, String name) {
        return null;
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
