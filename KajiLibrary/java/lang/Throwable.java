package java.lang;

// KajiLibrary's java.lang.Throwable — the root of the whole exception hierarchy
// (Exception/Error and everything under them extend this). It extends Object
// *implicitly*, so like Object/String/Thread it inherits from nobody explicitly.
//
// Fuller than bootstrap/'s empty stub: it carries the detail `message` (real Java over
// KajiLibrary's String). The stack-trace machinery (`fillInStackTrace`,
// `getStackTrace`, `printStackTrace`) needs the VM to walk the frame stack, so it is
// deferred until KajiJDK exposes that intrinsic.
public class Throwable {

    // The detail message, or null if there is none. Set once at construction.
    private String message;

    public Throwable() {
        this.message = null;
    }

    public Throwable(String message) {
        this.message = message;
    }

    // The detail message given at construction (null if none).
    public String getMessage() {
        return this.message;
    }

    // By default the localized message is the plain message (subclasses may override).
    public String getLocalizedMessage() {
        return getMessage();
    }
}
