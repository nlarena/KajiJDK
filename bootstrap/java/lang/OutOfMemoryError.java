package java.lang;

// Minimal java.lang.OutOfMemoryError — thrown when the VM cannot satisfy an
// allocation because the heap is exhausted and the GC can reclaim nothing more
// (JVMS §6.3). The real JDK extends VirtualMachineError; this bootstrap library
// has no VirtualMachineError yet, so it extends Error directly.
public class OutOfMemoryError extends Error {
    public OutOfMemoryError(String message) {
        super(message);
    }

    public OutOfMemoryError() {
    }
}
