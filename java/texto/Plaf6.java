import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicDirectoryModel;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicSpinnerUI;

/**
 * La barra de progreso, el spinner y el modelo de directorio, contra el JDK.
 *
 * <p>El modelo de directorio lee una carpeta del propio repositorio, por camino absoluto, para que
 * las dos corridas vean lo mismo. La lectura es en otro hilo -- ver la nota de la clase --, asi que
 * la prueba espera a que el numero se quede quieto antes de mirarlo.
 */
public class Plaf6 {

    /** Una carpeta del repositorio que no cambia; el camino es absoluto a proposito. */
    static final String CARPETA =
            "C:\\Users\\nicol\\Sources\\Larena\\KajiJVM\\KajiJDK\\repo\\KajiLibrary\\javax\\swing\\undo";

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static class Barra extends BasicProgressBarUI {
        String estado() {
            return "celda=" + getCellLength() + " espacio=" + getCellSpacing()
                    + " cuadros=" + getFrameCount() + " indice=" + getAnimationIndex()
                    + " caja=" + boxRect + " cambio=" + (changeListener != null);
        }

        String colores() {
            return "seleccion=" + col(getSelectionBackground())
                    + " sobre " + col(getSelectionForeground());
        }

        String internos() {
            return getPreferredInnerHorizontal() + " / " + getPreferredInnerVertical();
        }

        int lleno(Insets i, int w, int h) {
            return getAmountFull(i, w, h);
        }

        int largoCaja(int a, int b) {
            return getBoxLength(a, b);
        }

        Rectangle caja() {
            return getBox(new Rectangle());
        }
    }

    static class Rueda extends BasicSpinnerUI {
        String creados() {
            return "layout=" + corto(createLayout()) + " editor=" + corto(createEditor())
                    + " anterior=" + corto(createPreviousButton())
                    + " siguiente=" + corto(createNextButton());
        }
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    static void barras() {
        linea("--- BasicProgressBarUI ---");
        JProgressBar b = new JProgressBar();
        Barra u = new Barra();
        linea("comparte instancia="
                + (BasicProgressBarUI.createUI(b) == BasicProgressBarUI.createUI(b)));
        u.installUI(b);
        linea(u.estado());
        linea(u.colores());
        linea("internos=" + u.internos());
        linea("fondo=" + col(b.getBackground()) + " frente=" + col(b.getForeground())
                + " fuente=" + fue(b.getFont()) + " opaca=" + b.isOpaque()
                + " insets=" + b.getInsets());
        linea("horizontal preferido=" + u.getPreferredSize(b)
                + " minimo=" + u.getMinimumSize(b) + " maximo=" + u.getMaximumSize(b));
        b.setOrientation(SwingConstants.VERTICAL);
        linea("vertical preferido=" + u.getPreferredSize(b)
                + " minimo=" + u.getMinimumSize(b) + " maximo=" + u.getMaximumSize(b));
        b.setOrientation(SwingConstants.HORIZONTAL);

        // El minimo no mira el borde; ver la nota de la clase.
        b.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 7, 5, 7));
        linea("con borde 5/7 preferido=" + u.getPreferredSize(b)
                + " minimo=" + u.getMinimumSize(b));
        b.setBorder(new javax.swing.plaf.BorderUIResource.LineBorderUIResource(
                Color.black, 1));

        b.setValue(50);
        linea("lleno con 50 de 100 en 98 de ancho="
                + u.lleno(new Insets(1, 1, 1, 1), 98, 18));
        linea("largo de la caja: de 100 -> " + u.largoCaja(100, 6)
                + " de 20 -> " + u.largoCaja(20, 6));
        linea("sin texto: linea de base=" + u.getBaseline(b, 100, 20)
                + " al cambiar de tamano=" + u.getBaselineResizeBehavior(b));
        try {
            u.caja();
            linea("caja antes de ser indeterminada: aceptada");
        } catch (NullPointerException e) {
            linea("caja antes de ser indeterminada: revienta");
        }
        b.setIndeterminate(true);
        linea("indeterminada: cuadros=" + u.estado());
        linea("caja tras serlo=" + u.caja());
        b.setIndeterminate(false);
    }

    static void ruedas() {
        linea("--- BasicSpinnerUI ---");
        JSpinner s = new JSpinner();
        Rueda u = new Rueda();
        linea("comparte instancia="
                + (BasicSpinnerUI.createUI(s) == BasicSpinnerUI.createUI(s)));
        u.installUI(s);
        linea("creados: " + u.creados());
        linea("acomodador puesto=" + corto(s.getLayout()) + " opaco=" + s.isOpaque());
        linea("preferido=" + u.getPreferredSize(s)
                + " al cambiar de tamano=" + u.getBaselineResizeBehavior(s));
        // Las flechitas mueven el valor.
        Object antes = s.getValue();
        s.setValue(s.getNextValue());
        Object despues = s.getValue();
        linea("el siguiente cambia el valor=" + !antes.equals(despues)
                + " de " + antes + " a " + despues);
    }

    static void carpetas() {
        linea("--- BasicDirectoryModel ---");
        File dir = new File(CARPETA);
        linea("la carpeta existe=" + dir.isDirectory());
        JFileChooser fc = new JFileChooser(dir);
        BasicDirectoryModel m = new BasicDirectoryModel(fc);
        int n = esperar(m);
        // getDirectories() trae ".." adelante, que no esta en el modelo; ver la nota de la clase.
        linea("hay elementos=" + (n > 0)
                + " carpetas mas archivos es uno mas que el tamano="
                + (m.getDirectories().size() + m.getFiles().size() == n + 1));
        linea("la primera carpeta es el atajo de arriba="
                + "..".equals(m.getDirectories().get(0).getName()));
        linea("sin escuchas=" + (m.getPropertyChangeListeners().length == 0));
        if (n > 0) {
            Object primero = m.getElementAt(0);
            linea("el primero es un File=" + (primero instanceof File)
                    + " lo contiene=" + m.contains(primero)
                    + " esta en " + m.indexOf(primero));
        }
        linea("contiene nulo=" + m.contains(null) + " indice de nulo=" + m.indexOf(null));
        try {
            m.getElementAt(99999);
            linea("indice grande aceptado");
        } catch (ArrayIndexOutOfBoundsException e) {
            linea("indice grande revienta");
        }
        try {
            m.getElementAt(-1);
            linea("indice negativo aceptado");
        } catch (ArrayIndexOutOfBoundsException e) {
            linea("indice negativo revienta");
        }
        // El orden: las carpetas primero, y dentro de cada grupo por nombre.
        linea("ordenado por nombre=" + ordenado(m.getFiles()));
        linea("las carpetas van primero=" + carpetasPrimero(m));
    }

    /** Espera a que la lectura en otro hilo termine; ver la nota de la clase. */
    static int esperar(BasicDirectoryModel m) {
        int anterior = -1;
        for (int i = 0; i < 100; i++) {
            int n = m.getSize();
            if (n > 0 && n == anterior) {
                return n;
            }
            anterior = n;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return m.getSize();
    }

    static boolean ordenado(Vector<File> v) {
        for (int i = 1; i < v.size(); i++) {
            if (v.get(i - 1).getName().compareToIgnoreCase(v.get(i).getName()) > 0) {
                return false;
            }
        }
        return true;
    }

    static boolean carpetasPrimero(BasicDirectoryModel m) {
        // Sin contar el ".." que getDirectories() agrega adelante.
        int cuantas = m.getDirectories().size() - 1;
        for (int i = 0; i < m.getSize(); i++) {
            Object o = m.getElementAt(i);
            boolean esCarpeta = ((File) o).isDirectory();
            if (i < cuantas != esCarpeta) {
                return false;
            }
        }
        return true;
    }

    public static int run() {
        barras();
        ruedas();
        carpetas();
        return 0;
    }
}
