package java.lang.invoke;

// The bootstrap behind `"a" + b` since Java 9. Before that, the compiler emitted an explicit
// `StringBuilder` chain, which froze the strategy into every class file ever compiled; moving it
// behind an `invokedynamic` means the JDK can change how concatenation works — and it has, more
// than once — without recompiling anything.
//
// `makeConcatWithConstants` takes a RECIPE string in which one marker splices the next dynamic
// argument and another splices the next constant, so the constant parts of the expression travel
// in the constant pool instead of as arguments.
//
// As with `LambdaMetafactory`, our VM implements this in Rust and recognises the class by name,
// so this declaration is never loaded.
public final class StringConcatFactory {

    private StringConcatFactory() {
    }

    // `MethodHandles$Lookup` — the binary name — see `LambdaMetafactory` and `MethodHandles` for
    // why the Java spelling cannot be used here.
    public static CallSite makeConcat(MethodHandles$Lookup lookup, String name, MethodType concatType)
            throws StringConcatException {
        throw new UnsupportedOperationException("string concat linkage is done by the VM");
    }

    public static CallSite makeConcatWithConstants(MethodHandles$Lookup lookup, String name, MethodType concatType,
            String recipe, Object[] constants) throws StringConcatException {
        throw new UnsupportedOperationException("string concat linkage is done by the VM");
    }
}
