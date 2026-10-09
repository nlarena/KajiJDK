package jakarta.persistence;

// Thrown when a query times out and only the statement is rolled back (the transaction
// is not marked for rollback). Carries the offending Query when the provider supplies it.
public class QueryTimeoutException extends PersistenceException {
    private final Query query;
    public QueryTimeoutException() { super(); this.query = null; }
    public QueryTimeoutException(String message) { super(message); this.query = null; }
    public QueryTimeoutException(String message, Throwable cause) { super(message, cause); this.query = null; }
    public QueryTimeoutException(Throwable cause) { super(cause); this.query = null; }
    public QueryTimeoutException(Query query) { this.query = query; }
    public QueryTimeoutException(String message, Throwable cause, Query query) { super(message, cause); this.query = query; }
    public Query getQuery() { return query; }
}
