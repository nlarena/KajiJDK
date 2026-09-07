package javax.swing.text;

import java.awt.Graphics;
import java.awt.Shape;

/**
 * Un resaltador que pinta <em>debajo</em> del texto, vista por vista.
 *
 * <p>La diferencia con {@link Highlighter} a secas esta en el orden: el de arriba pinta todo
 * despues del texto, asi que un fondo opaco lo taparia; este se mete en el dibujado de cada vista
 * y pinta antes. Es lo que permite que la seleccion tenga fondo solido y el texto se siga leyendo.
 */
public abstract class LayeredHighlighter implements Highlighter {

    protected LayeredHighlighter() {
    }

    /** Pinta los resaltados de ese tramo dentro de esa vista, antes de su texto. */
    public abstract void paintLayeredHighlights(Graphics g, int p0, int p1, Shape viewBounds,
            JTextComponent editor, View view);

    /**
     * Un pintor que trabaja por capas.
     *
     * <p>Devuelve la region que pinto, que es lo que el componente usa para saber que repintar
     * despues; un pintor comun no lo sabe.
     */
    public abstract static class LayerPainter implements Highlighter.HighlightPainter {

        protected LayerPainter() {
        }

        public abstract Shape paintLayer(Graphics g, int p0, int p1, Shape viewBounds,
                JTextComponent editor, View view);
    }
}
