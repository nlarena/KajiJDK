package java.sql;

/**
 * KajiLibrary's java.sql.DriverPropertyInfo -- a property a driver accepts.
 *
 * <p>It is what allows writing a **generic** connection tool: the driver is asked which properties
 * it wants, the form is built from that, and they are handed back filled in. Without it each driver
 * would have to be known.
 *
 * <p>Public fields and not accessors. It is from 1997 and it is a data structure; changing it now
 * would break all its users without fixing anything for anyone.
 */
public class DriverPropertyInfo {

    /** The name of the property. */
    public String name;

    /** What it is for; may be `null`. */
    public String description;

    /** Whether it must be given to connect. */
    public boolean required;

    /** The current value, or the one the driver suggests. */
    public String value;

    /** The allowed values, if they are a closed set; `null` if free. */
    public String[] choices;

    public DriverPropertyInfo(String name, String value) {
        this.name = name;
        this.value = value;
        this.description = null;
        this.required = false;
        this.choices = null;
    }
}
