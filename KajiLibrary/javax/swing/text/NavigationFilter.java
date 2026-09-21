package javax.swing.text;

/**
 * A filter that sees each movement of the cursor before it happens.
 *
 * <p>It is {@link DocumentFilter}'s sibling for navigation: it serves so that the cursor cannot
 * enter a stretch --a template with fixed parts-- or so that it jumps by words. This class's
 * version lets everything through.
 *
 * <p>As over there, the filter does not move the cursor by calling the cursor: it calls the
 * {@link FilterBypass}, which skips the filter and does not enter it again.
 */
public class NavigationFilter {

    public NavigationFilter() {
    }

    /** Moving the cursor undoing the selection. */
    public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
        fb.setDot(dot, bias);
    }

    /** Moving the cursor extending the selection. */
    public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
        fb.moveDot(dot, bias);
    }

    /**
     * Where the cursor goes from that position in that direction.
     *
     * <p>It is passed to the component's look and feel, which is the one that knows the view tree;
     * a filter that wants to jump another way redefines it.
     */
    public int getNextVisualPositionFrom(JTextComponent text, int pos, Position.Bias bias,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        return text.getUI().getNextVisualPositionFrom(text, pos, bias, direction, biasRet);
    }

    /** The bypass for moving the cursor without going through the filter again. */
    public abstract static class FilterBypass {

        protected FilterBypass() {
        }

        public abstract Caret getCaret();

        public abstract void setDot(int dot, Position.Bias bias);

        public abstract void moveDot(int dot, Position.Bias bias);
    }
}
