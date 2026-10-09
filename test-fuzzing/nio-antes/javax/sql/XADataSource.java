package javax.sql;

/** KajiLibrary's javax.sql.XADataSource -- where the {@link XAConnection}s come from. */
public interface XADataSource extends CommonDataSource {

    XAConnection getXAConnection() throws java.sql.SQLException;

    XAConnection getXAConnection(String user, String password) throws java.sql.SQLException;

    default XAConnectionBuilder createXAConnectionBuilder() throws java.sql.SQLException {
        throw new UnsupportedOperationException("createXAConnectionBuilder not implemented");
    }
}
