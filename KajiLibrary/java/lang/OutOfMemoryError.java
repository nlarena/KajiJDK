package java.lang;

// KajiLibrary's java.lang.OutOfMemoryError — the allocator could not find room for an object
// and the garbage collector could not free enough. Thrown by the VM at the allocation site,
// which is rarely the code actually responsible for holding the memory.
public class OutOfMemoryError extends VirtualMachineError {

    public OutOfMemoryError() {
    }

    public OutOfMemoryError(String message) {
        super(message);
    }
}
