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
 * Edits a tree node's name, with the icon beside it.
 *
 * <h2>Three pieces</h2>
 *
 * <p>A <em>real</em> editor -- a text field wrapped in a {@link DefaultCellEditor} --, a
 * {@link DefaultTreeCellRenderer} from which the icon and the typeface are borrowed, and a
 * container that puts the two side by side. The icon is not edited: it is there so that the row
 * being edited goes on looking like the others.
 *
 * <h2>The click that starts editing is the second one, and with a pause</h2>
 *
 * <p>A click on an <em>already chosen</em> row does not start editing right away: it starts a
 * {@link Timer} of a third of a second. It is what tells "click to choose and then click to
 * rename" apart from "double-click", which is another thing. A real double click edits
 * immediately, without waiting.
 *
 * <p>That timer is the reason this class listens to the tree's selection: if the selection
 * changes while waiting, the click no longer means rename.
 *
 * <h2>Where one can click</h2>
 *
 * <p>{@link #inHitRegion} decides whether a click falls on the text or on the icon. On the icon
 * it does not edit: the icon is for expanding, not for renaming.
 */
public class DefaultTreeCellEditor implements ActionListener, TreeCellEditor,
        TreeSelectionListener {

    /** The editor that really edits; see the class note. */
    protected TreeCellEditor realEditor;

    /** Where the icon and the typeface come from. */
    protected DefaultTreeCellRenderer renderer;

    /** The container that puts the icon and the editor together. */
    protected Container editingContainer;

    /** What the real editor returned to edit with. */
    protected transient Component editingComponent;

    /** Whether the last chosen path can be edited. */
    protected boolean canEdit;

    /** How far the editor is shifted right, because of the icon. */
    protected transient int offset;

    /** The tree it belongs to. */
    protected transient JTree tree;

    /** The last chosen path. */
    protected transient TreePath lastPath;

    /** The click's timer; see the class note. */
    protected transient Timer timer;

    /** The last row that was drawn. */
    protected transient int lastRow;

    /** The colour of the box around what is being edited. */
    protected Color borderSelectionColor;

    /** The icon drawn beside the editor. */
    protected transient Icon editingIcon;

    /** The typeface; null takes the tree's. */
    protected Font font;

    /** With a text editor built by {@link #createTreeCellEditor}. */
    public DefaultTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
        this(tree, renderer, null);
    }

    /** With that real editor; null builds the text one. */
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

    /** The typeface; null leaves the renderer's or the tree's. */
    public void setFont(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    /**
     * The component to edit that row with, already placed beside its icon.
     *
     * <p>It is asked of the real editor and put into the container. The icon and the shift come
     * from {@link #determineOffset}.
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
     * Whether that gesture starts the editing.
     *
     * <p>A click on the already chosen row starts the timer and returns false: the editing starts
     * when the timer fires, not now. See the class note.
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
     * The selection changed: the new path is noted and the timer is cut off.
     *
     * <p>Without this, a click that changes the selection would end up opening the editor over the
     * row the user has just left.
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

    /** The timer fired: now the editing does start. */
    public void actionPerformed(ActionEvent e) {
        if (tree != null && lastPath != null) {
            tree.startEditingAtPath(lastPath);
        }
    }

    /**
     * It ties itself to that tree.
     *
     * <p>It listens to its selection; see {@link #valueChanged}.
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

    /** Whether that gesture starts the timer: a single click on what is already chosen. */
    protected boolean shouldStartEditingTimer(EventObject event) {
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            return (javax.swing.SwingUtilities.isLeftMouseButton(me) && me.getClickCount() == 1
                    && inHitRegion(me.getX(), me.getY()));
        }
        return false;
    }

    /** Starts the third-of-a-second timer; see the class note. */
    protected void startEditingTimer() {
        if (timer == null) {
            timer = new Timer(1200, this);
            timer.setRepeats(false);
        }
        timer.start();
    }

    /**
     * Whether that gesture edits without waiting: a double click, or anything not from the mouse.
     */
    protected boolean canEditImmediately(EventObject event) {
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            return (me.getClickCount() > 2 && inHitRegion(me.getX(), me.getY()));
        }
        return (event == null);
    }

    /**
     * Whether that point falls on the text and not on the icon.
     *
     * <p>It only looks at the horizontal coordinate: the vertical one was already resolved by
     * whoever chose the row.
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
     * Computes the icon and how far the editor has to be shifted.
     *
     * <p>The icon comes from the renderer, which knows which one goes according to whether it is a
     * leaf, an open folder or a closed one. With no renderer there is no icon and the editor starts
     * flush left.
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

    /** Puts the editor into the container. */
    protected void prepareForEditing() {
        if (editingComponent != null) {
            editingContainer.add(editingComponent);
        }
    }

    /** The container that puts the icon and the editor together. */
    protected Container createContainer() {
        return new EditorContainer(this);
    }

    /**
     * The text editor used if no other was given.
     *
     * <p>With three clicks to start, which in practice means that the click is handled by this
     * class and not by the editor inside.
     */
    protected TreeCellEditor createTreeCellEditor() {
        Border aBorder = javax.swing.UIManager.getBorder("Tree.editorBorder");
        DefaultCellEditor editor = new TextEditor(new DefaultTextField(this, aBorder));
        editor.setClickCountToStart(1);
        return editor;
    }

    /** It leaves everything as it was when the editing ends or is cancelled. */
    private void cleanupAfterEditing() {
        if (editingComponent != null) {
            editingContainer.remove(editingComponent);
        }
        editingComponent = null;
    }

    /** A {@link DefaultCellEditor} that also serves as a tree editor. */
    private static class TextEditor extends DefaultCellEditor implements TreeCellEditor {

        TextEditor(JTextField field) {
            super(field);
        }
    }

    /**
     * The base editor's text field.
     *
     * <p>It borrows the typeface from the editor that contains it, and measures itself with a
     * minimum width so that an empty name does not leave a field of zero pixels.
     */
    public static class DefaultTextField extends JTextField {

        /** The border; it may be null. */
        protected Border border;

        private final DefaultTreeCellEditor editor;

        /** With that border. */
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

        /** The editor's, if it has one; otherwise, its own. */
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

        /** At least the width of the row being edited. */
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
     * The container that puts the icon on the left and the editor on the right.
     *
     * <p>It is an AWT {@link Container} and not a Swing panel because it has nothing to do with
     * borders or looks and feels: it only places two things and draws an icon.
     */
    public static class EditorContainer extends Container {

        private final DefaultTreeCellEditor editor;

        /** Empty, with a layout manager of its own. */
        public EditorContainer(DefaultTreeCellEditor editor) {
            this.editor = editor;
            setLayout(null);
        }

        /**
         * It does nothing.
         *
         * <p>It is a method with the class's name and return type {@code void} -- that is, it is
         * not a constructor. It is in the JDK because of a historical accident that can no longer
         * be removed without breaking binary compatibility, and it is copied for that reason.
         */
        public void EditorContainer() {
        }

        /** Draws the icon on the left and then the children. */
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

        /** The editor takes up everything except what the icon takes. */
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
