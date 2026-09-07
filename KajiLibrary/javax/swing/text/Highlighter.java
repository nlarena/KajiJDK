package javax.swing.text;

import java.awt.Graphics;
import java.awt.Shape;

/**
 * Quien pinta los fondos de colores del texto: la seleccion, los resultados de una busqueda.
 *
 * <p>Un resaltado es un tramo mas un {@link HighlightPainter} que sabe pintarlo. Separar las dos
 * cosas es lo que permite tener a la vez una seleccion azul y cinco coincidencias amarillas sin
 * que el componente sepa de ninguna.
 *
 * <p>{@link #addHighlight} devuelve una etiqueta opaca, no un indice: los tramos se mueven cuando
 * el documento cambia, y un indice dejaria de valer.
 */
public interface Highlighter {

    void install(JTextComponent c);

    void deinstall(JTextComponent c);

    void paint(Graphics g);

    /** Agrega un resaltado y devuelve con que sacarlo despues. */
    Object addHighlight(int p0, int p1, HighlightPainter p) throws BadLocationException;

    void removeHighlight(Object tag);

    void removeAllHighlights();

    /** Le cambia el tramo a un resaltado que ya esta; es como sigue la seleccion al cursor. */
    void changeHighlight(Object tag, int p0, int p1) throws BadLocationException;

    Highlight[] getHighlights();

    /** Quien sabe pintar un resaltado. */
    public interface HighlightPainter {

        /** Pinta el tramo {@code [p0, p1)} dentro de esa forma. */
        void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c);
    }

    /** Un resaltado puesto: su tramo y quien lo pinta. */
    public interface Highlight {

        int getStartOffset();

        int getEndOffset();

        HighlightPainter getPainter();
    }
}
