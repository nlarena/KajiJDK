package java.lang;

// Our java.lang.Throwable — the root of the exception hierarchy. Carries an optional detail
// `message` and a `backtrace`: a pre-rendered stack-trace text that the VM fills in at throw time
// (in `athrow`'s unwind, the single point every thrown exception passes through — implicit faults
// and explicit `throw` alike). `toString` is native because it needs this object's runtime class
// name, which Java can't read of itself (no `Class.getName()` yet). `printStackTrace` then prints
// the header plus the captured frames.
public class Throwable {
    private String message;
    private String backtrace; // "\tat pkg.Class.method" lines; written by the VM at throw time

    public Throwable() {
    }

    public Throwable(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    // "pkg.Class" or "pkg.Class: message" — the VM reads our runtime class name + the message field.
    public native String toString();

    // Print the exception header and, if the VM captured one, the stack trace beneath it.
    public void printStackTrace() {
        System.out.println(toString());
        if (backtrace != null) {
            System.out.println(backtrace);
        }
    }
}
