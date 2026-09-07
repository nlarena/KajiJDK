import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import javax.swing.DefaultDesktopManager;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * El escritorio y sus ventanas internas, contra el JDK.
 *
 * <p>Lo que se compara no es solo el estado sino el <em>orden</em> de lo que pasa: el veto se
 * consulta antes de cambiar, el cambio se anuncia despues, y el evento de ventana llega al final.
 * Un orden distinto compila igual y rompe a quien escucha.
 *
 * <p>Sin pantalla nada esta visible, asi que activar una ventana no hace nada -- y eso tambien se
 * compara, porque es una regla del JDK y no un accidente de esta corrida.
 */
public class Escrit1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota cada cambio y cada veto, en el orden en que llegan. */
    static class Espia implements PropertyChangeListener, VetoableChangeListener {

        private final StringBuilder log = new StringBuilder();
        private String vetar = null;

        void veta(String propiedad) {
            vetar = propiedad;
        }

        public void propertyChange(PropertyChangeEvent e) {
            // "ancestor" y "wasIconOnce" los dispara el aspecto instalado, que esta biblioteca no
            // tiene: son la brecha conocida de los delegados de aspecto, no de estas clases.
            String n = e.getPropertyName();
            if ("ancestor".equals(n) || "wasIconOnce".equals(n)) {
                return;
            }
            log.append(" cambio:").append(n).append("=").append(e.getNewValue());
        }

        public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException {
            log.append(" veto?:").append(e.getPropertyName()).append("=")
                    .append(e.getNewValue());
            if (vetar != null && vetar.equals(e.getPropertyName())) {
                log.append("(NO)");
                throw new PropertyVetoException("no", e);
            }
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Anota los eventos de ventana interna. */
    static class Oyente extends InternalFrameAdapter {

        private final StringBuilder log = new StringBuilder();

        public void internalFrameOpened(InternalFrameEvent e) {
            log.append(" abierta");
        }

        public void internalFrameClosing(InternalFrameEvent e) {
            log.append(" cerrando");
        }

        public void internalFrameClosed(InternalFrameEvent e) {
            log.append(" cerrada");
        }

        public void internalFrameIconified(InternalFrameEvent e) {
            log.append(" icono");
        }

        public void internalFrameDeiconified(InternalFrameEvent e) {
            log.append(" desicono");
        }

        public void internalFrameActivated(InternalFrameEvent e) {
            log.append(" activada");
        }

        public void internalFrameDeactivated(InternalFrameEvent e) {
            log.append(" desactivada");
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Expone lo protegido del administrador; ver la nota de {@link DefaultDesktopManager}. */
    static class Admin extends DefaultDesktopManager {

        Rectangle lugarDeIcono(JInternalFrame f) {
            return getBoundsForIconOf(f);
        }

        void guardaPrevio(JInternalFrame f, Rectangle r) {
            setPreviousBounds(f, r);
        }

        Rectangle previo(JInternalFrame f) {
            return getPreviousBounds(f);
        }

        void marcaIcono(JInternalFrame f, Boolean v) {
            setWasIcon(f, v);
        }

        boolean fueIcono(JInternalFrame f) {
            return wasIcon(f);
        }

        void sacaIcono(JInternalFrame f) {
            removeIconFor(f);
        }
    }

    static String rect(Rectangle r) {
        if (r == null) {
            return "-";
        }
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    static String estado(JInternalFrame f) {
        return "cerr=" + f.isClosed() + " icon=" + f.isIcon() + " max=" + f.isMaximum()
                + " sel=" + f.isSelected() + " redim=" + f.isResizable();
    }

    static String titulo(JInternalFrame f) {
        return (f == null) ? "-" : f.getTitle();
    }

    static String nombres(JInternalFrame[] fs) {
        StringBuilder b = new StringBuilder();
        b.append(fs.length).append(":");
        for (int i = 0; i < fs.length; i++) {
            b.append(" ").append(fs[i].getTitle());
        }
        return b.toString();
    }

    public static int run() {
        JDesktopPane d = new JDesktopPane();
        d.setBounds(0, 0, 400, 300);
        linea("escritorio arrastre=" + d.getDragMode() + " vivo=" + JDesktopPane.LIVE_DRAG_MODE
                + " contorno=" + JDesktopPane.OUTLINE_DRAG_MODE);
        linea("activa inicial=" + d.getSelectedFrame() + " ventanas=" + nombres(d.getAllFrames()));

        // El JDK no valida el modo aunque su documentacion lo prometa; ver JDesktopPane.
        d.setDragMode(7);
        linea("modo 7 quedo=" + d.getDragMode());
        d.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        linea("modo ahora=" + d.getDragMode());

        Admin dm = new Admin();
        d.setDesktopManager(dm);
        linea("administrador=" + (d.getDesktopManager() == dm));

        JInternalFrame a = new JInternalFrame("A", true, true, true, true);
        JInternalFrame b = new JInternalFrame("B");
        linea("A " + estado(a) + " cerrable=" + a.isClosable() + " maxble=" + a.isMaximizable()
                + " iconble=" + a.isIconifiable());
        linea("B " + estado(b) + " cerrable=" + b.isClosable() + " maxble=" + b.isMaximizable()
                + " iconble=" + b.isIconifiable());
        linea("A titulo=" + a.getTitle() + " cierre=" + a.getDefaultCloseOperation());
        linea("A visible=" + a.isVisible() + " raiz ciclo=" + a.isFocusCycleRoot()
                + " ancestro=" + a.getFocusCycleRootAncestor()
                + " advertencia=" + a.getWarningString());
        linea("A escritorio=" + a.getDesktopPane() + " capa=" + a.getLayer());
        linea("A iconito visible=" + a.getDesktopIcon().isVisible()
                + " ventana del iconito=" + (a.getDesktopIcon().getInternalFrame() == a));

        Espia espia = new Espia();
        Oyente oyente = new Oyente();
        a.addPropertyChangeListener(espia);
        a.addVetoableChangeListener(espia);
        a.addInternalFrameListener(oyente);

        a.setSize(120, 90);
        b.setSize(140, 100);
        d.add(a, JLayeredPane.DEFAULT_LAYER);
        d.add(b, JLayeredPane.PALETTE_LAYER);
        espia.vaciar();
        oyente.vaciar();
        linea("agregadas " + nombres(d.getAllFrames()));
        linea("capas A=" + a.getLayer() + " B=" + b.getLayer());
        linea("en capa 0 " + nombres(d.getAllFramesInLayer(0)));
        linea("en capa 100 " + nombres(d.getAllFramesInLayer(100)));
        linea("A escritorio ahora=" + (a.getDesktopPane() == d));

        // Activar sin pantalla no hace nada: ver la nota de la clase.
        try {
            a.setSelected(true);
        } catch (PropertyVetoException e) {
            linea("activar vetado");
        }
        linea("tras activar " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());

        // Maximizar: veto primero, cambio despues.
        try {
            a.setMaximum(true);
        } catch (PropertyVetoException e) {
            linea("max vetado");
        }
        linea("max " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());
        linea("normal=" + rect(a.getNormalBounds()));

        espia.veta("maximum");
        try {
            a.setMaximum(false);
            linea("desmax paso");
        } catch (PropertyVetoException e) {
            linea("desmax vetado");
        }
        linea("tras veto " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());
        espia.veta(null);
        try {
            a.setMaximum(false);
        } catch (PropertyVetoException e) {
            linea("desmax 2 vetado");
        }
        linea("desmax " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());

        // Minimizar a icono.
        try {
            a.setIcon(true);
        } catch (PropertyVetoException e) {
            linea("icono vetado");
        }
        linea("icono " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());
        try {
            a.setIcon(true);
        } catch (PropertyVetoException e) {
            linea("icono repetido vetado");
        }
        linea("icono repetido " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());
        try {
            a.setIcon(false);
        } catch (PropertyVetoException e) {
            linea("desicono vetado");
        }
        linea("desicono " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());

        // El administrador guarda el estado en la ventana, no en si mismo.
        JInternalFrame c = new JInternalFrame("C");
        linea("wasIcon antes=" + dm.fueIcono(c));
        dm.marcaIcono(c, Boolean.TRUE);
        linea("wasIcon despues=" + dm.fueIcono(c));
        // Nulo no borra la marca: el metodo solo escribe cuando le dan un valor.
        dm.marcaIcono(c, null);
        linea("wasIcon con nulo=" + dm.fueIcono(c));
        dm.guardaPrevio(a, new Rectangle(3, 4, 5, 6));
        linea("previo=" + rect(dm.previo(a)) + " normal=" + rect(a.getNormalBounds()));
        dm.guardaPrevio(a, null);
        linea("previo nulo=" + rect(dm.previo(a)) + " limites=" + rect(a.getBounds()));

        // Ubicar el icono: fila al pie del escritorio.
        a.getDesktopIcon().setPreferredSize(new java.awt.Dimension(50, 20));
        b.getDesktopIcon().setPreferredSize(new java.awt.Dimension(50, 20));
        linea("lugar icono A=" + rect(dm.lugarDeIcono(a)));
        d.add(b.getDesktopIcon());
        b.getDesktopIcon().setVisible(true);
        b.getDesktopIcon().setBounds(0, 280, 50, 20);
        linea("lugar icono A con B puesto=" + rect(dm.lugarDeIcono(a)));

        // Mover con el administrador. El arrastre en si no entra en la comparacion: los dos
        // modos del JDK pintan directo -- con contorno sobre el escritorio, en vivo sobre la
        // ventana del sistema -- y sin pantalla los dos revientan antes de mover nada.
        dm.setBoundsForFrame(a, 10, 20, 60, 70);
        linea("movida=" + rect(a.getBounds()));
        dm.setBoundsForFrame(a, 10, 20, 61, 71);
        linea("redimensionada=" + rect(a.getBounds()));

        // Cerrar: cerrando, veto, cambio, cerrada.
        try {
            a.setClosed(true);
        } catch (PropertyVetoException e) {
            linea("cerrar vetado");
        }
        linea("cerrada " + estado(a) + " |" + espia.vaciar() + " |" + oyente.vaciar());

        // Sacar del escritorio NO olvida la activa; ver la nota de JDesktopPane.
        d.setSelectedFrame(b);
        linea("activa=" + d.getSelectedFrame().getTitle());
        d.remove(b);
        linea("tras sacar activa=" + titulo(d.getSelectedFrame()));
        d.removeAll();
        linea("vaciado " + nombres(d.getAllFrames()) + " activa=" + titulo(d.getSelectedFrame()));
        linea("elegir sin ventanas=" + d.selectFrame(true));
        return 0;
    }
}
