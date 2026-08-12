package java.time.temporal;

import java.time.DateTimeException;

// KajiLibrary's java.time.temporal.UnsupportedTemporalTypeException — thrown when a TemporalField or
// TemporalUnit is not supported by a temporal. A KajiLibrary subset: the cause-carrying constructor
// is omitted (the exception chain has no (String, Throwable)).
public class UnsupportedTemporalTypeException extends DateTimeException {

    public UnsupportedTemporalTypeException(String message) {
        super(message);
    }
}
