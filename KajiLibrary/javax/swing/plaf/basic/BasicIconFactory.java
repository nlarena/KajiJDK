package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * Los iconos que el aspecto basico le pone a las casillas, los redondeles y los menus.
 *
 * <h2>Casi todos estan vacios, y es a proposito</h2>
 *
 * <p>Cinco de los ocho no dibujan nada: solo ocupan lugar. El aspecto basico no tiene un dibujo
 * propio de casilla ni de flecha de menu --Metal, Windows y GTK lo tienen, y cada uno el suyo--,
 * pero el <em>tamano</em> si tiene que estar, porque de el sale la sangria del texto y la
 * alineacion de una columna de items. Un icono de 13 x 13 que no pinta nada deja el hueco donde el
 * aspecto de verdad va a poner el suyo.
 *
 * <p>Los dos que si dibujan son los de menu con estado: el tilde del item marcable y el punto del
 * item de opcion, y solo cuando el item esta elegido.
 *
 * <h2>Los tamanos, medidos</h2>
 *
 * <p>Casilla y redondel 13 x 13; tilde y punto de menu 9 x 9 y 6 x 6; el hueco del tilde 9 x 9; las
 * dos flechas 4 x 8; el icono vacio de ventana interna 14 x 16. Todos medidos en el JDK 25.
 *
 * <h2>Un objeto por tipo</h2>
 *
 * <p>Cada metodo devuelve siempre la misma instancia. Un icono sin estado se puede compartir entre
 * todos los componentes de la pantalla, y son varios cientos.
 */
public class BasicIconFactory implements Serializable {

    private static Icon frameIcon;
    private static Icon checkBoxIcon;
    private static Icon radioButtonIcon;
    private static Icon checkBoxMenuItemIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon menuItemCheckIcon;
    private static Icon menuItemArrowIcon;
    private static Icon menuArrowIcon;

    public BasicIconFactory() {
    }

    /** El de una casilla de verificacion: 13 x 13 y vacio. */
    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    /** El de un redondel de opcion: 13 x 13 y vacio. */
    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    /** El tilde de un item de menu marcable: 9 x 9, dibuja solo si esta marcado. */
    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    /** El punto de un item de menu de opcion: 6 x 6, dibuja solo si esta elegido. */
    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    /** El hueco donde va el tilde de un item: 9 x 9 y vacio. */
    public static Icon getMenuItemCheckIcon() {
        if (menuItemCheckIcon == null) {
            menuItemCheckIcon = new MenuItemCheckIcon();
        }
        return menuItemCheckIcon;
    }

    /** El hueco de la flecha de submenu de un item: 4 x 8 y vacio. */
    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    /** La flecha que dice que un menu tiene submenu: 4 x 8 y vacio. */
    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    /** El icono de una ventana interna sin icono propio: 14 x 16 y vacio. */
    public static Icon createEmptyFrameIcon() {
        if (frameIcon == null) {
            frameIcon = new EmptyFrameIcon();
        }
        return frameIcon;
    }

    // Los nombres de estas clases son los del JDK aunque sean privadas: se ven por getClass() y la
    // prueba diferencial los compara. Ver la nota de JTable sobre lo mismo.

    private static class CheckBoxIcon implements Icon, Serializable {

        static final int csize = 13;

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return csize;
        }

        public int getIconHeight() {
            return csize;
        }
    }

    private static class RadioButtonIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    private static class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable {

        /** El tilde: dos trazos, uno corto bajando y uno largo subiendo. */
        public void drawCheck(Component c, Graphics g, int x, int y) {
            int w = getIconWidth();
            int h = getIconHeight();
            g.drawLine(x + 1, y + h / 2, x + w / 3, y + h - 2);
            g.drawLine(x + w / 3, y + h - 2, x + w - 2, y + 1);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            if (model.isSelected()) {
                drawCheck(c, g, x, y);
            }
        }

        public int getIconWidth() {
            return 9;
        }

        public int getIconHeight() {
            return 9;
        }
    }

    private static class RadioButtonMenuItemIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            if (b.isSelected()) {
                g.fillOval(x, y, getIconWidth() - 1, getIconHeight() - 1);
            }
        }

        public int getIconWidth() {
            return 6;
        }

        public int getIconHeight() {
            return 6;
        }
    }

    private static class MenuItemCheckIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 9;
        }

        public int getIconHeight() {
            return 9;
        }
    }

    private static class MenuItemArrowIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 4;
        }

        public int getIconHeight() {
            return 8;
        }
    }

    private static class MenuArrowIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 4;
        }

        public int getIconHeight() {
            return 8;
        }
    }

    private static class EmptyFrameIcon implements Icon, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 14;
        }

        public int getIconHeight() {
            return 16;
        }
    }
}
