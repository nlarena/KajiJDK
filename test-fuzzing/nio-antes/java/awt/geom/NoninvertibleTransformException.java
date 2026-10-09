package java.awt.geom;

// KajiLibrary's java.awt.geom.NoninvertibleTransformException. It is *checked* on purpose: a
// matrix with a zero determinant has no inverse and the caller has to decide what to do, not
// receive a matrix of infinities and carry on as if nothing had happened.
public class NoninvertibleTransformException extends Exception {

    public NoninvertibleTransformException(String s) {
        super(s);
    }
}
