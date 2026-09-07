package javax.swing.plaf.synth;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * Un icono que sabe en que estado esta lo que lo lleva.
 *
 * <h2>Por que un icono necesita el contexto</h2>
 *
 * <p>Un {@link Icon} comun se dibuja siempre igual. Uno de Synth no: la tilde de una casilla
 * apagada, apretada o con el cursor encima son tres imagenes distintas, y hasta el
 * <strong>tamano</strong> puede cambiar entre estados. Por eso las tres operaciones de {@code Icon}
 * tienen aca una version que toma un {@link SynthContext}.
 *
 * <p>Las tres viejas siguen andando: son {@code default} y llaman a las nuevas con un contexto
 * nulo. Un icono que dependa del estado devolvera algo generico; uno que no, lo mismo de siempre.
 * Eso es lo que permite pasarle un {@code SynthIcon} a cualquier cosa que espere un {@code Icon}.
 */
public interface SynthIcon extends Icon {

    /**
     * El ancho en ese contexto.
     *
     * @param context el contexto, o {@code null}
     * @return el ancho
     */
    int getIconWidth(SynthContext context);

    /**
     * El alto en ese contexto.
     *
     * @param context el contexto, o {@code null}
     * @return el alto
     */
    int getIconHeight(SynthContext context);

    /**
     * Dibuja el icono en ese contexto.
     *
     * @param context el contexto, o {@code null}
     * @param g donde dibujar
     * @param x la esquina
     * @param y la esquina
     * @param w el ancho pedido
     * @param h el alto pedido
     */
    void paintIcon(SynthContext context, Graphics g, int x, int y, int w, int h);

    /** Sin contexto; ver la nota de la interfaz. */
    default int getIconWidth() {
        return getIconWidth(null);
    }

    /** Sin contexto. */
    default int getIconHeight() {
        return getIconHeight(null);
    }

    /** Sin contexto, y con el tamano que el icono diga. */
    default void paintIcon(Component c, Graphics g, int x, int y) {
        paintIcon(null, g, x, y, getIconWidth(null), getIconHeight(null));
    }
}
