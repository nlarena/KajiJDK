package java.awt.geom;

// java.awt.geom.IllegalPathStateException de KajiLibrary -- se tira cuando a un Path2D se le pide
// una operacion que necesita un punto actual (lineTo, quadTo, curveTo, closePath) y todavia no hubo
// ningun moveTo.
public class IllegalPathStateException extends RuntimeException {

    public IllegalPathStateException() {
        super();
    }

    public IllegalPathStateException(String s) {
        super(s);
    }
}
