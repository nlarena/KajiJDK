package javax.swing.text;

/**
 * A filter that sees each edit before it happens and decides what to do with it.
 *
 * <p>It is how a field that only accepts numbers is made, or one that turns everything into
 * upper case, or one of limited length: the filter receives what is to be inserted and writes
 * something else, or writes nothing. This class's version lets everything through, so that a
 * subclass redefines only what interests it.
 *
 * <p>The filter does <strong>not</strong> write by calling the document: it calls the
 * {@link FilterBypass} it is passed. If it called the document, the write would go through the
 * filter again and would never end.
 */
public class DocumentFilter {

    public DocumentFilter() {
    }

    /** It lets the removal happen; a subclass may not call the bypass and so veto the removal. */
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        fb.remove(offset, length);
    }

    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        fb.insertString(offset, string, attr);
    }

    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        fb.replace(offset, length, text, attrs);
    }

    /**
     * The bypass for writing without going through the filter again; see the class note.
     *
     * <p>The document implements it, not the filter: only the document can skip its own filter
     * without breaking anything.
     */
    public abstract static class FilterBypass {

        protected FilterBypass() {
        }

        public abstract Document getDocument();

        public abstract void remove(int offset, int length) throws BadLocationException;

        public abstract void insertString(int offset, String string, AttributeSet attr)
                throws BadLocationException;

        public abstract void replace(int offset, int length, String string, AttributeSet attrs)
                throws BadLocationException;
    }
}
