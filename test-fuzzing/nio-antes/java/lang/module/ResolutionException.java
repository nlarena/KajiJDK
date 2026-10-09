package java.lang.module;

// KajiLibrary's java.lang.module.ResolutionException -- thrown when resolving a module graph fails.
public class ResolutionException extends RuntimeException {

    public ResolutionException() {
    }

    public ResolutionException(String msg) {
        super(msg);
    }

    public ResolutionException(Throwable cause) {
        super(cause);
    }

    public ResolutionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
