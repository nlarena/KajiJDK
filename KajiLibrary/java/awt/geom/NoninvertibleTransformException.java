package java.awt.geom;

// java.awt.geom.NoninvertibleTransformException de KajiLibrary. Es *checked* a proposito: una
// matriz con determinante cero no tiene inversa y el llamador tiene que decidir que hacer, no
// recibir una matriz de infinitos y seguir como si nada.
public class NoninvertibleTransformException extends Exception {

    public NoninvertibleTransformException(String s) {
        super(s);
    }
}
