package javax.accessibility;

/**
 * Un tramo de texto con su contenido.
 *
 * <p>Como {@link AccessibleAttributeSequence}, es un registro de campos públicos: se devuelve muchas
 * veces al recorrer un documento y no gana nada con encapsulamiento.
 */
public class AccessibleTextSequence {

    /** Dónde empieza el tramo. */
    public int startIndex;

    /** Dónde termina. */
    public int endIndex;

    /** El texto del tramo. */
    public String text;

    /** Un tramo vacío. */
    public AccessibleTextSequence() {
    }

    /** Con el tramo y su texto. */
    public AccessibleTextSequence(int start, int end, String txt) {
        this.startIndex = start;
        this.endIndex = end;
        this.text = txt;
    }
}
