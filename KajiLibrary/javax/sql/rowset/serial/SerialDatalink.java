package javax.sql.rowset.serial;

import java.io.Serializable;
import java.net.URL;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialDatalink -- a copy of a DATALINK value.
 *
 * <p>An SQL DATALINK is a column that keeps a URL to a resource <b>outside</b> the database. This
 * class copies that URL so that the value survives the connection.
 *
 * <p>What it copies is the <b>URL, not the resource</b>. It is the only thing that can be done
 * --the resource can be a file of gigabytes on another machine-- and it has a consequence worth
 * keeping in mind: a {@code SerialDatalink} serialized and read elsewhere points to the same place,
 * which from there may not exist.
 */
public class SerialDatalink implements Serializable, Cloneable {

    private static final long serialVersionUID = 2826907821828733626L;

    /** The copied URL. */
    private final URL url;

    /**
     * @param url the URL of the resource
     * @throws SerialException if it is null
     */
    public SerialDatalink(URL url) throws SerialException {
        if (url == null) {
            throw new SerialException("Cannot serialize empty URL instance");
        }
        this.url = url;
    }

    /** The URL. */
    public URL getDatalink() throws SerialException {
        return this.url;
    }

    /** Equal if they point to the same URL. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialDatalink)) {
            return false;
        }
        return this.url.equals(((SerialDatalink) obj).url);
    }

    /** Consistent with {@link #equals}. */
    public int hashCode() {
        return 31 + this.url.hashCode();
    }

    /** A copy; the URL is immutable and is shared. */
    public Object clone() {
        try {
            return new SerialDatalink(this.url);
        } catch (SerialException e) {
            return null;
        }
    }
}
