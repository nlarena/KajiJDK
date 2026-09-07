package javax.swing;

import java.awt.Container;

/**
 * Cuanto espacio va entre dos componentes, segun las reglas del sistema.
 *
 * <h2>El espaciado no es una preferencia, es una regla de la plataforma</h2>
 *
 * <p>Cada sistema tiene su guia de estilo, y ahi dice cuantos pixeles van entre una etiqueta y su
 * campo, entre dos botones de la misma barra, y entre el borde de un dialogo y su contenido. No son
 * los mismos numeros en Windows, en macOS y en GNOME, y una aplicacion que los fija a mano se ve
 * fuera de lugar en dos de los tres.
 *
 * <p>Esta clase es donde el aspecto instalado contesta esas dos preguntas -- {@link #getPreferredGap}
 * entre dos componentes y {@link #getContainerGap} contra el borde -- y es lo que
 * {@link GroupLayout} consulta cuando se le pide un espacio "el que corresponda".
 *
 * <h2>Tres clases de vecindad</h2>
 *
 * <p>{@link ComponentPlacement#RELATED} para dos cosas que van juntas -- la etiqueta y su campo --,
 * {@link ComponentPlacement#UNRELATED} para dos grupos distintos, y
 * {@link ComponentPlacement#INDENT} para lo que cuelga de otra cosa, como la casilla que solo tiene
 * sentido si la de arriba esta marcada.
 *
 * <h2>De donde sale la instancia</h2>
 *
 * <p>De {@link #setInstance} si alguien la puso, y si no del aspecto instalado. Sin aspecto no hay
 * guia de estilo que consultar, y {@link #getInstance} devuelve una que da los numeros del JDK
 * --seis pixeles entre cosas relacionadas, doce entre grupos y doce contra el borde--, medidos
 * contra el suyo. Un aspecto de verdad los reemplaza por los de su plataforma.
 */
public abstract class LayoutStyle {

    private static LayoutStyle instance;

    /** Fija la instancia; nulo devuelve la decision al aspecto. */
    public static void setInstance(LayoutStyle style) {
        synchronized (LayoutStyle.class) {
            instance = style;
        }
    }

    /**
     * La instancia en uso.
     *
     * <p>Ver la nota de la clase: sin aspecto instalado, una con los numeros de siempre.
     */
    public static LayoutStyle getInstance() {
        LayoutStyle style;
        synchronized (LayoutStyle.class) {
            style = instance;
        }
        if (style != null) {
            return style;
        }
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf != null) {
            LayoutStyle delAspecto = laf.getLayoutStyle();
            if (delAspecto != null) {
                return delAspecto;
            }
        }
        return DeSiempre.UNICA;
    }

    /** Para las subclases. */
    public LayoutStyle() {
    }

    /**
     * El espacio que va entre esos dos componentes.
     *
     * @param position de que lado esta el segundo respecto del primero; una de las constantes de
     *     {@link SwingConstants}
     * @throws IllegalArgumentException si algun componente o la posicion son invalidos
     */
    public abstract int getPreferredGap(JComponent component1, JComponent component2,
            ComponentPlacement type, int position, Container parent);

    /**
     * El espacio que va entre ese componente y el borde de su contenedor.
     *
     * @throws IllegalArgumentException si el componente o la posicion son invalidos
     */
    public abstract int getContainerGap(JComponent component, int position, Container parent);

    /** Que relacion hay entre los dos componentes; ver la nota de la clase. */
    public enum ComponentPlacement {

        /** Van juntos: una etiqueta y su campo. */
        RELATED,

        /** Son grupos distintos. */
        UNRELATED,

        /** El segundo cuelga del primero. */
        INDENT;
    }

    /**
     * La que se usa sin aspecto instalado.
     *
     * <p>Los numeros son los que casi todas las guias comparten. No estan medidos contra ninguna
     * plataforma en particular, y por eso esta clase no es publica: quien quiera los de su sistema
     * tiene que instalar el aspecto que los sepa.
     */
    private static class DeSiempre extends LayoutStyle {

        static final LayoutStyle UNICA = new DeSiempre();

        public int getPreferredGap(JComponent component1, JComponent component2,
                ComponentPlacement type, int position, Container parent) {
            // Un componente nulo sale como NullPointerException, no como IllegalArgumentException:
            // el JDK no lo comprueba y revienta al usarlo. Esta medido.
            component1.getWidth();
            component2.getWidth();
            if (type == null) {
                throw new NullPointerException("type");
            }
            checkPosition(position);
            if (type == ComponentPlacement.INDENT
                    && (position == SwingConstants.EAST || position == SwingConstants.WEST)) {
                return 12;
            }
            return (type == ComponentPlacement.UNRELATED) ? 12 : 6;
        }

        public int getContainerGap(JComponent component, int position, Container parent) {
            component.getWidth();
            checkPosition(position);
            return 12;
        }

        private static void checkPosition(int position) {
            if (position != SwingConstants.NORTH && position != SwingConstants.SOUTH
                    && position != SwingConstants.EAST && position != SwingConstants.WEST) {
                throw new IllegalArgumentException("Invalid position");
            }
        }
    }
}
