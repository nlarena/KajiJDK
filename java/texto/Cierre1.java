import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.DefaultFocusManager;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

/**
 * Lo que faltaba de {@code javax.swing}, contra el JDK.
 *
 * <p>Son clases muy distintas entre si -- iconos, foco, acomodadores, administradores -- y lo que
 * tienen en comun es que todas se pueden probar sin pantalla, porque lo que se compara es su
 * <em>estado</em> y sus cuentas, no lo que dibujan.
 *
 * <p>Queda afuera, y esta dicho en cada clase: mostrar un cartel de ayuda, mostrar el cartel de
 * progreso, y el destello de {@code DebugGraphics}. Las tres necesitan una pantalla y un mouse.
 */
public class Cierre1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String clase(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    static String n(Component c) {
        return (c == null) ? "-" : String.valueOf(c.getName());
    }

    static void iconos() {
        linea("--- ImageIcon ---");
        ImageIcon vacio = new ImageIcon();
        linea("vacio ancho=" + vacio.getIconWidth() + " alto=" + vacio.getIconHeight()
                + " descripcion=" + vacio.getDescription()
                + " imagen=" + vacio.getImage() + " observador=" + vacio.getImageObserver());
        // Un archivo que no existe: la carga falla y las medidas quedan en -1.
        ImageIcon falta = new ImageIcon("no_existe_este_archivo.png");
        linea("archivo ausente ancho=" + falta.getIconWidth()
                + " alto=" + falta.getIconHeight()
                + " descripcion=" + falta.getDescription());
        linea("estado de carga es ERRORED="
                + ((falta.getImageLoadStatus() & java.awt.MediaTracker.ERRORED) != 0));
        falta.setDescription("otra cosa");
        linea("descripcion cambiada=" + falta.getDescription()
                + " toString=" + falta.toString());
        ImageIcon conDesc = new ImageIcon("x.png", "mi icono");
        linea("con descripcion=" + conDesc.getDescription());
    }

    /** Expone {@code getComparator}, que es protegido. */
    static class Politica extends LayoutFocusTraversalPolicy {

        boolean hayComparador() {
            return getComparator() != null;
        }
    }

    /** Un panel con nombre y posicion fijas, para el recorrido de foco. */
    static JPanel caja(String nombre, int x, int y, int w, int h) {
        JPanel p = new JPanel();
        p.setName(nombre);
        p.setBounds(x, y, w, h);
        p.setFocusable(true);
        return p;
    }

    static void foco() {
        linea("--- el recorrido de foco ---");
        JPanel raiz = new JPanel();
        raiz.setLayout(null);
        raiz.setBounds(0, 0, 200, 100);
        raiz.setFocusCycleRoot(true);
        // Se agregan en orden inverso al que se leen, para que ordenar tenga algo que hacer.
        JPanel abajoDer = caja("abajoDer", 100, 50, 50, 20);
        JPanel abajoIzq = caja("abajoIzq", 0, 50, 50, 20);
        JPanel arribaDer = caja("arribaDer", 100, 0, 50, 20);
        JPanel arribaIzq = caja("arribaIzq", 0, 0, 50, 20);
        raiz.add(abajoDer);
        raiz.add(abajoIzq);
        raiz.add(arribaDer);
        raiz.add(arribaIzq);

        Politica p = new Politica();
        linea("baja a los ciclos de adentro=" + p.getImplicitDownCycleTraversal()
                + " tiene comparador=" + p.hayComparador());

        // Sin pantalla ningun componente es "displayable", y esa es una de las tres condiciones
        // que pide accept(). Asi que el recorrido queda vacio en las dos bibliotecas, y lo que se
        // compara es el contrato: que devuelve nulo en vez de reventar, y donde si revienta.
        linea("primero=" + n(p.getFirstComponent(raiz))
                + " ultimo=" + n(p.getLastComponent(raiz))
                + " por omision=" + n(p.getDefaultComponent(raiz)));
        linea("siguiente=" + n(p.getComponentAfter(raiz, abajoDer))
                + " anterior=" + n(p.getComponentBefore(raiz, arribaIzq)));

        // Un contenedor que no es raiz de ciclo: solo lo rechazan los dos que preguntan "que
        // sigue". Ver la nota de SortingFocusTraversalPolicy.
        JPanel noRaiz = new JPanel();
        noRaiz.setFocusCycleRoot(false);
        JPanel hijo = caja("hijo", 0, 0, 10, 10);
        noRaiz.add(hijo);
        linea("primero de uno que no es raiz=" + n(p.getFirstComponent(noRaiz)));
        try {
            p.getComponentAfter(noRaiz, hijo);
            linea("siguiente de uno que no es raiz aceptado");
        } catch (IllegalArgumentException e) {
            linea("siguiente de uno que no es raiz rechazado: " + e.getMessage());
        }

        try {
            p.getFirstComponent(null);
            linea("contenedor nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("contenedor nulo rechazado");
        }
        try {
            p.getComponentAfter(raiz, null);
            linea("componente nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("componente nulo rechazado");
        }

        p.setImplicitDownCycleTraversal(false);
        linea("sin bajar a los ciclos=" + p.getImplicitDownCycleTraversal());
        p.setImplicitDownCycleTraversal(true);

        // El orden por posicion si se puede comprobar: no depende de la pantalla.
        DefaultFocusManager fm = new DefaultFocusManager();
        linea("arribaIzq antes que arribaDer=" + fm.compareTabOrder(arribaIzq, arribaDer)
                + " arribaDer antes que arribaIzq=" + fm.compareTabOrder(arribaDer, arribaIzq));
        linea("arribaDer antes que abajoIzq=" + fm.compareTabOrder(arribaDer, abajoIzq)
                + " abajoIzq antes que arribaDer=" + fm.compareTabOrder(abajoIzq, arribaDer));
        // Dos que se superponen verticalmente son "la misma fila" aunque no empiecen igual.
        // Tienen que tener padre: el JDK lo desreferencia para desempatar.
        JPanel otroPadre = new JPanel();
        otroPadre.setLayout(null);
        JPanel alto = caja("alto", 0, 0, 20, 40);
        JPanel bajo = caja("bajo", 50, 10, 20, 10);
        otroPadre.add(alto);
        otroPadre.add(bajo);
        linea("alto antes que bajo=" + fm.compareTabOrder(alto, bajo)
                + " bajo antes que alto=" + fm.compareTabOrder(bajo, alto));
    }

    static void estiloYUtiles() {
        linea("--- LayoutStyle y SwingUtilities ---");
        LayoutStyle ls = LayoutStyle.getInstance();
        linea("hay una instancia=" + (ls != null));
        JButton a = new JButton("a");
        JButton b = new JButton("b");
        linea("relacionados=" + ls.getPreferredGap(a, b,
                LayoutStyle.ComponentPlacement.RELATED, SwingConstants.EAST, null)
                + " sin relacion=" + ls.getPreferredGap(a, b,
                        LayoutStyle.ComponentPlacement.UNRELATED, SwingConstants.EAST, null)
                + " contra el borde=" + ls.getContainerGap(a, SwingConstants.NORTH, null));
        try {
            ls.getContainerGap(a, 999, null);
            linea("posicion 999 aceptada");
        } catch (IllegalArgumentException e) {
            linea("posicion 999 rechazada");
        }
        try {
            ls.getPreferredGap(null, b, LayoutStyle.ComponentPlacement.RELATED,
                    SwingConstants.EAST, null);
            linea("componente nulo aceptado");
        } catch (NullPointerException e) {
            linea("componente nulo rechazado por NullPointerException");
        }
        linea("constantes=" + java.util.Arrays.toString(
                LayoutStyle.ComponentPlacement.values()));

        // Coordenadas: se modifica el punto que se pasa.
        JPanel padre = new JPanel();
        padre.setBounds(10, 20, 100, 100);
        JPanel hijo = new JPanel();
        hijo.setBounds(5, 7, 50, 50);
        padre.add(hijo);
        Point pt = new Point(1, 2);
        SwingUtilities.convertPointToScreen(pt, hijo);
        linea("a pantalla=" + pt);
        SwingUtilities.convertPointFromScreen(pt, hijo);
        linea("y de vuelta=" + pt);

        linea("padre sin envoltura=" + clase(SwingUtilities.getUnwrappedParent(hijo)));
        linea("panel raiz de un suelto=" + SwingUtilities.getRootPane(hijo));
        javax.swing.JFrame ventana = null;
        try {
            ventana = new javax.swing.JFrame();
        } catch (java.awt.HeadlessException e) {
            ventana = null;
        }
        linea("hay ventana=" + (ventana != null));

        // Los cinco metodos de accesibilidad de SwingUtilities preguntan por el
        // AccessibleContext del componente y no comprueban nada: con nulo revientan. Eso si se
        // compara. Lo que contestan con un componente que si tiene contexto no, porque ninguna
        // clase de javax.swing de esta biblioteca lo arma todavia; queda anotado como hueco --
        // es una tanda entera, no una linea.
        try {
            SwingUtilities.getAccessibleChildrenCount(null);
            linea("componente nulo aceptado");
        } catch (NullPointerException e) {
            linea("componente nulo revienta");
        }
    }

    static void selecciones() {
        linea("--- los dos metodos nuevos de ListSelectionModel ---");
        ListSelectionModel m = new DefaultListSelectionModel();
        linea("vacio=" + java.util.Arrays.toString(m.getSelectedIndices())
                + " cuantos=" + m.getSelectedItemsCount());
        m.setSelectionInterval(1, 3);
        m.addSelectionInterval(7, 7);
        linea("elegidos=" + java.util.Arrays.toString(m.getSelectedIndices())
                + " cuantos=" + m.getSelectedItemsCount());
        m.removeSelectionInterval(2, 2);
        linea("con un hueco=" + java.util.Arrays.toString(m.getSelectedIndices())
                + " cuantos=" + m.getSelectedItemsCount());
    }

    static void administradores() {
        linea("--- los administradores ---");
        ToolTipManager t = ToolTipManager.sharedInstance();
        linea("es unico=" + (t == ToolTipManager.sharedInstance())
                + " prendido=" + t.isEnabled()
                + " liviano=" + t.isLightWeightPopupEnabled());
        linea("demoras inicial=" + t.getInitialDelay() + " descarte=" + t.getDismissDelay()
                + " reaparicion=" + t.getReshowDelay());
        t.setInitialDelay(1000);
        t.setDismissDelay(3000);
        t.setReshowDelay(200);
        linea("cambiadas=" + t.getInitialDelay() + "/" + t.getDismissDelay()
                + "/" + t.getReshowDelay());
        t.setEnabled(false);
        linea("apagado=" + t.isEnabled());
        t.setEnabled(true);
        t.setInitialDelay(750);
        t.setDismissDelay(4000);
        t.setReshowDelay(500);

        RepaintManager r = new RepaintManager();
        // El tope de omision es lo que ocupan las pantallas, y eso depende del monitor de quien
        // corra la prueba. Lo que se compara es que haya uno y que se pueda cambiar.
        linea("doble buffer=" + r.isDoubleBufferingEnabled()
                + " hay tope=" + (r.getDoubleBufferMaximumSize() != null));
        JPanel p = new JPanel();
        p.setBounds(0, 0, 100, 100);
        linea("sin nada sucio=" + r.getDirtyRegion(p) + " completo=" + r.isCompletelyDirty(p));
        r.addDirtyRegion(p, 10, 10, 20, 20);
        linea("un rectangulo=" + r.getDirtyRegion(p));
        // Dos rectangulos se unen en el que los contiene.
        r.addDirtyRegion(p, 50, 50, 10, 10);
        linea("dos unidos=" + r.getDirtyRegion(p));
        r.addDirtyRegion(p, 0, 0, 0, 0);
        linea("uno vacio no cuenta=" + r.getDirtyRegion(p));
        r.markCompletelyClean(p);
        linea("limpiado=" + r.getDirtyRegion(p));
        r.markCompletelyDirty(p);
        linea("todo sucio=" + r.isCompletelyDirty(p));
        r.markCompletelyClean(p);
        r.setDoubleBufferMaximumSize(new Dimension(200, 200));
        linea("tope nuevo=" + r.getDoubleBufferMaximumSize());
        r.setDoubleBufferingEnabled(false);
        linea("doble buffer apagado=" + r.isDoubleBufferingEnabled());

        // El monitor de progreso: las dos demoras y los limites.
        ProgressMonitor pm = new ProgressMonitor(null, "haciendo cosas", "empezando", 0, 100);
        linea("monitor min=" + pm.getMinimum() + " max=" + pm.getMaximum()
                + " nota=" + pm.getNote() + " cancelado=" + pm.isCanceled());
        linea("demoras decidir=" + pm.getMillisToDecideToPopup()
                + " mostrar=" + pm.getMillisToPopup());
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(50);
        pm.setNote("a mitad");
        pm.setMinimum(10);
        pm.setMaximum(200);
        linea("cambiado min=" + pm.getMinimum() + " max=" + pm.getMaximum()
                + " nota=" + pm.getNote() + " demoras=" + pm.getMillisToDecideToPopup()
                + "/" + pm.getMillisToPopup());
        pm.setProgress(50);
        linea("con avance cancelado=" + pm.isCanceled());
        pm.close();
        linea("cerrado, cancelado=" + pm.isCanceled());
    }

    static void grupos() {
        linea("--- GroupLayout ---");
        linea("constantes DEFAULT_SIZE=" + GroupLayout.DEFAULT_SIZE
                + " PREFERRED_SIZE=" + GroupLayout.PREFERRED_SIZE);
        linea("alineaciones=" + java.util.Arrays.toString(GroupLayout.Alignment.values()));

        JPanel panel = new JPanel();
        GroupLayout g = new GroupLayout(panel);
        panel.setLayout(g);
        linea("huecos solos=" + g.getAutoCreateGaps()
                + " contra el borde=" + g.getAutoCreateContainerGaps()
                + " respeta lo escondido=" + g.getHonorsVisibility());

        JLabel etiqueta = new JLabel("Nombre:");
        etiqueta.setPreferredSize(new Dimension(60, 20));
        etiqueta.setMinimumSize(new Dimension(60, 20));
        etiqueta.setMaximumSize(new Dimension(60, 20));
        JTextField campo = new JTextField();
        campo.setPreferredSize(new Dimension(100, 20));
        campo.setMinimumSize(new Dimension(40, 20));
        campo.setMaximumSize(new Dimension(1000, 20));
        panel.add(etiqueta);
        panel.add(campo);

        g.setHorizontalGroup(g.createSequentialGroup()
                .addComponent(etiqueta)
                .addComponent(campo));
        g.setVerticalGroup(g.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(etiqueta)
                .addComponent(campo));

        linea("preferido=" + g.preferredLayoutSize(panel));
        linea("minimo=" + g.minimumLayoutSize(panel));
        panel.setSize(300, 40);
        g.layoutContainer(panel);
        linea("colocados etiqueta=" + rect(etiqueta.getBounds())
                + " campo=" + rect(campo.getBounds()));

        // Con el minimo, el campo se achica y la etiqueta no.
        panel.setSize(100, 40);
        g.layoutContainer(panel);
        linea("apretado etiqueta=" + rect(etiqueta.getBounds())
                + " campo=" + rect(campo.getBounds()));

        // Un componente escondido deja de ocupar lugar.
        etiqueta.setVisible(false);
        linea("con la etiqueta escondida preferido=" + g.preferredLayoutSize(panel));
        g.setHonorsVisibility(false);
        linea("sin respetar lo escondido=" + g.preferredLayoutSize(panel));
        g.setHonorsVisibility(true);
        etiqueta.setVisible(true);

        try {
            new GroupLayout(null);
            linea("contenedor nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("contenedor nulo rechazado");
        }
        try {
            g.setHorizontalGroup(null);
            linea("grupo nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("grupo nulo rechazado");
        }
        try {
            g.createSequentialGroup().addComponent(null);
            linea("componente nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("componente nulo rechazado");
        }
        try {
            g.createSequentialGroup().addComponent(campo, 100, 50, 10);
            linea("tamanos incoherentes aceptados");
        } catch (IllegalArgumentException e) {
            linea("tamanos incoherentes rechazados");
        }
        try {
            g.createParallelGroup(null);
            linea("alineacion nula aceptada");
        } catch (IllegalArgumentException e) {
            linea("alineacion nula rechazada");
        }
        // layoutContainer no mira de quien es el contenedor; los que miden si.
        try {
            new GroupLayout(new JPanel()).layoutContainer(panel);
            linea("otro contenedor al colocar aceptado");
        } catch (IllegalArgumentException e) {
            linea("otro contenedor al colocar rechazado");
        }
        try {
            new GroupLayout(new JPanel()).preferredLayoutSize(panel);
            linea("otro contenedor al medir aceptado");
        } catch (IllegalArgumentException e) {
            linea("otro contenedor al medir rechazado: " + e.getMessage());
        }
        JPanel otro = new JPanel();
        GroupLayout gv = new GroupLayout(otro);
        linea("sin grupos preferido=" + gv.preferredLayoutSize(otro)
                + " minimo=" + gv.minimumLayoutSize(otro)
                + " maximo=" + gv.maximumLayoutSize(otro));

        // Un hueco fijo y uno elastico.
        JPanel p2 = new JPanel();
        GroupLayout g2 = new GroupLayout(p2);
        p2.setLayout(g2);
        JLabel uno = new JLabel("uno");
        uno.setPreferredSize(new Dimension(30, 10));
        uno.setMinimumSize(new Dimension(30, 10));
        uno.setMaximumSize(new Dimension(30, 10));
        JLabel dos = new JLabel("dos");
        dos.setPreferredSize(new Dimension(30, 10));
        dos.setMinimumSize(new Dimension(30, 10));
        dos.setMaximumSize(new Dimension(30, 10));
        p2.add(uno);
        p2.add(dos);
        g2.setHorizontalGroup(g2.createSequentialGroup()
                .addComponent(uno)
                .addGap(10)
                .addComponent(dos));
        g2.setVerticalGroup(g2.createParallelGroup()
                .addComponent(uno)
                .addComponent(dos));
        linea("con hueco fijo preferido=" + g2.preferredLayoutSize(p2));
        p2.setSize(70, 10);
        g2.layoutContainer(p2);
        linea("colocados uno=" + rect(uno.getBounds()) + " dos=" + rect(dos.getBounds()));
    }

    static String rect(Rectangle r) {
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    public static int run() {
        iconos();
        foco();
        estiloYUtiles();
        selecciones();
        administradores();
        grupos();
        return 0;
    }
}
