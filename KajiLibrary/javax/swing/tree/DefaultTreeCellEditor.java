package javax.swing.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 * Edita el nombre de un nodo de arbol, con el icono al lado.
 *
 * <h2>Tres piezas</h2>
 *
 * <p>Un editor <em>de verdad</em> -- un campo de texto envuelto en un {@link DefaultCellEditor} --,
 * un {@link DefaultTreeCellRenderer} del que se toman prestados el icono y la tipografia, y un
 * contenedor que pone los dos uno al lado del otro. El icono no se edita: esta ahi para que la fila
 * en edicion se siga pareciendo a las demas.
 *
 * <h2>El clic que empieza a editar es el segundo, y con pausa</h2>
 *
 * <p>Un clic sobre una fila <em>ya elegida</em> no empieza a editar enseguida: arranca un
 * {@link Timer} de un tercio de segundo. Es lo que distingue "hacer clic para elegir y despues clic
 * para renombrar" de "hacer doble clic", que es otra cosa. Un doble clic de verdad edita de
 * inmediato, sin esperar.
 *
 * <p>Ese temporizador es la razon de que esta clase escuche la seleccion del arbol: si la seleccion
 * cambia mientras se espera, el clic ya no significa renombrar.
 *
 * <h2>Donde se puede hacer clic</h2>
 *
 * <p>{@link #inHitRegion} decide si un clic cae sobre el texto o sobre el icono. Sobre el icono no
 * edita: el icono es para desplegar, no para renombrar.
 */
public class DefaultTreeCellEditor implements ActionListener, TreeCellEditor,
        TreeSelectionListener {

    /** El editor que de verdad edita; ver la nota de la clase. */
    protected TreeCellEditor realEditor;

    /** De donde salen el icono y la tipografia. */
    protected DefaultTreeCellRenderer renderer;

    /** El contenedor que pone el icono y el editor juntos. */
    protected Container editingContainer;

    /** Lo que el editor de verdad devolvio para editar. */
    protected transient Component editingComponent;

    /** Si el ultimo camino elegido se puede editar. */
    protected boolean canEdit;

    /** Cuanto se corre el editor a la derecha, por el icono. */
    protected transient int offset;

    /** El arbol al que pertenece. */
    protected transient JTree tree;

    /** El ultimo camino elegido. */
    protected transient TreePath lastPath;

    /** El temporizador del clic; ver la nota de la clase. */
    protected transient Timer timer;

    /** La ultima fila que se dibujo. */
    protected transient int lastRow;

    /** El color del recuadro alrededor de lo que se edita. */
    protected Color borderSelectionColor;

    /** El icono que se dibuja al lado del editor. */
    protected transient Icon editingIcon;

    /** La tipografia; nula toma la del arbol. */
    protected Font font;

    /** Con un editor de texto armado por {@link #createTreeCellEditor}. */
    public DefaultTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
        this(tree, renderer, null);
    }

    /** Con ese editor de verdad; nulo arma el de texto. */
    public DefaultTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer,
            TreeCellEditor editor) {
        this.renderer = renderer;
        realEditor = editor;
        if (realEditor == null) {
            realEditor = createTreeCellEditor();
        }
        editingContainer = createContainer();
        setTree(tree);
        setBorderSelectionColor(javax.swing.UIManager.getColor("Tree.editorBorderSelectionColor"));
    }

    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    /** La tipografia; nula deja la del dibujante o la del arbol. */
    public void setFont(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    /**
     * El componente con el que se edita esa fila, ya colocado al lado de su icono.
     *
     * <p>Se le pide al editor de verdad y se lo mete en el contenedor. El icono y el corrimiento
     * salen de {@link #determineOffset}.
     */
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row) {
        setTree(tree);
        lastRow = row;
        determineOffset(tree, value, isSelected, expanded, leaf, row);
        if (editingComponent != null) {
            editingContainer.remove(editingComponent);
        }
        editingComponent = realEditor.getTreeCellEditorComponent(tree, value, isSelected,
                expanded, leaf, row);
        TreePath newPath = tree.getPathForRow(row);
        canEdit = (lastPath != null && newPath != null && lastPath.equals(newPath));
        Font aFont = getFont();
        if (aFont == null) {
            if (renderer != null) {
                aFont = renderer.getFont();
            }
            if (aFont == null) {
                aFont = tree.getFont();
            }
        }
        editingContainer.setFont(aFont);
        prepareForEditing();
        return editingContainer;
    }

    public Object getCellEditorValue() {
        return realEditor.getCellEditorValue();
    }

    /**
     * Si ese gesto empieza la edicion.
     *
     * <p>Un clic sobre la fila ya elegida arranca el temporizador y devuelve falso: la edicion
     * empieza cuando el temporizador salta, no ahora. Ver la nota de la clase.
     */
    public boolean isCellEditable(EventObject event) {
        boolean retValue = false;
        boolean editable = false;
        if (event != null) {
            if (event.getSource() instanceof JTree) {
                setTree((JTree) event.getSource());
                if (event instanceof MouseEvent) {
                    TreePath path = tree.getPathForLocation(((MouseEvent) event).getX(),
                            ((MouseEvent) event).getY());
                    editable = (lastPath != null && path != null && lastPath.equals(path));
                    if (path != null) {
                        lastRow = tree.getRowForPath(path);
                        Object value = path.getLastPathComponent();
                        boolean isSelected = tree.isRowSelected(lastRow);
                        boolean expanded = tree.isExpanded(path);
                        determineOffset(tree, value, isSelected, expanded,
                                tree.getModel().isLeaf(value), lastRow);
                    }
                }
            }
        }
        if (!realEditor.isCellEditable(event)) {
            return false;
        }
        if (canEditImmediately(event)) {
            retValue = true;
        } else if (editable && shouldStartEditingTimer(event)) {
            startEditingTimer();
        } else if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        if (retValue) {
            prepareForEditing();
        }
        return retValue;
    }

    public boolean shouldSelectCell(EventObject event) {
        return realEditor.shouldSelectCell(event);
    }

    public boolean stopCellEditing() {
        if (realEditor.stopCellEditing()) {
            cleanupAfterEditing();
            return true;
        }
        return false;
    }

    public void cancelCellEditing() {
        realEditor.cancelCellEditing();
        cleanupAfterEditing();
    }

    public void addCellEditorListener(CellEditorListener l) {
        realEditor.addCellEditorListener(l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        realEditor.removeCellEditorListener(l);
    }

    public CellEditorListener[] getCellEditorListeners() {
        if (realEditor instanceof DefaultCellEditor) {
            return ((DefaultCellEditor) realEditor).getCellEditorListeners();
        }
        return new CellEditorListener[0];
    }

    /**
     * La seleccion cambio: se anota el nuevo camino y se corta el temporizador.
     *
     * <p>Sin esto, un clic que cambia la seleccion terminaria abriendo el editor sobre la fila que
     * el usuario acaba de dejar.
     */
    public void valueChanged(TreeSelectionEvent e) {
        if (tree != null) {
            if (tree.getSelectionCount() == 1) {
                lastPath = tree.getSelectionPath();
            } else {
                lastPath = null;
            }
        }
        if (timer != null) {
            timer.stop();
        }
    }

    /** El temporizador salto: ahora si se empieza a editar. */
    public void actionPerformed(ActionEvent e) {
        if (tree != null && lastPath != null) {
            tree.startEditingAtPath(lastPath);
        }
    }

    /**
     * Se ata a ese arbol.
     *
     * <p>Escucha su seleccion; ver {@link #valueChanged}.
     */
    protected void setTree(JTree newTree) {
        if (tree != newTree) {
            if (tree != null) {
                tree.removeTreeSelectionListener(this);
            }
            tree = newTree;
            if (tree != null) {
                tree.addTreeSelectionListener(this);
            }
            if (timer != null) {
                timer.stop();
            }
        }
    }

    /** Si ese gesto arranca el temporizador: un clic simple sobre lo ya elegido. */
    protected boolean shouldStartEditingTimer(EventObject event) {
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            return (javax.swing.SwingUtilities.isLeftMouseButton(me) && me.getClickCount() == 1
                    && inHitRegion(me.getX(), me.getY()));
        }
        return false;
    }

    /** Arranca el temporizador de un tercio de segundo; ver la nota de la clase. */
    protected void startEditingTimer() {
        if (timer == null) {
            timer = new Timer(1200, this);
            timer.setRepeats(false);
        }
        timer.start();
    }

    /** Si ese gesto edita sin esperar: un doble clic, o cualquier cosa que no sea del mouse. */
    protected boolean canEditImmediately(EventObject event) {
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            return (me.getClickCount() > 2 && inHitRegion(me.getX(), me.getY()));
        }
        return (event == null);
    }

    /**
     * Si ese punto cae sobre el texto y no sobre el icono.
     *
     * <p>Solo mira la coordenada horizontal: la vertical ya la resolvio quien eligio la fila.
     */
    protected boolean inHitRegion(int x, int y) {
        if (lastRow != -1 && tree != null) {
            Rectangle bounds = tree.getRowBounds(lastRow);
            if (bounds != null) {
                if (tree.getComponentOrientation().isLeftToRight()) {
                    return !(bounds.x + offset > x);
                }
                return !(bounds.x + bounds.width - offset < x);
            }
        }
        return true;
    }

    /**
     * Calcula el icono y cuanto hay que correr el editor.
     *
     * <p>El icono sale del dibujante, que sabe cual va segun sea hoja, carpeta abierta o cerrada.
     * Sin dibujante no hay icono y el editor arranca pegado a la izquierda.
     */
    protected void determineOffset(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row) {
        if (renderer != null) {
            if (leaf) {
                editingIcon = renderer.getLeafIcon();
            } else if (expanded) {
                editingIcon = renderer.getOpenIcon();
            } else {
                editingIcon = renderer.getClosedIcon();
            }
            if (editingIcon != null) {
                offset = renderer.getIconTextGap() + editingIcon.getIconWidth();
            } else {
                offset = renderer.getIconTextGap();
            }
        } else {
            editingIcon = null;
            offset = 0;
        }
    }

    /** Mete el editor en el contenedor. */
    protected void prepareForEditing() {
        if (editingComponent != null) {
            editingContainer.add(editingComponent);
        }
    }

    /** El contenedor que pone el icono y el editor juntos. */
    protected Container createContainer() {
        return new EditorContainer(this);
    }

    /**
     * El editor de texto que se usa si no se dio otro.
     *
     * <p>Con tres clics para empezar, que en la practica significa que el clic lo maneja esta clase
     * y no el editor de adentro.
     */
    protected TreeCellEditor createTreeCellEditor() {
        Border aBorder = javax.swing.UIManager.getBorder("Tree.editorBorder");
        DefaultCellEditor editor = new EditorDeTexto(new DefaultTextField(this, aBorder));
        editor.setClickCountToStart(1);
        return editor;
    }

    /** Deja todo como estaba cuando la edicion termina o se cancela. */
    private void cleanupAfterEditing() {
        if (editingComponent != null) {
            editingContainer.remove(editingComponent);
        }
        editingComponent = null;
    }

    /** Un {@link DefaultCellEditor} que ademas sirve como editor de arbol. */
    private static class EditorDeTexto extends DefaultCellEditor implements TreeCellEditor {

        EditorDeTexto(JTextField campo) {
            super(campo);
        }
    }

    /**
     * El campo de texto del editor de base.
     *
     * <p>Toma prestada la tipografia del editor que lo contiene, y se mide con un ancho minimo para
     * que un nombre vacio no deje un campo de cero pixeles.
     */
    public static class DefaultTextField extends JTextField {

        /** El borde; puede ser nulo. */
        protected Border border;

        private final DefaultTreeCellEditor editor;

        /** Con ese borde. */
        public DefaultTextField(DefaultTreeCellEditor editor, Border border) {
            this.editor = editor;
            setBorder(border);
        }

        public void setBorder(Border border) {
            super.setBorder(border);
            this.border = border;
        }

        public Border getBorder() {
            return border;
        }

        /** La del editor, si tiene; si no, la propia. */
        public Font getFont() {
            Font font = super.getFont();
            if (font instanceof javax.swing.plaf.FontUIResource) {
                Container parent = getParent();
                if (parent != null && parent.getFont() != null) {
                    font = parent.getFont();
                }
            }
            return font;
        }

        /** Al menos el ancho de la fila que se esta editando. */
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (editor != null && editor.renderer != null && editor.tree != null) {
                Dimension rSize = editor.renderer.getPreferredSize();
                size.height = rSize.height;
            }
            return size;
        }
    }

    /**
     * El contenedor que pone el icono a la izquierda y el editor a la derecha.
     *
     * <p>Es un {@link Container} de AWT y no un panel de Swing porque no tiene nada que hacer con
     * bordes ni con aspectos: solo coloca dos cosas y dibuja un icono.
     */
    public static class EditorContainer extends Container {

        private final DefaultTreeCellEditor editor;

        /** Vacio, con acomodador propio. */
        public EditorContainer(DefaultTreeCellEditor editor) {
            this.editor = editor;
            setLayout(null);
        }

        /**
         * No hace nada.
         *
         * <p>Es un metodo con el nombre de la clase y tipo de retorno {@code void} -- o sea, no es
         * un constructor. Esta en el JDK por un accidente historico que ya no se puede sacar sin
         * romper compatibilidad binaria, y se copia por eso.
         */
        public void EditorContainer() {
        }

        /** Dibuja el icono a la izquierda y despues los hijos. */
        public void paint(Graphics g) {
            if (editor != null && editor.editingComponent != null) {
                Icon icon = editor.editingIcon;
                if (icon != null) {
                    int yLoc = Math.max(0, (getHeight() - icon.getIconHeight()) / 2);
                    int xLoc = 0;
                    if (!getComponentOrientation().isLeftToRight()) {
                        xLoc = getWidth() - editor.offset;
                    }
                    icon.paintIcon(this, g, xLoc, yLoc);
                }
            }
            super.paint(g);
        }

        /** El editor ocupa todo menos lo que se lleva el icono. */
        public void doLayout() {
            if (editor != null && editor.editingComponent != null) {
                int width = getWidth();
                int height = getHeight();
                if (getComponentOrientation().isLeftToRight()) {
                    editor.editingComponent.setBounds(editor.offset, 0,
                            width - editor.offset, height);
                } else {
                    editor.editingComponent.setBounds(0, 0, width - editor.offset, height);
                }
            }
        }

        public Dimension getPreferredSize() {
            if (editor != null && editor.editingComponent != null) {
                Dimension pSize = editor.editingComponent.getPreferredSize();
                pSize.width += editor.offset + 5;
                if (editor.renderer != null) {
                    Dimension rSize = editor.renderer.getPreferredSize();
                    pSize.height = Math.max(pSize.height, rSize.height);
                }
                if (editor.editingIcon != null) {
                    pSize.height = Math.max(pSize.height, editor.editingIcon.getIconHeight());
                }
                pSize.width = Math.max(pSize.width, 100);
                return pSize;
            }
            return new Dimension(0, 0);
        }
    }
}
