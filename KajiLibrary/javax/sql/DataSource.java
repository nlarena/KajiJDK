package javax.sql;

// KajiLibrary's javax.sql.DataSource (finding #267).
//
// It exists because the API needs the TYPE: `jakarta.persistence.spi.PersistenceUnitInfo`
// declares `getJtaDataSource()` and `getNonJtaDataSource()` returning one, and without the
// interface the file does not compile. That is the honest scope of this interface today.
//
// It is EMPTY on purpose, and that is the interesting part. In the JDK a DataSource is a factory
// for `java.sql.Connection`:
//
//     Connection getConnection() throws SQLException;
//     Connection getConnection(String username, String password) throws SQLException;
//
// and it extends `CommonDataSource` (login timeout, log writer) and `Wrapper` (unwrap). None of
// those types exist here -- there is no java.sql at all -- so every method would have to name a
// type that is not there. Declaring them against invented stand-ins would be worse than not
// declaring them: the signature is the contract, and a `getConnection()` that returned some other
// Connection is not the method the caller compiled against.
//
// So this is a marker: enough to name the type in a signature, and nothing that pretends to work.
// The day java.sql exists, the methods come with it. A missing member is a legal subset; a member
// that lies is not.
public interface DataSource {
}
