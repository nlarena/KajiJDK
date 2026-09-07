package javax.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EventObject;

import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellEditor;

/**
 * El editor de celda que sirve para los tres casos comunes: texto, tilde y lista desplegable.
 *
 * <h2>Un editor, tres componentes, un delegado</h2>
 *
 * <p>Los tres componentes no tienen nada en comun: uno guarda texto, otro un booleano, otro un
 * elemento elegido. En vez de tres clases, hay una y un {@link EditorDelegate} que sabe hablar con
 * el componente que le toco. Los tres constructores arman el delegado que corresponde.
 *
 * <p>El delegado es protegido y reemplazable a proposito: es el punto donde se le enseña a esta
 * clase a manejar un componente que no es ninguno de los tres.
 *
 * <h2>Cuando empieza a editar</h2>
 *
 * <p>{@link #setClickCountToStart} decide cuantos clics hacen falta. Es dos para el campo de texto y
 * <strong>uno</strong> para el tilde y la lista, y la diferencia tiene sentido: en un campo de texto
 * el primer clic se usa para poner el cursor, mientras que un tilde no tiene nada que hacer con un
 * clic que no sea cambiar.
 *
 * <p>Un gesto que no es del mouse -- una tecla -- empieza la edicion sin contar clics.
 */
public class DefaultCellEditor extends AbstractCellEditor
        implements TableCellEditor, TreeCellEditor {

    /** El componente con el que se edita. */
    protected JComponent editorComponent;

    /** Quien sabe hablar con ese componente; ver la nota de la clase. */
    protected EditorDelegate delegate;

    /** Cuantos clics empiezan la edicion. */
    protected int clickCountToStart = 1;

    /**
     * Con un campo de texto.
     *
     * <p>Dos clics, por lo que dice la nota de la clase.
     */
    public DefaultCellEditor(final JTextField textField) {
        editorComponent = textField;
        this.clickCountToStart = 2;
        delegate = new DelegadoDeTexto(this, textField);
        textField.addActionListener(delegate);
    }

    /** Con un tilde; un solo clic. */
    public DefaultCellEditor(final JCheckBox checkBox) {
        editorComponent = checkBox;
        delegate = new DelegadoDeTilde(this, checkBox);
        checkBox.addActionListener(delegate);
        checkBox.setRequestFocusEnabled(false);
    }

    /**
     * Con una lista desplegable; un solo clic.
     *
     * <p>La lista queda marcada para que apretar Enter no dispare tambien el boton por omision de la
     * ventana: en una celda, Enter significa "termine de editar".
     */
    public DefaultCellEditor(final JComboBox<?> comboBox) {
        editorComponent = comboBox;
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        delegate = new DelegadoDeLista(this, comboBox);
        comboBox.addActionListener(delegate);
    }

    /** El componente con el que se edita. */
    public Component getComponent() {
        return editorComponent;
    }

    /** Cuantos clics empiezan la edicion; ver la nota de la clase. */
    public void setClickCountToStart(int count) {
        clickCountToStart = count;
    }

    public int getClickCountToStart() {
        return clickCountToStart;
    }

    public Object getCellEditorValue() {
        return delegate.getCellEditorValue();
    }

    /** Si ese gesto alcanza para empezar a editar. */
    public boolean isCellEditable(EventObject anEvent) {
        return delegate.isCellEditable(anEvent);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return delegate.shouldSelectCell(anEvent);
    }

    public boolean stopCellEditing() {
        return delegate.stopCellEditing();
    }

    public void cancelCellEditing() {
        delegate.cancelCellEditing();
    }

    /** El componente ya cargado con el valor de esa fila del arbol. */
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row) {
        String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row,
                false);
        delegate.setValue(stringValue);
        return editorComponent;
    }

    /**
     * El componente ya cargado con el valor de esa celda de la tabla.
     *
     * <p><strong>El tilde lleva un paso de mas.</strong> Un tilde no llena toda la celda, asi que se
     * le pide a la tabla el dibujante de esa celda y se le copian el borde y el color de fondo; sin
     * eso, al empezar a editar se ve un parpadeo donde la celda cambia de aspecto. Los otros dos
     * editores llenan la celda y no lo necesitan.
     */
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        delegate.setValue(value);
        if (editorComponent instanceof JCheckBox) {
            javax.swing.table.TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component c = renderer.getTableCellRendererComponent(table, value, isSelected, true,
                    row, column);
            if (c != null) {
                editorComponent.setOpaque(true);
                editorComponent.setBackground(c.getBackground());
                if (c instanceof JComponent) {
                    editorComponent.setBorder(((JComponent) c).getBorder());
                }
            } else {
                editorComponent.setOpaque(false);
            }
        }
        return editorComponent;
    }

    /**
     * Lo que hay que saber de cada componente para editar con el.
     *
     * <p>Escucha al componente -- de ahi que implemente los dos oyentes -- para terminar la edicion
     * cuando el usuario aprieta Enter o elige de la lista.
     */
    protected static class EditorDelegate implements ActionListener, ItemListener, Serializable {

        /** El valor que se esta editando. */
        protected Object value;

        /**
         * El editor al que pertenece este delegado.
         *
         * <p>En el JDK esto es una clase interna y la referencia a la externa es implicita. Aca es
         * anidada estatica con la externa como primer parametro -- que es la misma firma que el JDK
         * emite -- porque este compilador no analiza `externa.super(...)`; ver el hallazgo #518.
         */
        final DefaultCellEditor editor;

        /** Para las subclases. */
        protected EditorDelegate(DefaultCellEditor editor) {
            this.editor = editor;
        }

        public Object getCellEditorValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        /**
         * Si ese gesto alcanza.
         *
         * <p>Un clic cuenta los clics; cualquier otro evento -- una tecla, o nada -- empieza la
         * edicion sin mas.
         */
        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                return ((MouseEvent) anEvent).getClickCount() >= editor.clickCountToStart;
            }
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        /** Empieza a editar. Nadie la llama en esta biblioteca; esta porque el JDK la expone. */
        public boolean startCellEditing(EventObject anEvent) {
            return true;
        }

        public boolean stopCellEditing() {
            editor.fireEditingStopped();
            return true;
        }

        public void cancelCellEditing() {
            editor.fireEditingCanceled();
        }

        /** El usuario apreto Enter o eligio: se termina de editar. */
        public void actionPerformed(ActionEvent e) {
            editor.stopCellEditing();
        }

        /** Lo mismo, para los componentes que avisan por seleccion y no por accion. */
        public void itemStateChanged(ItemEvent e) {
            editor.stopCellEditing();
        }
    }

    /** El delegado que habla con un campo de texto: el valor es lo escrito. */
    private static class DelegadoDeTexto extends EditorDelegate {

        private final JTextField campo;

        DelegadoDeTexto(DefaultCellEditor editor, JTextField campo) {
            super(editor);
            this.campo = campo;
        }

        public void setValue(Object value) {
            campo.setText((value != null) ? value.toString() : "");
        }

        public Object getCellEditorValue() {
            return campo.getText();
        }
    }

    /** El delegado que habla con un tilde: el valor es un booleano. */
    private static class DelegadoDeTilde extends EditorDelegate {

        private final JCheckBox tilde;

        DelegadoDeTilde(DefaultCellEditor editor, JCheckBox tilde) {
            super(editor);
            this.tilde = tilde;
        }

        /** Un texto tambien sirve: {@code "true"} prende, cualquier otro apaga. */
        public void setValue(Object value) {
            boolean selected = false;
            if (value instanceof Boolean) {
                selected = ((Boolean) value).booleanValue();
            } else if (value instanceof String) {
                selected = value.equals("true");
            }
            tilde.setSelected(selected);
        }

        public Object getCellEditorValue() {
            return Boolean.valueOf(tilde.isSelected());
        }
    }

    /** El delegado que habla con una lista desplegable: el valor es lo elegido. */
    private static class DelegadoDeLista extends EditorDelegate {

        private final JComboBox<?> lista;

        DelegadoDeLista(DefaultCellEditor editor, JComboBox<?> lista) {
            super(editor);
            this.lista = lista;
        }

        public void setValue(Object value) {
            lista.setSelectedItem(value);
        }

        public Object getCellEditorValue() {
            return lista.getSelectedItem();
        }

        /**
         * Un clic sobre la lista no empieza a editar por si mismo.
         *
         * <p>La lista ya reacciona al clic abriendose; contar ese clic como el que empieza la
         * edicion haria que el desplegable se abriera y se cerrara de una.
         */
        public boolean shouldSelectCell(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                MouseEvent e = (MouseEvent) anEvent;
                return e.getID() != MouseEvent.MOUSE_DRAGGED;
            }
            return true;
        }

        public boolean stopCellEditing() {
            if (lista.isEditable()) {
                // Una lista editable puede tener texto a medio escribir; se lo confirma antes.
                lista.actionPerformed(new ActionEvent(this, 0, ""));
            }
            return super.stopCellEditing();
        }
    }
}
