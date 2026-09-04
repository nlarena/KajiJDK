package javax.accessibility;

import javax.swing.text.AttributeSet;

/**
 * Texto accesible que además se puede **modificar**.
 *
 * <p>Es lo que permite que una ayuda técnica no sólo lea un campo sino que escriba en él: dictado
 por
 * voz, corrección automática, rellenado de formularios.
 *
 * <p>{@link #cut} y {@link #paste} están además de {@link #delete} e {@link #insertTextAtIndex}
 * porque no son lo mismo: cortar deja el texto en el portapapeles y borrar no.
 */
public interface AccessibleEditableText extends AccessibleText {

    /** Reemplaza todo el texto. */
    void setTextContents(String s);

    /** Inserta texto en esa posición. */
    void insertTextAtIndex(int index, String s);

    /** El texto de ese tramo. */
    String getTextRange(int startIndex, int endIndex);

    /** Borra ese tramo. */
    void delete(int startIndex, int endIndex);

    /** Corta ese tramo al portapapeles. */
    void cut(int startIndex, int endIndex);

    /** Pega el portapapeles en esa posición. */
    void paste(int startIndex);

    /** Reemplaza ese tramo. */
    void replaceText(int startIndex, int endIndex, String s);

    /** Selecciona ese tramo. */
    void selectText(int startIndex, int endIndex);

    /** Le cambia los atributos a ese tramo. */
    void setAttributes(int startIndex, int endIndex, AttributeSet as);
}
