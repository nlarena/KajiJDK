package java.security;

// An entry of the key store could not be recovered: typically, the password is wrong.
//
// Two constructors and not four: it carries no chained cause because the real cause —"the password
// was not the right one"— is just what it is better not to expose. A stack trace that told "badly
// set key" apart from "corrupt entry" would tell an attacker when they got half the problem right.
public class UnrecoverableEntryException extends GeneralSecurityException {

    public UnrecoverableEntryException() {
        super();
    }

    public UnrecoverableEntryException(String message) {
        super(message);
    }
}
