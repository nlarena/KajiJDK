package java.sql;

/**
 * KajiLibrary's java.sql.DriverAction -- lo que un driver quiere hacer al ser dado de baja.
 *
 * <p>Existe para que la limpieza no sea publica: si el driver expusiera un metodo para esto,
 * cualquiera podria llamarlo. Registrandolo con {@link DriverManager#registerDriver(Driver,
 * DriverAction)} solo el gestor tiene la referencia, y solo la usa cuando corresponde.
 */
public interface DriverAction {

    /** Lo llama {@link DriverManager} al dar de baja al driver. */
    void deregister();
}
