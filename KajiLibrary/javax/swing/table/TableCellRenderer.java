package javax.swing.table;

import java.awt.Component;

import javax.swing.JTable;

/**
 * How a cell is drawn.
 *
 * <h2>A borrowed component, not one per cell</h2>
 *
 * <p>The method returns a {@link Component}, and the temptation is to think each cell has its
 * own. It does not: a table of ten thousand rows creating ten thousand labels would be unviable.
 * What is done is to return <strong>always the same component</strong>, reconfigured with the
 * value of the cell in turn, use it to paint, and move on to the next.
 *
 * <p>Hence the returned component must not be kept nor subscribed to anything: it lives as long
 * as a paint lasts. It is the pattern the JDK calls <em>rubber stamp</em>, and it explains why
 * all the parameters --selected, focused, row, column-- arrive together: they are everything
 * needed to reconfigure it in one go.
 */
public interface TableCellRenderer {

    /**
     * Configures and returns the component to paint that cell with.
     *
     * @param isSelected whether the cell is selected
     * @param hasFocus whether it has the keyboard focus
     */
    Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column);
}
