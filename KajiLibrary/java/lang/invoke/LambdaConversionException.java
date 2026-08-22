package java.lang.invoke;

// The lambda metafactory could not build the call site: the functional interface and the
// implementation method do not fit together. Checked, because it is raised while LINKING a
// bootstrap and the bootstrap is expected to report why.
public class LambdaConversionException extends Exception {

    public LambdaConversionException() {
        super();
    }

    public LambdaConversionException(String message) {
        super(message);
    }
}
