package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * Los iconos del aspecto Metal; por ahora, la casilla y el boton de radio.
 *
 * <h2>Iconos medidos, no dibujados</h2>
 *
 * <p>En el JDK estos iconos se dibujan con lineas, arcos y un degradado ({@code Button.gradient})
 * sobre colores del tema. Aca estan como mapas de pixeles: cada estado de cada icono es lo que el
 * JDK 25 pinto, medido pixel por pixel en el tema Ocean. Es la misma sustitucion que
 * {@code jdk.internal.awt.FuenteBitmap}: el resultado exacto, en vez del procedimiento.
 *
 * <p>Los mapas usan una letra por color: {@code #} la sombra oscura (122, 138, 153), {@code p} el
 * control primario (184, 207, 229), {@code o} el negro del tema (51, 51, 51), {@code q} el gris
 * inactivo (153, 153, 153), {@code g} la fila correspondiente del degradado, y {@code .} nada. El
 * degradado va de arriba abajo y depende de la altura, asi que cada icono tiene su tabla de filas.
 *
 * <p>Los estados salen del modelo del boton, de mas a menos especifico: deshabilitado, apretado y
 * armado, con el cursor encima, y en reposo; cada uno con y sin seleccion. Un icono apretado
 * baja la tilde de la casilla un pixel, como baja el texto del boton.
 */
public class MetalIconFactory implements Serializable {

    private static final Color SOMBRA_OSCURA = new Color(122, 138, 153);
    private static final Color CONTROL_PRIMARIO = new Color(184, 207, 229);
    private static final Color NEGRO = new Color(51, 51, 51);
    private static final Color INACTIVO = new Color(153, 153, 153);

    private static Icon checkBoxIcon;
    private static Icon radioButtonIcon;

    public MetalIconFactory() {
    }

    /** El icono de casilla, compartido. */
    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    /** El icono de boton de radio, compartido. */
    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    /** Pinta un mapa de pixeles en ese lugar; ver la leyenda en la nota de la clase. */
    static void pintarMapa(Graphics g, int x, int y, String[] mapa, Color[] degradado) {
        for (int fila = 0; fila < mapa.length; fila++) {
            String linea = mapa[fila];
            for (int col = 0; col < linea.length(); col++) {
                char c = linea.charAt(col);
                Color color;
                if (c == '#') {
                    color = SOMBRA_OSCURA;
                } else if (c == 'p') {
                    color = CONTROL_PRIMARIO;
                } else if (c == 'o') {
                    color = NEGRO;
                } else if (c == 'q') {
                    color = INACTIVO;
                } else if (c == 'g') {
                    color = degradado[fila];
                } else {
                    continue;
                }
                g.setColor(color);
                g.fillRect(x + col, y + fila, 1, 1);
            }
        }
    }

    /** Que mapa corresponde al estado del modelo; el orden es el de la nota de la clase. */
    static int estado(ButtonModel m) {
        boolean sel = m.isSelected();
        if (!m.isEnabled()) {
            return sel ? 5 : 4;
        }
        if (m.isPressed() && m.isArmed()) {
            return sel ? 3 : 2;
        }
        if (m.isRollover()) {
            return sel ? 7 : 6;
        }
        return sel ? 1 : 0;
    }

    /** La casilla de Ocean: 13 por 13, medida. */
    private static class CheckBoxIcon implements Icon, UIResource, Serializable {

        private static final Color[] DEGRADADO = {
            null, new Color(0xE8EFF6), new Color(0xF3F7FA), new Color(0xFFFFFF),
            new Color(0xF3F7FB), new Color(0xE8EFF7), new Color(0xDDE8F3), new Color(0xD7E4F1),
            new Color(0xD2E0EF), new Color(0xCDDDED), new Color(0xC7D9EB), new Color(0xC2D6E9),
            null };

        private static final String[] NORMAL = {
            "#############",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#############" };

        private static final String[] SELECCIONADO = {
            "#############",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggogg#",
            "#gggggggoogg#",
            "#ggooggooggg#",
            "#ggoogoogggg#",
            "#ggooooggggg#",
            "#ggooogggggg#",
            "#ggooggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#############" };

        private static final String[] APRETADO = {
            "#############",
            "#############",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "#############" };

        private static final String[] APRETADO_SELECCIONADO = {
            "#############",
            "#############",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppopp#",
            "##ppppppoopp#",
            "##pooppooppp#",
            "##poopoopppp#",
            "##pooooppppp#",
            "##pooopppppp#",
            "##pooppppppp#",
            "##pppppppppp#",
            "#############" };

        private static final String[] DESHABILITADO = {
            "#############",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#############" };

        private static final String[] DESHABILITADO_SELECCIONADO = {
            "#############",
            "#...........#",
            "#...........#",
            "#........#..#",
            "#.......##..#",
            "#..##..##...#",
            "#..##.##....#",
            "#..####.....#",
            "#..###......#",
            "#..##.......#",
            "#...........#",
            "#...........#",
            "#############" };

        private static final String[] ROLLOVER = {
            "#############",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#############" };

        private static final String[] ROLLOVER_SELECCIONADO = {
            "#############",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#ppggggggopp#",
            "#ppgggggoopp#",
            "#ppooggoogpp#",
            "#ppoogooggpp#",
            "#ppoooogggpp#",
            "#ppoooggggpp#",
            "#ppoogggggpp#",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#############" };

        private static final String[][] MAPAS = { NORMAL, SELECCIONADO, APRETADO,
            APRETADO_SELECCIONADO, DESHABILITADO, DESHABILITADO_SELECCIONADO, ROLLOVER,
            ROLLOVER_SELECCIONADO };

        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel modelo = ((AbstractButton) c).getModel();
            pintarMapa(g, x, y, MAPAS[estado(modelo)], DEGRADADO);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    /** El boton de radio de Ocean: 13 por 13, medido; la ultima fila queda vacia. */
    private static class RadioButtonIcon implements Icon, UIResource, Serializable {

        private static final Color[] DEGRADADO = {
            null, new Color(0xDDE8F3), new Color(0xE8EFF6), new Color(0xF3F7FA),
            new Color(0xFFFFFF), new Color(0xF3F7FB), new Color(0xE8EFF7), new Color(0xDDE8F3),
            new Color(0xD3E1EF), new Color(0xCADBEC), new Color(0xC1D5E8), null, null };

        private static final String[] NORMAL = {
            "....####.....",
            "..##gggg##...",
            ".#gggggggg#..",
            ".#gggggggg#..",
            "#gggggggggg#.",
            "#gggggggggg#.",
            "#gggggggggg#.",
            "#gggggggggg#.",
            ".#gggggggg#..",
            ".#gggggggg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] SELECCIONADO = {
            "....####.....",
            "..##gggg##...",
            ".#gggggggg#..",
            ".#ggoooogg#..",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            ".#ggoooogg#..",
            ".#gggggggg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] APRETADO = {
            "....####.....",
            "..########...",
            ".#.#ppppp.#..",
            ".##ppppppp#..",
            "##pppppppp.#.",
            "##pppppppp.#.",
            "##pppppppp.#.",
            "##pppppppp.#.",
            ".#pppppppp#..",
            ".##pppppp.#..",
            "..##....##...",
            "....####.....",
            "............." };

        private static final String[] APRETADO_SELECCIONADO = {
            "....####.....",
            "..########...",
            ".#.#ppppp.#..",
            ".##poooopp#..",
            "##poooooop.#.",
            "##poooooop.#.",
            "##poooooop.#.",
            "##poooooop.#.",
            ".#ppoooopp#..",
            ".##pppppp.#..",
            "..##....##...",
            "....####.....",
            "............." };

        private static final String[] DESHABILITADO = {
            "....qqqq.....",
            "..qq....qq...",
            ".q........q..",
            ".q........q..",
            "q..........q.",
            "q..........q.",
            "q..........q.",
            "q..........q.",
            ".q........q..",
            ".q........q..",
            "..qq....qq...",
            "....qqqq.....",
            "............." };

        private static final String[] DESHABILITADO_SELECCIONADO = {
            "....qqqq.....",
            "..qq....qq...",
            ".q........q..",
            ".q..####..q..",
            "q..######..q.",
            "q..######..q.",
            "q..######..q.",
            "q..######..q.",
            ".q..####..q..",
            ".q........q..",
            "..qq....qq...",
            "....qqqq.....",
            "............." };

        private static final String[] ROLLOVER = {
            "....####.....",
            "..##gggg##...",
            ".#ggppppgg#..",
            ".#gpggggpg#..",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            ".#gpggggpg#..",
            ".#ggppppgg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] ROLLOVER_SELECCIONADO = {
            "....####.....",
            "..##gggg##...",
            ".#ggppppgg#..",
            ".#gpoooopg#..",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            ".#gpoooopg#..",
            ".#ggppppgg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[][] MAPAS = { NORMAL, SELECCIONADO, APRETADO,
            APRETADO_SELECCIONADO, DESHABILITADO, DESHABILITADO_SELECCIONADO, ROLLOVER,
            ROLLOVER_SELECCIONADO };

        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel modelo = ((AbstractButton) c).getModel();
            pintarMapa(g, x, y, MAPAS[estado(modelo)], DEGRADADO);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }
}
