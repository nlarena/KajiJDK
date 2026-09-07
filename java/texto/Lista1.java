import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Los modelos de lista y los mapas de atajos, contra el JDK.
 *
 * <p>Se comparan los avisos, no solo el estado: casi todos los errores de un modelo son avisos que
 * faltan o que sobran, y eso no se ve mirando el resultado final.
 */
public class Lista1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota cada aviso del modelo de datos. */
    static class EspiaDatos implements ListDataListener {

        public void contentsChanged(ListDataEvent e) {
            linea("  cambio [" + e.getIndex0() + "," + e.getIndex1() + "]");
        }

        public void intervalAdded(ListDataEvent e) {
            linea("  agrego [" + e.getIndex0() + "," + e.getIndex1() + "]");
        }

        public void intervalRemoved(ListDataEvent e) {
            linea("  saco [" + e.getIndex0() + "," + e.getIndex1() + "]");
        }
    }

    /** Anota cada aviso de seleccion. */
    static class EspiaSeleccion implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            linea("  seleccion [" + e.getFirstIndex() + "," + e.getLastIndex()
                    + "] ajustando=" + e.getValueIsAdjusting());
        }
    }

    static void estado(String titulo, ListSelectionModel m) {
        String s = "";
        for (int i = 0; i <= 12; i++) {
            s = s + (m.isSelectedIndex(i) ? "#" : ".");
        }
        linea(titulo + " [" + s + "] min=" + m.getMinSelectionIndex()
                + " max=" + m.getMaxSelectionIndex() + " ancla=" + m.getAnchorSelectionIndex()
                + " guia=" + m.getLeadSelectionIndex() + " vacia=" + m.isSelectionEmpty());
    }

    public static int run() {
        // --- el modelo de lista ---
        linea("=== modelo de lista");
        DefaultListModel<String> m = new DefaultListModel<String>();
        m.addListDataListener(new EspiaDatos());
        m.addElement("uno");
        m.addElement("dos");
        m.addElement("tres");
        linea("tamano=" + m.getSize() + " vacio=" + m.isEmpty() + " contiene dos="
                + m.contains("dos") + " indice de tres=" + m.indexOf("tres"));
        m.insertElementAt("cero", 0);
        m.setElementAt("DOS", 2);
        linea("texto=" + m);
        linea("saco=" + m.remove(1) + " reemplazo=" + m.set(0, "CERO"));
        linea("texto=" + m);
        m.addAll(java.util.Arrays.asList("a", "b", "c"));
        linea("texto=" + m + " capacidad>=" + (m.capacity() >= m.size()));
        m.removeRange(1, 2);
        linea("texto=" + m);
        linea("primero=" + m.firstElement() + " ultimo=" + m.lastElement());
        m.setSize(2);
        linea("texto=" + m);
        m.setSize(4);
        linea("texto=" + m);
        m.clear();
        linea("texto=" + m + " vacio=" + m.isEmpty());

        // --- el modelo de lista desplegable ---
        linea("=== modelo desplegable");
        String[] datos = {"rojo", "verde", "azul"};
        DefaultComboBoxModel<String> c = new DefaultComboBoxModel<String>(datos);
        c.addListDataListener(new EspiaDatos());
        linea("tamano=" + c.getSize() + " elegido=" + c.getSelectedItem()
                + " indice de azul=" + c.getIndexOf("azul"));
        c.setSelectedItem("verde");
        linea("elegido=" + c.getSelectedItem());
        c.removeElement("verde");
        linea("tras sacar el elegido: elegido=" + c.getSelectedItem()
                + " tamano=" + c.getSize());
        c.addElement("negro");
        linea("elegido=" + c.getSelectedItem());
        c.removeAllElements();
        linea("vacio: elegido=" + c.getSelectedItem() + " tamano=" + c.getSize());
        c.addElement("unico");
        linea("tras el primero: elegido=" + c.getSelectedItem());

        // --- la seleccion ---
        linea("=== seleccion");
        DefaultListSelectionModel s = new DefaultListSelectionModel();
        s.addListSelectionListener(new EspiaSeleccion());
        estado("inicio", s);
        s.setSelectionInterval(2, 5);
        estado("2..5", s);
        s.addSelectionInterval(8, 9);
        estado("mas 8..9", s);
        s.removeSelectionInterval(3, 4);
        estado("menos 3..4", s);
        linea("modo=" + s.getSelectionMode());
        s.setLeadSelectionIndex(1);
        estado("guia a 1", s);
        s.setAnchorSelectionIndex(6);
        estado("ancla a 6", s);
        s.setLeadSelectionIndex(10);
        estado("guia a 10", s);
        s.insertIndexInterval(0, 2, true);
        estado("inserta 2 al principio", s);
        s.removeIndexInterval(0, 1);
        estado("saca 2 del principio", s);
        s.setValueIsAdjusting(true);
        s.setSelectionInterval(1, 1);
        s.addSelectionInterval(3, 3);
        s.setValueIsAdjusting(false);
        estado("junta avisos", s);
        s.clearSelection();
        estado("limpia", s);
        s.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        s.setSelectionInterval(2, 5);
        estado("modo unico", s);

        // --- los mapas ---
        linea("=== mapas");
        ActionMap padre = new ActionMap();
        padre.put("a", new AbstractAction("A") {
            public void actionPerformed(java.awt.event.ActionEvent e) {
            }
        });
        ActionMap hijo = new ActionMap();
        hijo.setParent(padre);
        hijo.put("b", new AbstractAction("B") {
            public void actionPerformed(java.awt.event.ActionEvent e) {
            }
        });
        linea("hijo tamano=" + hijo.size() + " propias=" + hijo.keys().length
                + " todas=" + hijo.allKeys().length);
        linea("busca a=" + (hijo.get("a") != null) + " busca b=" + (hijo.get("b") != null)
                + " busca z=" + (hijo.get("z") != null));
        hijo.put("b", null);
        linea("tras poner nulo: tamano=" + hijo.size() + " b=" + (hijo.get("b") != null));
        hijo.clear();
        linea("tras limpiar: propias=" + hijo.keys().length
                + " todas=" + hijo.allKeys().length + " a=" + (hijo.get("a") != null));

        InputMap ip = new InputMap();
        InputMap ih = new InputMap();
        ih.setParent(ip);
        ip.put(KeyStroke.getKeyStroke('x'), "equis");
        ih.put(KeyStroke.getKeyStroke('y'), "ye");
        linea("entrada propias=" + ih.keys().length + " todas=" + ih.allKeys().length
                + " x=" + ih.get(KeyStroke.getKeyStroke('x')));
        return 0;
    }
}
