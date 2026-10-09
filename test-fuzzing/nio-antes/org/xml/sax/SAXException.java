package org.xml.sax;

// KajiLibrary's org.xml.sax.SAXException -- the checked exception every SAX callback is allowed
// to throw. It predates the chained exceptions of JDK 1.4, so it grew its own "wrapped exception"
// slot; today that slot *is* the cause of Throwable, and the two constructors that take an
// Exception simply pass it to super. getException() reads the cause back, narrowed to Exception
// (a cause that is an Error or a bare Throwable reads as null, which is what the JDK does).
//
// The rule worth reading twice is getMessage(): when this exception has no message of its own but
// does have a cause, it answers with the message *of the cause* instead of null. So
//
//     new SAXException(new java.io.IOException("disk on fire")).getMessage()
//
// gives "disk on fire", not null. Only with both at null does null come out, and a message of its
// own always beats that of the cause.
//
// What is left out: the serialisation hooks (writeObject/readObject and the
// serialPersistentFields array that keeps the historical field name "exception" on the wire).
// They are private, so no contract depends on them. The note gave as the other reason that they
// would need java.io.ObjectStreamField plus ObjectOutputStream.PutField /
// ObjectInputStream.GetField, "which this library does not have"; the three are in the library
// now, so that reason no longer holds and the hooks could be written. serialVersionUID is left at
// the JDK's value so that a stream written elsewhere still names the same class.
public class SAXException extends Exception {

    static final long serialVersionUID = 583241635256073760L;

    public SAXException() {
        super();
    }

    public SAXException(String message) {
        super(message);
    }

    public SAXException(Exception e) {
        super(e);
    }

    public SAXException(String message, Exception e) {
        super(message, e);
    }

    // The rule of delegation described above. Note that it reads super.getMessage(), not
    // getMessage(): the second would call itself.
    public String getMessage() {
        String message = super.getMessage();
        Throwable cause = super.getCause();

        if (message == null && cause != null) {
            return cause.getMessage();
        } else {
            return message;
        }
    }

    // The wrapped exception, that is the cause when it happens to be an Exception.
    public Exception getException() {
        return getExceptionInternal();
    }

    // Declared explicitly (instead of inherited) because the JDK declares it here as well: the
    // contract lists getCause() as a member of this class.
    public Throwable getCause() {
        return super.getCause();
    }

    public String toString() {
        Throwable exception = super.getCause();
        if (exception != null) {
            return super.toString() + "\n" + exception.toString();
        } else {
            return super.toString();
        }
    }

    private Exception getExceptionInternal() {
        Throwable cause = super.getCause();
        if (cause instanceof Exception) {
            return (Exception) cause;
        } else {
            return null;
        }
    }
}
