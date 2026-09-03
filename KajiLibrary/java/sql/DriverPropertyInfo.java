package java.sql;

/**
 * KajiLibrary's java.sql.DriverPropertyInfo -- una propiedad que un driver acepta.
 *
 * <p>Es lo que permite escribir una herramienta de conexion **generica**: se le pregunta al driver
 * que propiedades quiere, se arma el formulario con eso, y se le devuelven llenas. Sin esto habria
 * que conocer cada driver.
 *
 * <p>Campos publicos y no accesores. Es de 1997 y es una estructura de datos; cambiarlo ahora
 * romperia a todos sus usuarios sin arreglarle nada a nadie.
 */
public class DriverPropertyInfo {

    /** El nombre de la propiedad. */
    public String name;

    /** Para que sirve; puede ser `null`. */
    public String description;

    /** Si hay que darla si o si para conectar. */
    public boolean required;

    /** El valor actual, o el que el driver sugiere. */
    public String value;

    /** Los valores admitidos, si son un conjunto cerrado; `null` si es libre. */
    public String[] choices;

    public DriverPropertyInfo(String name, String value) {
        this.name = name;
        this.value = value;
        this.description = null;
        this.required = false;
        this.choices = null;
    }
}
