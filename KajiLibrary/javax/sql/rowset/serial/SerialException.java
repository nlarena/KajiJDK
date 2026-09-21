package javax.sql.rowset.serial;

import java.sql.SQLException;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialException -- failure to copy an SQL datum into
 * memory.
 *
 * <p>All the classes of this package throw it, and it means one of two things: the datum that was
 * to be copied could not be read from the server, or a slice out of range of a copy was asked for.
 *
 * <p>It inherits from {@link SQLException} because whoever uses these classes is already catching
 * database errors; a separate hierarchy would force a second {@code catch} that would do the same.
 */
public class SerialException extends SQLException {

    private static final long serialVersionUID = -489794565168592690L;

    /** Without detail. */
    public SerialException() {
        super();
    }

    /** With a message that says what happened. */
    public SerialException(String msg) {
        super(msg);
    }
}
