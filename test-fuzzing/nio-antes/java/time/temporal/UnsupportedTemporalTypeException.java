package java.time.temporal;

import java.time.DateTimeException;

// KajiLibrary's java.time.temporal.UnsupportedTemporalTypeException — thrown when a TemporalField or
// TemporalUnit is not supported by a temporal.
public class UnsupportedTemporalTypeException extends DateTimeException {

    public UnsupportedTemporalTypeException(String message) {
        super(message);
    }

    public UnsupportedTemporalTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
