package javax.swing.tree;

import java.awt.Component;

import javax.swing.JTree;

/**
 * Whoever turns a model node into something that can be drawn.
 *
 * <p>The same idea as {@link javax.swing.ListCellRenderer}: it returns a component used as a
 * stamp, one per visible row. See that note.
 *
 * <p>What it adds are the data only a tree has: whether the node is expanded, whether it is a
 * leaf, and which row it falls in. With that the renderer can pick the open folder icon, the
 * closed folder one or the file one without asking the model anything.
 */
public interface TreeCellRenderer {

    /** The component that draws that node. */
    Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus);
}
