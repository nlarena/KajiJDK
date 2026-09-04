package javax.accessibility;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.text.AttributeSet;

/**
 * Lo implementa lo que muestra texto que se puede recorrer.
 *
 * <p>Lo distintivo es que el texto se pide **por unidad**: un carácter, una palabra o una oración, y
 * eso lo dicen las tres constantes. Una ayuda técnica que lee en voz alta no quiere el texto entero
 * ni letra por letra; quiere la oración donde está el cursor.
 *
 * <p>Los tres métodos de lectura —{@link #getAtIndex}, {@link #getAfterIndex},
 * {@link #getBeforeIndex}— existen por lo mismo: permiten moverse por el texto en la unidad que le
 * sirva a quien lee, sin traerse todo.
 */
public interface AccessibleText {

    /** La unidad "un carácter". */
    int CHARACTER = 1;

    /** La unidad "una palabra". */
    int WORD = 2;

    /** La unidad "una oración". */
    int SENTENCE = 3;

    /**
     * Qué carácter cae en ese punto.
     *
     * @return el índice, o -1 si el punto cae fuera del texto
     */
    int getIndexAtPoint(Point p);

    /**
     * Dónde está ese carácter en la pantalla.
     *
     * @return el rectángulo, o `null` si el índice no existe
     */
    Rectangle getCharacterBounds(int i);

    /** Cuántos caracteres hay. */
    int getCharCount();

    /** Dónde está el cursor. */
    int getCaretPosition();

    /**
     * La unidad que empieza en ese índice.
     *
     * @param part {@link #CHARACTER}, {@link #WORD} o {@link #SENTENCE}
     */
    String getAtIndex(int part, int index);

    /** La unidad siguiente a la de ese índice. */
    String getAfterIndex(int part, int index);

    /** La unidad anterior a la de ese índice. */
    String getBeforeIndex(int part, int index);

    /** Con qué atributos se dibuja ese carácter. */
    AttributeSet getCharacterAttribute(int i);

    /** Dónde empieza la selección. */
    int getSelectionStart();

    /** Dónde termina la selección. */
    int getSelectionEnd();

    /** El texto seleccionado, o `null` si no hay. */
    String getSelectedText();
}
