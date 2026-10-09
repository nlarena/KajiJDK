package javax.sql;

/**
 * KajiLibrary's javax.sql.StatementEventListener -- a **statement** pool listens to it.
 *
 * <p>The same idea as {@link ConnectionEventListener} one level down: preparing a statement is
 * expensive, so a pool keeps them too, and needs to find out when the application closed its own.
 */
public interface StatementEventListener extends java.util.EventListener {

    /** The application closed the statement: it can be reused. */
    void statementClosed(StatementEvent event);

    /** The statement stopped being valid: it has to be discarded. */
    void statementErrorOccurred(StatementEvent event);
}
