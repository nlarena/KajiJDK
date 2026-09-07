package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ActionMap;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

/**
 * El selector de archivos de Metal.
 *
 * <h2>Un tamano fijo de 500 x 326</h2>
 *
 * <p>El preferido y el minimo son el mismo numero y no dependen de nada: ni del contenido, ni de la
 * carpeta, ni de la fuente. Medido. Un selector que se achicara para caber en su contenido seria
 * inservible -- la lista de archivos entra en cualquier tamano, cortando -- y uno que creciera con
 * el nombre mas largo cambiaria de tamano al navegar.
 *
 * <p>El maximo, en cambio, es {@code Integer.MAX_VALUE}: agrandarlo si sirve.
 *
 * <h2>Dos desplegables con modelo propio</h2>
 *
 * <p>{@link DirectoryComboBoxModel} es el de arriba -- la ruta, de la raiz a la carpeta actual --
 * y {@link FilterComboBoxModel} el de abajo, con los filtros. Los dos son modelos y no listas
 * porque su contenido cambia solo: el primero cada vez que se navega, el segundo cuando el
 * programa agrega un filtro.
 *
 * <h2>Un campo de archivo y ningun campo de carpeta</h2>
 *
 * <p>Metal tiene un campo donde se escribe el nombre del archivo, y de ahi que
 * {@link #getFileName} conteste la cadena vacia donde el basico contesta {@code null}. Carpeta no
 * tiene: la ruta se elige en el desplegable de arriba, no se escribe. Por eso
 * {@link #getDirectoryName} es {@code null} <em>siempre</em> y {@link #setDirectoryName} no hace
 * nada. Las dos cosas, medidas.
 *
 * <h2>Lo que queda dicho y no tapado</h2>
 *
 * <p>Los componentes no se arman. {@link #installComponents} deja el selector como lo dejo el
 * basico; la lista, los dos desplegables y los botones necesitan una tabla de aspecto con once
 * iconos que no estan. Lo que si contesta bien es todo lo que no dibuja: los tres tamanos, los
 * modelos, el mapa de acciones y los nombres.
 */
public class MetalFileChooserUI extends BasicFileChooserUI {

    private static final Dimension FIJO = new Dimension(500, 326);
    private static final Dimension MAXIMO =
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** El campo de archivo; ver la nota de la clase. */
    private final JTextField campoDeNombre = new JTextField();

    private JPanel panelDeBotones;
    private JPanel panelDeAbajo;
    private ActionMap acciones;

    public MetalFileChooserUI(JFileChooser filechooser) {
        super(filechooser);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalFileChooserUI((JFileChooser) c);
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected void installStrings(JFileChooser fc) {
        super.installStrings(fc);
    }

    protected void installListeners(JFileChooser fc) {
        super.installListeners(fc);
    }

    public void installComponents(JFileChooser fc) {
        super.installComponents(fc);
    }

    public void uninstallComponents(JFileChooser fc) {
        super.uninstallComponents(fc);
    }

    /** Quinientos por trescientos veintiseis; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(FIJO);
    }

    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(FIJO);
    }

    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(MAXIMO);
    }

    /** El panel con los botones de aprobar y cancelar. */
    protected JPanel getButtonPanel() {
        if (panelDeBotones == null) {
            panelDeBotones = new JPanel();
        }
        return panelDeBotones;
    }

    /** El de abajo, que lo contiene. */
    protected JPanel getBottomPanel() {
        if (panelDeAbajo == null) {
            panelDeAbajo = new JPanel();
        }
        return panelDeAbajo;
    }

    /**
     * Pone los dos botones -- aprobar y cancelar -- en su panel.
     *
     * <p>Son de Metal y no del basico: el basico no arma botones. Sin la tabla de aspecto el panel
     * queda vacio; ver la nota de la clase.
     */
    protected void addControlButtons() {
        getBottomPanel().add(getButtonPanel());
    }

    protected void removeControlButtons() {
        getBottomPanel().remove(getButtonPanel());
    }

    protected JButton getApproveButton(JFileChooser fc) {
        JButton b = super.getApproveButton(fc);
        if (b == null) {
            b = new JButton(getApproveButtonText(fc));
        }
        return b;
    }

    protected ActionMap createActionMap() {
        return new ActionMapUIResource();
    }

    protected ActionMap getActionMap() {
        if (acciones == null) {
            acciones = createActionMap();
        }
        return acciones;
    }

    /** Sin la lista armada no hay panel que devolver; ver la nota de la clase. */
    protected JPanel createList(JFileChooser fc) {
        return new JPanel();
    }

    protected JPanel createDetailsView(JFileChooser fc) {
        return new JPanel();
    }

    public ListSelectionListener createListSelectionListener(JFileChooser fc) {
        return new SelectionListener(this);
    }

    public void valueChanged(ListSelectionEvent e) {
    }

    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        return super.createPropertyChangeListener(fc);
    }

    protected DirectoryComboBoxModel createDirectoryComboBoxModel(JFileChooser fc) {
        return new DirectoryComboBoxModel();
    }

    protected FilterComboBoxModel createFilterComboBoxModel() {
        return new FilterComboBoxModel(getFileChooser());
    }

    protected FilterComboBoxRenderer createFilterComboBoxRenderer() {
        return new FilterComboBoxRenderer();
    }

    /** La cadena vacia mientras nadie escriba; ver la nota de la clase. */
    public String getFileName() {
        return campoDeNombre.getText();
    }

    public void setFileName(String filename) {
        campoDeNombre.setText((filename == null) ? "" : filename);
    }

    /** Nulo siempre; ver la nota de la clase. */
    public String getDirectoryName() {
        return null;
    }

    /** No hace nada; ver {@link #getDirectoryName}. */
    public void setDirectoryName(String dirname) {
    }

    public void rescanCurrentDirectory(JFileChooser fc) {
        super.rescanCurrentDirectory(fc);
    }

    public void ensureFileIsVisible(JFileChooser fc, File f) {
        super.ensureFileIsVisible(fc, f);
    }

    protected void setDirectorySelected(boolean directorySelected) {
        super.setDirectorySelected(directorySelected);
    }

    /** La ruta de la raiz a la carpeta actual, del desplegable de arriba. */
    protected class DirectoryComboBoxModel extends javax.swing.AbstractListModel<Object>
            implements ComboBoxModel<Object> {

        private final java.util.List<File> ruta = new java.util.ArrayList<File>();
        private Object elegido;

        public DirectoryComboBoxModel() {
        }

        public int getSize() {
            return ruta.size();
        }

        public Object getElementAt(int index) {
            return (index >= 0 && index < ruta.size()) ? ruta.get(index) : null;
        }

        public void setSelectedItem(Object anItem) {
            elegido = anItem;
            fireContentsChanged(this, -1, -1);
        }

        public Object getSelectedItem() {
            return elegido;
        }

        /** La profundidad de esa entrada dentro de la ruta; es lo que la sangra. */
        public int getDepth(int i) {
            return i;
        }
    }

    /**
     * Los filtros que el selector acepta.
     *
     * <p>El selector viene por parametro y no se pide con {@code getFileChooser()}: ese metodo lo
     * hereda la clase de afuera, y llamarlo desde adentro de una clase interna revienta con
     * {@code NoSuchMethodError}. Ver el hallazgo #522.
     */
    protected class FilterComboBoxModel extends javax.swing.AbstractListModel<Object>
            implements ComboBoxModel<Object>, java.beans.PropertyChangeListener {

        protected javax.swing.filechooser.FileFilter[] filters;

        private final JFileChooser selector;

        public FilterComboBoxModel(JFileChooser fc) {
            selector = fc;
            filters = (fc == null) ? null : fc.getChoosableFileFilters();
        }

        public void propertyChange(java.beans.PropertyChangeEvent e) {
            if ("ChoosableFileFilterChangedProperty".equals(e.getPropertyName())
                    && selector != null) {
                filters = selector.getChoosableFileFilters();
                fireContentsChanged(this, -1, -1);
            }
        }

        public int getSize() {
            return (filters == null) ? 0 : filters.length;
        }

        public Object getElementAt(int index) {
            if (filters == null || index < 0 || index >= filters.length) {
                return null;
            }
            return filters[index];
        }

        public void setSelectedItem(Object filter) {
            if (selector != null && filter instanceof javax.swing.filechooser.FileFilter) {
                selector.setFileFilter((javax.swing.filechooser.FileFilter) filter);
                fireContentsChanged(this, -1, -1);
            }
        }

        public Object getSelectedItem() {
            return (selector == null) ? null : selector.getFileFilter();
        }
    }

    /** Un filtro se muestra por su descripcion, no por su {@code toString}. */
    public class FilterComboBoxRenderer extends javax.swing.DefaultListCellRenderer {

        public FilterComboBoxRenderer() {
        }

        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            java.awt.Component c = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof javax.swing.filechooser.FileFilter) {
                setText(((javax.swing.filechooser.FileFilter) value).getDescription());
            }
            return c;
        }
    }

    /**
     * El que se entera de que se eligio otro archivo en la lista.
     *
     * <p>Estatica y con el UI por parametro, por lo mismo que {@link FilterComboBoxModel}.
     */
    private static class SelectionListener implements ListSelectionListener {

        private final MetalFileChooserUI ui;

        SelectionListener(MetalFileChooserUI ui) {
            this.ui = ui;
        }

        public void valueChanged(ListSelectionEvent e) {
            ui.valueChanged(e);
        }
    }
}
