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
 * Metal's file chooser.
 *
 * <h2>A fixed size of 500 x 326</h2>
 *
 * <p>The preferred and the minimum are the same number and depend on nothing: not on the
 * content, not on the folder, not on the font. Measured. A chooser that shrank to fit its
 * content would be useless -- the file list fits any size, by cutting -- and one that grew with
 * the longest name would change size while navigating.
 *
 * <p>The maximum, on the other hand, is {@code Integer.MAX_VALUE}: growing it does serve.
 *
 * <h2>Two combo boxes with models of their own</h2>
 *
 * <p>{@link DirectoryComboBoxModel} is the top one -- the path, from the root to the current
 * folder -- and {@link FilterComboBoxModel} the bottom one, with the filters. Both are models
 * and not lists because their content changes by itself: the first every time one navigates, the
 * second when the program adds a filter.
 *
 * <h2>A file field and no folder field</h2>
 *
 * <p>Metal has a field where the file's name is typed, and hence {@link #getFileName} answers
 * the empty string where the basic one answers {@code null}. A folder field it does not have:
 * the path is chosen in the top combo box, it is not typed. That is why
 * {@link #getDirectoryName} is {@code null} <em>always</em> and {@link #setDirectoryName} does
 * nothing. Both things, measured.
 *
 * <h2>What is said and not covered up</h2>
 *
 * <p>The components are not assembled. {@link #installComponents} leaves the chooser as the
 * basic one left it; the list, the two combo boxes and the buttons need a look and feel table
 * with eleven icons that are not there. What does answer correctly is everything that does not
 * draw: the three sizes, the models, the action map and the names.
 */
public class MetalFileChooserUI extends BasicFileChooserUI {

    private static final Dimension FIXED = new Dimension(500, 326);
    private static final Dimension MAX =
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** The file field; see the class note. */
    private final JTextField nameField = new JTextField();

    private JPanel buttonPanel;
    private JPanel bottomPanel;
    private ActionMap actions;

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

    /** Five hundred by three hundred and twenty-six; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(FIXED);
    }

    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(FIXED);
    }

    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(MAX);
    }

    /** The panel with the approve and cancel buttons. */
    protected JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel();
        }
        return buttonPanel;
    }

    /** The bottom one, which contains it. */
    protected JPanel getBottomPanel() {
        if (bottomPanel == null) {
            bottomPanel = new JPanel();
        }
        return bottomPanel;
    }

    /**
     * It puts the two buttons -- approve and cancel -- in their panel.
     *
     * <p>They are Metal's and not the basic one's: the basic one does not assemble buttons. Without
     * the look and feel table the panel is left empty; see the class note.
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
        if (actions == null) {
            actions = createActionMap();
        }
        return actions;
    }

    /** Without the list assembled there is no panel to return; see the class note. */
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

    /** The empty string while nobody types; see the class note. */
    public String getFileName() {
        return nameField.getText();
    }

    public void setFileName(String filename) {
        nameField.setText((filename == null) ? "" : filename);
    }

    /** Null always; see the class note. */
    public String getDirectoryName() {
        return null;
    }

    /** It does nothing; see {@link #getDirectoryName}. */
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

    /** The path from the root to the current folder, from the top combo box. */
    protected class DirectoryComboBoxModel extends javax.swing.AbstractListModel<Object>
            implements ComboBoxModel<Object> {

        private final java.util.List<File> path = new java.util.ArrayList<File>();
        private Object chosen;

        public DirectoryComboBoxModel() {
        }

        public int getSize() {
            return path.size();
        }

        public Object getElementAt(int index) {
            return (index >= 0 && index < path.size()) ? path.get(index) : null;
        }

        public void setSelectedItem(Object anItem) {
            chosen = anItem;
            fireContentsChanged(this, -1, -1);
        }

        public Object getSelectedItem() {
            return chosen;
        }

        /** That entry's depth within the path; it is what indents it. */
        public int getDepth(int i) {
            return i;
        }
    }

    /**
     * The filters the chooser accepts.
     *
     * <p>The chooser arrives as a parameter and is not asked for with {@code getFileChooser()}:
     * that method is inherited by the outer class, and calling it from inside an inner class blows
     * up with {@code NoSuchMethodError}. See finding #522.
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

    /** A filter is shown by its description, not by its {@code toString}. */
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
     * The one that hears that another file was chosen in the list.
     *
     * <p>Static and with the UI as a parameter, for the same reason as
     * {@link FilterComboBoxModel}.
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
