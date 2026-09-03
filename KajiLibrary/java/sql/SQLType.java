package java.sql;

/**
 * KajiLibrary's java.sql.SQLType -- un tipo de dato SQL, con nombre.
 *
 * <p>Es una interfaz y no un enum a proposito: los tipos estandar son un enum (`JDBCType`), pero un
 * proveedor puede tener los suyos, y sin esta interfaz no habria como nombrarlos en una firma.
 */
public interface SQLType {

    /** El nombre del tipo, tal como el proveedor lo llama. */
    String getName();

    /** El proveedor que lo define; para los estandar, `"java.sql"`. */
    String getVendor();

    /** El numero con el que ese proveedor lo identifica. */
    Integer getVendorTypeNumber();
}
