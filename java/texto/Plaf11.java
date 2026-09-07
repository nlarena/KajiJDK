import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicDesktopIconUI;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * La ventana interna: su barra de titulo, su aspecto y su icono, contra el JDK.
 *
 * <p>Los tamanos no se comparan crudos: los cuatro iconos de los botones vienen de la tabla del
 * aspecto y aca no hay ninguna, asi que la barra mide menos de ancho. Se compara la estructura, los
 * colores, y el comportamiento de las acciones.
 *
 * <p>Tampoco se compara {@code getPreferredSize} de una ventana suelta: el JDK desborda la pila ahi
 * -- ver la nota de {@code BasicInternalFrameUI} --.
 */
public class Plaf11 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    static class Barra extends BasicInternalFrameTitlePane {
        Barra(JInternalFrame f) {
            super(f);
        }

        static String comandos() {
            return CLOSE_CMD + "/" + ICONIFY_CMD + "/" + MAXIMIZE_CMD + "/" + RESTORE_CMD
                    + "/" + MOVE_CMD + "/" + SIZE_CMD;
        }

        String estado() {
            return "ventana=" + (frame != null)
                    + " barra de menu=" + (menuBar != null)
                    + " items del menu=" + (windowMenu == null ? -1 : windowMenu.getItemCount())
                    + " botones: cerrar=" + (closeButton != null)
                    + " max=" + (maxButton != null) + " icono=" + (iconButton != null)
                    + " propiedad=" + (propertyChangeListener != null);
        }

        String acciones() {
            return "cerrar=" + (closeAction != null) + " max=" + (maximizeAction != null)
                    + " icono=" + (iconifyAction != null) + " restaurar=" + (restoreAction != null)
                    + " mover=" + (moveAction != null) + " tamano=" + (sizeAction != null);
        }

        String prendidas() {
            return "cerrar=" + closeAction.isEnabled() + " max=" + maximizeAction.isEnabled()
                    + " icono=" + iconifyAction.isEnabled()
                    + " restaurar=" + restoreAction.isEnabled()
                    + " mover=" + moveAction.isEnabled() + " tamano=" + sizeAction.isEnabled();
        }

        String colores() {
            return "elegido: titulo=" + col(selectedTitleColor)
                    + " texto=" + col(selectedTextColor)
                    + " | no elegido: titulo=" + col(notSelectedTitleColor)
                    + " texto=" + col(notSelectedTextColor);
        }

        String titulo(String t, int ancho) {
            FontMetrics fm = getFontMetrics(getFont());
            return getTitle(t, fm, ancho);
        }

        String acomodador() {
            return corto(createLayout());
        }
    }

    static class Ventana extends BasicInternalFrameUI {
        Ventana(JInternalFrame f) {
            super(f);
        }

        String estado() {
            return "ventana=" + (frame != null) + " norte=" + corto(northPane)
                    + " sur=" + corto(southPane) + " este=" + corto(eastPane)
                    + " oeste=" + corto(westPane) + " barra=" + corto(titlePane)
                    + " acomodador=" + corto(internalFrameLayout);
        }

        String escuchas() {
            return "borde=" + (borderListener != null)
                    + " propiedad=" + (propertyChangeListener != null)
                    + " componente=" + (componentListener != null)
                    + " cristal=" + (glassPaneDispatcher != null)
                    + " tecla del menu=" + openMenuKey;
        }

        String teclas() {
            return "registrada=" + isKeyBindingRegistered() + " activa=" + isKeyBindingActive();
        }

        String administrador() {
            return corto(getDesktopManager()) + " / " + corto(createDesktopManager());
        }
    }

    static void barras() {
        linea("--- BasicInternalFrameTitlePane ---");
        linea("comandos=" + Barra.comandos());
        JInternalFrame f = new JInternalFrame("Titulo", true, true, true, true);
        Barra t = new Barra(f);
        linea("estado: " + t.estado());
        linea("acciones: " + t.acciones());
        linea("prendidas: " + t.prendidas());
        linea("colores: " + t.colores());
        linea("fuente=" + fue(t.getFont()) + " opaca=" + t.isOpaque()
                + " acomodador=" + corto(t.getLayout()) + " creado=" + t.acomodador()
                + " hijos=" + t.getComponentCount());
        linea("titulo nulo='" + t.titulo(null, 400) + "'");
        linea("titulo vacio='" + t.titulo("", 400) + "'");
        linea("titulo que entra='" + t.titulo("ab", 400) + "'");
        String cortado = t.titulo("Un titulo bastante largo para que no entre", 30);
        linea("titulo largo termina en puntos=" + cortado.endsWith("...")
                + " y es mas corto que el original="
                + (cortado.length() < "Un titulo bastante largo para que no entre".length()));

        // Una ventana que no permite nada tiene las acciones apagadas.
        JInternalFrame quieta = new JInternalFrame("q", false, false, false, false);
        Barra tq = new Barra(quieta);
        linea("ventana sin permisos: " + tq.prendidas());
    }

    static void ventanas() {
        linea("--- BasicInternalFrameUI ---");
        JInternalFrame f = new JInternalFrame("Titulo", true, true, true, true);
        Ventana u = new Ventana(f);
        linea("comparte instancia="
                + (BasicInternalFrameUI.createUI(f) == BasicInternalFrameUI.createUI(f)));
        // Por `setUI` y no por `installUI` a mano: la ventana apaga la comprobacion de panel raiz
        // mientras instala, y sin eso los paneles irian a parar al contenido en vez de a la
        // ventana. Es la via de verdad, y ademas saca del medio el aspecto que ya estaba puesto.
        f.setUI(u);
        linea("estado: " + u.estado());
        linea("escuchas: " + u.escuchas());
        linea("teclas: " + u.teclas());
        linea("paneles: norte=" + (u.getNorthPane() != null)
                + " sur=" + (u.getSouthPane() != null)
                + " este=" + (u.getEastPane() != null)
                + " oeste=" + (u.getWestPane() != null));
        linea("administrador=" + u.administrador());
        linea("fondo=" + col(f.getBackground()) + " opaca=" + f.isOpaque()
                + " acomodador=" + corto(f.getLayout()) + " hijos=" + f.getComponentCount());
        // De otro componente los tres tamanos son numeros fijos.
        JPanel otro = new JPanel();
        linea("de otro componente: preferido=" + u.getPreferredSize(otro)
                + " minimo=" + u.getMinimumSize(otro) + " maximo=" + u.getMaximumSize(otro));
        linea("maximo de la ventana=" + u.getMaximumSize(f));
    }

    static void iconos() {
        linea("--- BasicDesktopIconUI ---");
        JInternalFrame f = new JInternalFrame("Titulo", true, true, true, true);
        JInternalFrame.JDesktopIcon di = f.getDesktopIcon();
        BasicDesktopIconUI u = (BasicDesktopIconUI) BasicDesktopIconUI.createUI(di);
        linea("comparte instancia="
                + (BasicDesktopIconUI.createUI(di) == BasicDesktopIconUI.createUI(di)));
        di.setUI(u);
        linea("acomodador=" + corto(di.getLayout()) + " hijos=" + di.getComponentCount());
        linea("maximo=" + u.getMaximumSize(di));
        // El icono es una barra de titulo; ver la nota de la clase.
        boolean esBarra = false;
        for (int i = 0; i < di.getComponentCount(); i++) {
            if (di.getComponent(i) instanceof BasicInternalFrameTitlePane) {
                esBarra = true;
            }
        }
        linea("adentro hay una barra de titulo=" + esBarra);
        linea("el preferido es el del acomodador="
                + u.getPreferredSize(di).equals(
                        di.getLayout().preferredLayoutSize(di)));
    }

    public static int run() {
        barras();
        ventanas();
        iconos();
        return 0;
    }
}
