import java.awt.Color;
import java.awt.SystemColor;

/**
 * java.awt.SystemColor: los veintiseis papeles del escritorio.
 *
 * <p><b>Precondicion</b>: en el JDK real hay que correrla con {@code -Djava.awt.headless=true}. No
 * es un rodeo, es la comparacion que corresponde: sin escritorio los dos lados devuelven la tabla
 * por omision, y con escritorio el JDK devuelve los colores del tema del usuario -- que no son un
 * valor esperado sino una configuracion de la maquina. Los indices, la identidad y el formato del
 * toString, en cambio, valen siempre.
 *
 * <p>Lo que se prueba de fondo es la parte que no es obvia: cada constante es un objeto <b>unico</b>
 * cuyo color se lee de una tabla viva, asi que la identidad y el valor son cosas distintas.
 */
public class AwtSystemColorTest {

    public static int run() {
        int i = 0;

        // -- los indices son parte del API: identifican al papel
        if (SystemColor.DESKTOP != 0) { return i; } i++;
        if (SystemColor.ACTIVE_CAPTION != 1) { return i; } i++;
        if (SystemColor.ACTIVE_CAPTION_TEXT != 2) { return i; } i++;
        if (SystemColor.ACTIVE_CAPTION_BORDER != 3) { return i; } i++;
        if (SystemColor.INACTIVE_CAPTION != 4) { return i; } i++;
        if (SystemColor.INACTIVE_CAPTION_TEXT != 5) { return i; } i++;
        if (SystemColor.INACTIVE_CAPTION_BORDER != 6) { return i; } i++;
        if (SystemColor.WINDOW != 7) { return i; } i++;
        if (SystemColor.WINDOW_BORDER != 8) { return i; } i++;
        if (SystemColor.WINDOW_TEXT != 9) { return i; } i++;
        if (SystemColor.MENU != 10) { return i; } i++;
        if (SystemColor.MENU_TEXT != 11) { return i; } i++;
        if (SystemColor.TEXT != 12) { return i; } i++;
        if (SystemColor.TEXT_TEXT != 13) { return i; } i++;
        if (SystemColor.TEXT_HIGHLIGHT != 14) { return i; } i++;
        if (SystemColor.TEXT_HIGHLIGHT_TEXT != 15) { return i; } i++;
        if (SystemColor.TEXT_INACTIVE_TEXT != 16) { return i; } i++;
        if (SystemColor.CONTROL != 17) { return i; } i++;
        if (SystemColor.CONTROL_TEXT != 18) { return i; } i++;
        if (SystemColor.CONTROL_HIGHLIGHT != 19) { return i; } i++;
        if (SystemColor.CONTROL_LT_HIGHLIGHT != 20) { return i; } i++;
        if (SystemColor.CONTROL_SHADOW != 21) { return i; } i++;
        if (SystemColor.CONTROL_DK_SHADOW != 22) { return i; } i++;
        if (SystemColor.SCROLLBAR != 23) { return i; } i++;
        if (SystemColor.INFO != 24) { return i; } i++;
        if (SystemColor.INFO_TEXT != 25) { return i; } i++;
        if (SystemColor.NUM_COLORS != 26) { return i; } i++;

        // -- el toString imprime el INDICE y no el color: el color cambia con el tema
        if (!SystemColor.window.toString().equals("java.awt.SystemColor[i=7]")) { return i; } i++;
        if (!SystemColor.desktop.toString().equals("java.awt.SystemColor[i=0]")) { return i; } i++;
        if (!SystemColor.infoText.toString().equals("java.awt.SystemColor[i=25]")) { return i; } i++;

        // -- cada papel es un objeto unico, y eso es lo que se puede guardar en un campo
        if (SystemColor.window != SystemColor.window) { return i; } i++;
        if (SystemColor.window == SystemColor.text) { return i; } i++;
        // Es un Color, que es lo que lo hace utilizable en cualquier lado.
        if (!(SystemColor.window instanceof Color)) { return i; } i++;

        // -- getRGB coincide con las componentes, sea cual sea el tema
        SystemColor[] todos = new SystemColor[] {
            SystemColor.desktop, SystemColor.activeCaption, SystemColor.activeCaptionText,
            SystemColor.activeCaptionBorder, SystemColor.inactiveCaption,
            SystemColor.inactiveCaptionText, SystemColor.inactiveCaptionBorder,
            SystemColor.window, SystemColor.windowBorder, SystemColor.windowText,
            SystemColor.menu, SystemColor.menuText, SystemColor.text, SystemColor.textText,
            SystemColor.textHighlight, SystemColor.textHighlightText,
            SystemColor.textInactiveText, SystemColor.control, SystemColor.controlText,
            SystemColor.controlHighlight, SystemColor.controlLtHighlight,
            SystemColor.controlShadow, SystemColor.controlDkShadow, SystemColor.scrollbar,
            SystemColor.info, SystemColor.infoText,
        };
        if (todos.length != SystemColor.NUM_COLORS) { return i; } i++;
        for (int k = 0; k < todos.length; k++) {
            int rgb = todos[k].getRGB();
            if (todos[k].getRed() != ((rgb >> 16) & 0xFF)) { return i; }
            if (todos[k].getGreen() != ((rgb >> 8) & 0xFF)) { return i; }
            if (todos[k].getBlue() != (rgb & 0xFF)) { return i; }
            // Todos son opacos: un color de sistema translucido no tendria sentido.
            if (todos[k].getAlpha() != 255) { return i; }
            // Y el toString de cada uno nombra su indice.
            if (!todos[k].toString().equals("java.awt.SystemColor[i=" + k + "]")) { return i; }
        }
        i++;
        // El orden del arreglo de arriba es el de los indices: eso lo comprueba el toString.

        // -- los valores por omision (ver la precondicion de la clase)
        if (SystemColor.desktop.getRGB() != 0xFF005C5C) { return i; } i++;
        if (SystemColor.window.getRGB() != 0xFFFFFFFF) { return i; } i++;
        if (SystemColor.windowText.getRGB() != 0xFF000000) { return i; } i++;
        if (SystemColor.control.getRGB() != 0xFFC0C0C0) { return i; } i++;
        if (SystemColor.controlLtHighlight.getRGB() != 0xFFE0E0E0) { return i; } i++;
        if (SystemColor.textHighlight.getRGB() != 0xFF000080) { return i; } i++;
        if (SystemColor.info.getRGB() != 0xFFE0E000) { return i; } i++;
        if (SystemColor.scrollbar.getRGB() != 0xFFE0E0E0) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
