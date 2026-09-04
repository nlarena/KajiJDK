package javax.accessibility;

import javax.swing.text.AttributeSet;

/**
 * Un tramo de texto que comparte los mismos atributos.
 *
 * <p>Es un registro de tres campos y por eso los tiene **públicos**: agregarle métodos de acceso a
 * algo que se devuelve por decenas al recorrer un documento sería ceremonia sin contenido.
 */
public class AccessibleAttributeSequence {

    /** Dónde empieza el tramo. */
    public int startIndex;

    /** Dónde termina. */
    public int endIndex;

    /** Los atributos que comparte. */
    public AttributeSet attributes;

    /** Un tramo vacío. */
    public AccessibleAttributeSequence() {
    }

    /** Con el tramo y sus atributos. */
    public AccessibleAttributeSequence(int start, int end, AttributeSet a) {
        this.startIndex = start;
        this.endIndex = end;
        this.attributes = a;
    }
}
