package java.lang;

// Minimal java.lang.StackOverflowError — thrown by the VM when a thread's call
// stack exceeds its limit (JVMS §6.3). The real JDK makes it extend
// VirtualMachineError; here it extends Error directly.
public class StackOverflowError extends Error {
    public StackOverflowError(String message) {
        super(message);
    }

    public StackOverflowError() {
    }
}
