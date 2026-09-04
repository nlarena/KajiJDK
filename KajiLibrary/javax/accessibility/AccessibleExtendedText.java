package javax.accessibility;

import java.awt.Rectangle;

/**
 * Texto accesible que se puede pedir **de a tramos** en vez de de a unidades.
 *
 * <p>La diferencia con {@link AccessibleText} está en lo que devuelve: allá una cadena suelta, acá un
 * {@link AccessibleTextSequence} que además dice **dónde empieza y dónde termina**. Sin eso, quien
 * lee tiene que adivinar la posición del texto que recibió, y con unidades de largo variable eso no
 * se puede.
 *
 * <p>Agrega dos unidades que la otra no tiene: la línea y el "atributo homogéneo", que es el tramo
 * más largo alrededor de una posición que se dibuja todo igual.
 */
public interface AccessibleExtendedText {

    /** La unidad "una línea". */
    int LINE = 4;

    /** La unidad "un tramo dibujado todo igual". */
    int ATTRIBUTE_RUN = 5;

    /** El texto de ese tramo. */
    String getTextRange(int startIndex, int endIndex);

    /** La unidad que contiene a ese índice, con sus límites. */
    AccessibleTextSequence getTextSequenceAt(int part, int index);

    /** La unidad siguiente, con sus límites. */
    AccessibleTextSequence getTextSequenceAfter(int part, int index);

    /** La unidad anterior, con sus límites. */
    AccessibleTextSequence getTextSequenceBefore(int part, int index);

    /** Dónde cae ese tramo en la pantalla. */
    Rectangle getTextBounds(int startIndex, int endIndex);
}
