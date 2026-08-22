package java.util.function;

// KajiLibrary's java.util.function.BooleanSupplier — a source of boolean values: takes
// nothing and produces a boolean on each call. SAM: `getAsBoolean`. It exists instead of
// reusing Supplier<Boolean> because that would box every answer; a guard consulted inside
// a loop is exactly the place where a Boolean allocation per iteration is unacceptable.
public interface BooleanSupplier {

    boolean getAsBoolean();
}
