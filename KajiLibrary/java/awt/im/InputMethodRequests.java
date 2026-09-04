package java.awt.im;

import java.awt.Rectangle;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;

/**
 * Lo que un componente de texto tiene que saber contestar para trabajar con un método de entrada.
 *
 * <p>Al escribir en japonés o en chino, el método de entrada necesita **preguntarle cosas al
 * componente**: dónde poner su ventana de candidatos, qué texto hay confirmado alrededor, qué está
 * seleccionado. Un componente que no implemente esto sólo puede recibir texto ya terminado.
 *
 * <p>{@link #cancelLatestCommittedText} es la más rara y la que explica el resto: permite
 * **deshacer** la última confirmación, porque en algunos métodos de entrada el usuario puede volver
 * atrás sobre una palabra que ya había aceptado y volver a elegir entre los candidatos.
 */
public interface InputMethodRequests {

    /**
     * Dónde está en pantalla esa posición del texto en composición.
     *
     * <p>Es lo que le permite al método de entrada poner su ventana de candidatos debajo del texto y
     * no en cualquier lado.
     */
    Rectangle getTextLocation(TextHitInfo offset);

    /** Qué posición del texto cae en ese punto de la pantalla, o `null` si ninguna. */
    TextHitInfo getLocationOffset(int x, int y);

    /** Dónde empezaría a insertarse el texto en composición. */
    int getInsertPositionOffset();

    /**
     * Un tramo del texto ya confirmado.
     *
     * @param attributes qué atributos interesan, o `null` si ninguno
     */
    AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex,
            AttributedCharacterIterator.Attribute[] attributes);

    /** Cuánto texto confirmado hay. */
    int getCommittedTextLength();

    /**
     * Deshace la última confirmación y devuelve lo que se sacó.
     *
     * @return lo que se deshizo, o `null` si el componente no lo admite
     */
    AttributedCharacterIterator cancelLatestCommittedText(
            AttributedCharacterIterator.Attribute[] attributes);

    /** El texto seleccionado, o `null` si no hay. */
    AttributedCharacterIterator getSelectedText(
            AttributedCharacterIterator.Attribute[] attributes);
}
