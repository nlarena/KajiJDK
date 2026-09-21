package javax.swing;

/**
 * The names of a {@link JScrollPane}'s nine positions and the six bar policies.
 *
 * <p>They are in a separate interface, and not as constants of {@code JScrollPane}, because they
 * are used by three classes that do not inherit from one another: the pane, its layout and its
 * look and feel. Each one implements them and names them unqualified.
 *
 * <p>The positions are strings and not integers because they are constraints of {@code add}:
 * they go to the same place as {@code BorderLayout.CENTER}. The four <em>leading</em> and
 * <em>trailing</em> corners -- {@code LEADING}, {@code TRAILING} -- are resolved according to
 * the pane's orientation, and they are the ones to use so that a corner goes on being on the
 * right side in a language that is read right to left.
 */
public interface ScrollPaneConstants {

    /** The viewport that shows the content. */
    String VIEWPORT = "VIEWPORT";

    String VERTICAL_SCROLLBAR = "VERTICAL_SCROLLBAR";

    String HORIZONTAL_SCROLLBAR = "HORIZONTAL_SCROLLBAR";

    /** The fixed strip on the left, which scrolls only vertically. */
    String ROW_HEADER = "ROW_HEADER";

    /** The fixed strip at the top, which scrolls only horizontally. */
    String COLUMN_HEADER = "COLUMN_HEADER";

    String LOWER_LEFT_CORNER = "LOWER_LEFT_CORNER";

    String LOWER_RIGHT_CORNER = "LOWER_RIGHT_CORNER";

    String UPPER_LEFT_CORNER = "UPPER_LEFT_CORNER";

    String UPPER_RIGHT_CORNER = "UPPER_RIGHT_CORNER";

    /** The bottom corner on the side the line starts on; see the interface note. */
    String LOWER_LEADING_CORNER = "LOWER_LEADING_CORNER";

    String LOWER_TRAILING_CORNER = "LOWER_TRAILING_CORNER";

    String UPPER_LEADING_CORNER = "UPPER_LEADING_CORNER";

    String UPPER_TRAILING_CORNER = "UPPER_TRAILING_CORNER";

    String VERTICAL_SCROLLBAR_POLICY = "VERTICAL_SCROLLBAR_POLICY";

    String HORIZONTAL_SCROLLBAR_POLICY = "HORIZONTAL_SCROLLBAR_POLICY";

    /** The vertical bar appears only when the content does not fit. */
    int VERTICAL_SCROLLBAR_AS_NEEDED = 20;

    int VERTICAL_SCROLLBAR_NEVER = 21;

    int VERTICAL_SCROLLBAR_ALWAYS = 22;

    /** The horizontal bar appears only when the content does not fit. */
    int HORIZONTAL_SCROLLBAR_AS_NEEDED = 30;

    int HORIZONTAL_SCROLLBAR_NEVER = 31;

    int HORIZONTAL_SCROLLBAR_ALWAYS = 32;
}
