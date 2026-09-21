package javax.swing.text;

/**
 * An {@link Error} of a broken invariant: the document would be left in an impossible state.
 *
 * <p>It is an error and not an exception on purpose. It is not thrown because of what the user
 * did --that is {@link BadLocationException}-- but because of what the program did: changing
 * attributes without the write lock, or releasing a read lock that was never taken. None of that
 * can be handled; it is fixed in the code.
 *
 * <p>It is not public: the JDK does not expose it either, and whoever sees it will see it as an
 * {@code Error}.
 */
class StateInvariantError extends Error {

    public StateInvariantError(String s) {
        super(s);
    }
}
