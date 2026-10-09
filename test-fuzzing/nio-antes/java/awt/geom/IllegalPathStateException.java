package java.awt.geom;

// KajiLibrary's java.awt.geom.IllegalPathStateException -- thrown when a Path2D is asked for an
// operation that needs a current point (lineTo, quadTo, curveTo, closePath) and there has not been
// any moveTo yet.
public class IllegalPathStateException extends RuntimeException {

    public IllegalPathStateException() {
        super();
    }

    public IllegalPathStateException(String s) {
        super(s);
    }
}
