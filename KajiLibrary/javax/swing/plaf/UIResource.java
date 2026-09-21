package javax.swing.plaf;

/**
 * Marks "this was set by the look and feel, not by the user".
 *
 * <p>It is the answer to a question every look and feel has to ask itself when installing
 * itself: if a button already has a border, did the programmer choose it or did the previous look
 * and feel leave it? A value that implements this interface belongs to the look and feel and can
 * be overwritten; one that does not belongs to the user and is respected. It has no methods
 * because it does nothing: it is a label on the type, not on the value.
 *
 * <p>The classes {@code ColorUIResource}, {@code FontUIResource}, {@code InsetsUIResource} and
 * {@code BorderUIResource} are exactly that: the usual value, with the label put on.
 */
public interface UIResource {
}
