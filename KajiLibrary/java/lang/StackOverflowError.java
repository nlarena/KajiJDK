package java.lang;

// KajiLibrary's java.lang.StackOverflowError — a thread's call stack ran past its limit,
// almost always from unbounded recursion. Note it is an Error, not an Exception: the stack
// is what the handler itself would need to run, so catching it is a gamble.
public class StackOverflowError extends VirtualMachineError {

    public StackOverflowError() {
    }

    public StackOverflowError(String message) {
        super(message);
    }
}
