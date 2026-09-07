package javax.swing.text;

import java.io.IOException;

/**
 * "El juego de caracteres no era el que dijiste, hay que empezar de nuevo."
 *
 * <p>La lanza un lector de HTML cuando encuentra, ya empezado el documento, una etiqueta que
 * declara otra codificacion. Es una excepcion y no un error porque el que lee <em>puede</em>
 * manejarla: cierra el flujo, lo vuelve a abrir con la codificacion que dice y arranca otra vez.
 *
 * <p>{@link #keyEqualsCharSet} distingue las dos formas de escribir esa declaracion en HTML, y
 * hace falta porque el texto que se guarda es distinto en cada caso.
 */
public class ChangedCharSetException extends IOException {

    String charSetSpec;
    boolean charSetKey;

    public ChangedCharSetException(String charSetSpec, boolean charSetKey) {
        this.charSetSpec = charSetSpec;
        this.charSetKey = charSetKey;
    }

    /** Lo que decia la declaracion. */
    public String getCharSetSpec() {
        return charSetSpec;
    }

    /** Si la declaracion venia como {@code charset=...} y no como el atributo entero. */
    public boolean keyEqualsCharSet() {
        return charSetKey;
    }
}
