package javax.management.openmbean;

import javax.management.JMException;

/**
 * A piece of data that does not meet the open type it claims to have.
 *
 * <p>It is <b>checked</b>, and it is the only one of the four in this package that is. The
 * difference is not one of style: this one is thrown by a constructor given data that comes from
 * outside --from a connection, from a configuration file-- and that may not have been possible to
 * validate beforehand. The other three are programming errors over data one already holds, and that
 * is why they are unchecked.
 */
public class OpenDataException extends JMException {

    private static final long serialVersionUID = 8346311255433349870L;

    /** Without a message. */
    public OpenDataException() {
        super();
    }

    /** With that message. */
    public OpenDataException(String msg) {
        super(msg);
    }
}
