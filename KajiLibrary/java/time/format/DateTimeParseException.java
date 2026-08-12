package java.time.format;

import java.time.DateTimeException;

// KajiLibrary's java.time.format.DateTimeParseException — thrown when text cannot be parsed into a
// date-time. Carries the text being parsed and the index at which the error occurred. A KajiLibrary
// subset: the cause-carrying constructor is omitted (the exception chain has no (String, Throwable)).
public class DateTimeParseException extends DateTimeException {

    private final String parsedString;
    private final int errorIndex;

    public DateTimeParseException(String message, CharSequence parsedData, int errorIndex) {
        super(message);
        this.parsedString = parsedData.toString();
        this.errorIndex = errorIndex;
    }

    public String getParsedString() {
        return this.parsedString;
    }

    public int getErrorIndex() {
        return this.errorIndex;
    }
}
