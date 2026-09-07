import java.awt.Color;
import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.metal.MetalButtonUI;
import javax.swing.plaf.metal.MetalCheckBoxIcon;
import javax.swing.plaf.metal.MetalCheckBoxUI;
import javax.swing.plaf.metal.MetalComboBoxIcon;
import javax.swing.plaf.metal.MetalProgressBarUI;
import javax.swing.plaf.metal.MetalRadioButtonUI;
import javax.swing.plaf.metal.MetalScrollButton;
import javax.swing.plaf.metal.MetalToggleButtonUI;

/**
 * Los botones, los dos iconos y la barra de progreso de Metal, contra el JDK.
 *
 * <p>Los tamanos que dependen de la fuente no se comparan; los fijos si, y son casi todos: los dos
 * iconos y las ocho combinaciones del boton de barra son numeros puros.
 */
public class Metal3 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String d(Dimension x) {
        return (x == null) ? "-" : x.width + "x" + x.height;
    }

    static String c(Color x) {
        return (x == null) ? "-" : x.getRed() + "," + x.getGreen() + "," + x.getBlue();
    }

    static class Boton extends MetalButtonUI {
        String colores() {
            return "foco=" + c(getFocusColor()) + " seleccion=" + c(getSelectColor())
                    + " texto apagado=" + c(getDisabledTextColor());
        }

        String campos() {
            return "foco=" + c(focusColor) + " seleccion=" + c(selectColor)
                    + " texto apagado=" + c(disabledTextColor);
        }

        void sacar(AbstractButton b) {
            uninstallDefaults(b);
        }
    }

    static class Conmuta extends MetalToggleButtonUI {
        String colores() {
            return "foco=" + c(getFocusColor()) + " seleccion=" + c(getSelectColor())
                    + " texto apagado=" + c(getDisabledTextColor());
        }
    }

    static class Opcion extends MetalRadioButtonUI {
        String colores() {
            return "foco=" + c(getFocusColor()) + " seleccion=" + c(getSelectColor())
                    + " texto apagado=" + c(getDisabledTextColor());
        }
    }

    static class Casilla extends MetalCheckBoxUI {
        String prefijo() {
            return getPropertyPrefix();
        }

        String colores() {
            return "foco=" + c(getFocusColor()) + " seleccion=" + c(getSelectColor());
        }
    }

    static class Cuadro extends MetalCheckBoxIcon {
        int lado() {
            return getControlSize();
        }
    }

    static void botones() {
        linea("--- los cuatro botones ---");
        JButton b = new JButton("hola");
        Boton u = new Boton();
        b.setUI(u);
        linea("boton: " + u.colores());
        linea("boton, los campos: " + u.campos());
        linea("boton: fondo=" + c(b.getBackground()) + " frente=" + c(b.getForeground())
                + " margen=" + b.getMargin());
        u.sacar(b);
        linea("tras sacar los valores, los campos: " + u.campos());
        linea("y los metodos los vuelven a armar: " + u.colores());

        JToggleButton t = new JToggleButton("x");
        Conmuta ut = new Conmuta();
        t.setUI(ut);
        linea("conmutador: " + ut.colores());

        JRadioButton r = new JRadioButton("x");
        Opcion ur = new Opcion();
        r.setUI(ur);
        linea("opcion: " + ur.colores());

        JCheckBox k = new JCheckBox("x");
        Casilla uk = new Casilla();
        k.setUI(uk);
        linea("casilla: prefijo=" + uk.prefijo() + " " + uk.colores());

        linea("comparten instancia: boton=" + (MetalButtonUI.createUI(b)
                == MetalButtonUI.createUI(b))
                + " conmutador=" + (MetalToggleButtonUI.createUI(t)
                        == MetalToggleButtonUI.createUI(t))
                + " opcion=" + (MetalRadioButtonUI.createUI(r)
                        == MetalRadioButtonUI.createUI(r))
                + " casilla=" + (MetalCheckBoxUI.createUI(k)
                        == MetalCheckBoxUI.createUI(k)));
        linea("y la casilla no comparte con la opcion="
                + (MetalCheckBoxUI.createUI(k) == MetalRadioButtonUI.createUI(r)));
    }

    static void iconos() {
        linea("--- los dos iconos ---");
        Cuadro q = new Cuadro();
        linea("casilla: " + q.getIconWidth() + "x" + q.getIconHeight()
                + " lado del control=" + q.lado());
        MetalComboBoxIcon f = new MetalComboBoxIcon();
        linea("flecha: " + f.getIconWidth() + "x" + f.getIconHeight());
        linea("la casilla es un recurso del aspecto="
                + (q instanceof javax.swing.plaf.UIResource)
                + " y la flecha no=" + !(f instanceof javax.swing.plaf.UIResource));
    }

    static void flechas() {
        linea("--- el boton de una barra ---");
        int[] dirs = {SwingConstants.NORTH, SwingConstants.SOUTH,
                SwingConstants.EAST, SwingConstants.WEST};
        String[] nom = {"norte", "sur", "este", "oeste"};
        for (int i = 0; i < 4; i++) {
            for (int libre = 0; libre < 2; libre++) {
                MetalScrollButton s = new MetalScrollButton(dirs[i], 16, libre == 1);
                linea(" " + nom[i] + " suelto=" + (libre == 1)
                        + " preferido=" + d(s.getPreferredSize())
                        + " minimo=" + d(s.getMinimumSize())
                        + " maximo=" + d(s.getMaximumSize())
                        + " ancho=" + s.getButtonWidth());
            }
        }
        MetalScrollButton raro = new MetalScrollButton(99, 16, false);
        linea("con una direccion que no existe: " + d(raro.getPreferredSize()));

        // Soltarlo despues tiene que dar lo mismo que haberlo creado suelto.
        MetalScrollButton e = new MetalScrollButton(SwingConstants.EAST, 16, false);
        linea("este pegado=" + d(e.getPreferredSize()));
        e.setFreeStanding(true);
        linea("y tras soltarlo=" + d(e.getPreferredSize()));

        MetalScrollButton chico = new MetalScrollButton(SwingConstants.SOUTH, 5, false);
        linea("uno de ancho 5: " + d(chico.getPreferredSize()));
        linea("el maximo es uno nuevo cada vez="
                + (e.getMaximumSize() != e.getMaximumSize()));
        linea("no toma el foco=" + e.isFocusable() + " direccion=" + e.getDirection());
    }

    static void progreso() {
        linea("--- la barra de progreso ---");
        JProgressBar p = new JProgressBar();
        MetalProgressBarUI u = new MetalProgressBarUI();
        p.setUI(u);
        linea("fondo=" + c(p.getBackground()) + " frente=" + c(p.getForeground()));
        linea("cada llamada da un UI nuevo=" + (MetalProgressBarUI.createUI(p)
                != MetalProgressBarUI.createUI(p)));
        linea("el preferido tiene alto positivo=" + (u.getPreferredSize(p).height > 0));
    }

    public static int run() {
        botones();
        iconos();
        flechas();
        progreso();
        return 0;
    }
}
